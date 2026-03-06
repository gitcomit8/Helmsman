use crate::{models::{CommandSpec, JobResult}, state::DbPool};
use axum::{
	extract::{Path, State},
	http::StatusCode,
	Json,
};
use rusqlite::params;

pub async fn list_commands(State(pool): State<DbPool>) -> Result<Json<Vec<CommandSpec>>, StatusCode> {
	// Explicitly annotate the closure's return type signature
	let commands = tokio::task::spawn_blocking(move || -> Result<Vec<CommandSpec>, StatusCode> {
		let conn = pool.get().map_err(|e| {
			eprintln!("Pool acquisition error: {}", e);
			StatusCode::INTERNAL_SERVER_ERROR
		})?;

		let mut stmt = conn.prepare("SELECT id, name, command, working_dir, timeout_seconds FROM commands").map_err(|e| {
			eprintln!("Statement prepare error: {}", e);
			StatusCode::INTERNAL_SERVER_ERROR
		})?;

		let command_iter = stmt.query_map([], |row| {
			let cmd_str: String = row.get(2)?;
			let timeout_i64: Option<i64> = row.get(4)?;
			Ok(CommandSpec {
				id: row.get(0)?,
				name: row.get(1)?,
				command: serde_json::from_str(&cmd_str).unwrap_or_default(),
				working_dir: row.get(3)?,
				timeout_seconds: timeout_i64.map(|t| t as u64),
			})
		}).map_err(|e| {
			eprintln!("Query map error: {}", e);
			StatusCode::INTERNAL_SERVER_ERROR
		})?;

		let mut results = Vec::new();
		for cmd in command_iter {
			match cmd {
				Ok(c) => results.push(c),
				Err(e) => eprintln!("Row parsing error: {}", e),
			}
		}
		Ok(results)
	}).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)??;

	Ok(Json(commands))
}

pub async fn create_command(
	State(pool): State<DbPool>,
	Json(payload): Json<CommandSpec>,
) -> Result<StatusCode, StatusCode> {
	tokio::task::spawn_blocking(move || {
		let conn = pool.get().map_err(|e| {
			eprintln!("Pool acquisition error: {}", e);
			StatusCode::INTERNAL_SERVER_ERROR
		})?;

		let cmd_json = serde_json::to_string(&payload.command).unwrap_or_default();
		// Coerce u64 down to i64 to satisfy SQLite INTEGER constraints
		let timeout = payload.timeout_seconds.map(|t| t as i64);

		if let Err(e) = conn.execute(
			"INSERT OR REPLACE INTO commands (id, name, command, working_dir, timeout_seconds) VALUES (?1, ?2, ?3, ?4, ?5)",
			params![payload.id, payload.name, cmd_json, payload.working_dir, timeout],
		) {
			eprintln!("Database execution error: {}", e);
			return Err(StatusCode::INTERNAL_SERVER_ERROR);
		}

		Ok(StatusCode::CREATED)
	}).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
}

pub async fn delete_command(
	State(pool): State<DbPool>,
	Path(id): Path<String>,
) -> Result<StatusCode, StatusCode> {
	tokio::task::spawn_blocking(move || {
		let conn = pool.get().map_err(|e| {
			eprintln!("Pool error: {}", e);
			StatusCode::INTERNAL_SERVER_ERROR
		})?;

		match conn.execute("DELETE FROM commands WHERE id = ?1", params![id]) {
			Ok(rows) if rows > 0 => Ok(StatusCode::NO_CONTENT),
			Ok(_) => Ok(StatusCode::NOT_FOUND),
			Err(e) => {
				eprintln!("Delete error: {}", e);
				Err(StatusCode::INTERNAL_SERVER_ERROR)
			}
		}
	}).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
}

// In src/handlers.rs

pub async fn run_command(
	State(pool): State<DbPool>,
	Path(id): Path<String>,
) -> Result<Json<JobResult>, StatusCode> {
	// Explicitly annotate the closure's return type signature
	let spec_opt = tokio::task::spawn_blocking(move || -> Result<Option<CommandSpec>, StatusCode> {
		let conn = pool.get().map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
		let mut stmt = conn.prepare("SELECT name, command, working_dir, timeout_seconds FROM commands WHERE id = ?1").map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

		let spec = stmt.query_row(params![id], |row| {
			let cmd_str: String = row.get(1)?;
			let timeout_i64: Option<i64> = row.get(3)?;
			Ok(CommandSpec {
				id: id.clone(),
				name: row.get(0)?,
				command: serde_json::from_str(&cmd_str).unwrap_or_default(),
				working_dir: row.get(2)?,
				timeout_seconds: timeout_i64.map(|t| t as u64),
			})
		}).ok();
		Ok(spec)
	}).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)??;

	let spec = spec_opt.ok_or(StatusCode::NOT_FOUND)?;

	match crate::executor::execute_command(&spec).await {
		Ok(result) => Ok(Json(result)),
		Err(e) => {
			eprintln!("Subprocess executor failed: {}", e);
			Err(StatusCode::INTERNAL_SERVER_ERROR)
		}
	}
}