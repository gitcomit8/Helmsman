use crate::models::{Job, JobCreateRequest, Server, ServerCreateRequest, ServerStatus};
use crate::{models::{CommandSpec, JobResult}, state::DbPool};
use axum::{
	extract::{Path, State},
	http::StatusCode,
	Json,
};
use chrono::{DateTime, Utc};
use cron::Schedule;
use rusqlite::params;
use std::str::FromStr;

pub async fn list_commands(State(pool): State<DbPool>) -> Result<Json<Vec<CommandSpec>>, StatusCode> {
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


pub async fn run_command(
	State(pool): State<DbPool>,
	Path(id): Path<String>,
) -> Result<Json<JobResult>, StatusCode> {
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

pub async fn create_job(
	State(pool): State<DbPool>,
	Json(payload): Json<JobCreateRequest>,
) -> Result<StatusCode, StatusCode> {
	let schedule = Schedule::from_str(&payload.schedule).map_err(|e| {
		eprintln!("Cron Parse error: {}", e);
		StatusCode::BAD_REQUEST
	})?;

	let next_run = schedule.upcoming(Utc).next().ok_or(StatusCode::BAD_REQUEST)?;

	tokio::task::spawn_blocking(move || -> Result<StatusCode, StatusCode> {
		let conn = pool.get().map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
		let next_run_str = next_run.to_rfc3339();

		if let Err(e) = conn.execute(
			"INSERT OR REPLACE INTO JOBS (id, name,schedule, command_id, next_run) VALUES (?1, ?2, ?3, ?4, ?5)",
			params![payload.id, payload.name, payload.schedule, payload.command_id, next_run_str],
		) {
			eprintln!("Job insertion fault: {}", e);
			return Err(StatusCode::INTERNAL_SERVER_ERROR);
		}

		Ok(StatusCode::CREATED)
	}).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
}

pub async fn list_jobs(State(pool): State<DbPool>) -> Result<Json<Vec<Job>>, StatusCode> {
	let jobs = tokio::task::spawn_blocking(move || -> Result<Vec<Job>, StatusCode> {
		let conn = pool.get().map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
		let mut stmt = conn.prepare("SELECT id, name, schedule, command_id, last_run, next_run FROM jobs").map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

		let job_iter = stmt.query_map([], |row| {
			let last_run_str: Option<String> = row.get(4)?;
			let next_run_str: String = row.get(5)?;

			let last_run = last_run_str.and_then(|s| DateTime::parse_from_rfc3339(&s).ok().map(|d| d.with_timezone(&Utc)));
			let next_run = DateTime::parse_from_rfc3339(&next_run_str).unwrap().with_timezone(&Utc);

			Ok(Job {
				id: row.get(0)?,
				name: row.get(1)?,
				schedule: row.get(2)?,
				command_id: row.get(3)?,
				last_run,
				next_run,
			})
		}).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

		let mut results = Vec::new();
		for j in job_iter {
			if let Ok(job) = j {
				results.push(job);
			}
		}
		Ok(results)
	}).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)??;

	Ok(Json(jobs))
}

pub async fn delete_job(
	State(pool): State<DbPool>,
	Path(id): Path<String>,
) -> Result<StatusCode, StatusCode> {
	tokio::task::spawn_blocking(move || -> Result<StatusCode, StatusCode> {
		let conn = pool.get().map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
		match conn.execute("DELETE FROM jobs WHERE id = ?1", params![id]) {
			Ok(rows) if rows > 0 => Ok(StatusCode::NO_CONTENT),
			Ok(_) => Ok(StatusCode::NOT_FOUND),
			Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
		}
	}).await.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
}

pub async fn list_servers(State(pool): State<DbPool>) -> Result<Json<Vec<Server>>, StatusCode> {
	let servers = tokio::task::spawn_blocking(move || -> Result<Vec<Server>, StatusCode> {
		let conn = pool.get().map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
		let mut stmt = conn
			.prepare("SELECT id, name, host FROM servers")
			.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

		let iter = stmt
			.query_map([], |row| {
				Ok(Server {
					id: row.get(0)?,
					name: row.get(1)?,
					host: row.get(2)?,
				})
			})
			.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

		let mut results = Vec::new();
		for s in iter {
			if let Ok(server) = s {
				results.push(server);
			}
		}
		Ok(results)
	})
	.await
	.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)??;

	Ok(Json(servers))
}

pub async fn create_server(
	State(pool): State<DbPool>,
	Json(payload): Json<ServerCreateRequest>,
) -> Result<StatusCode, StatusCode> {
	tokio::task::spawn_blocking(move || {
		let conn = pool.get().map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
		conn.execute(
			"INSERT OR REPLACE INTO servers (id, name, host) VALUES (?1, ?2, ?3)",
			params![payload.id, payload.name, payload.host],
		)
		.map_err(|e| {
			eprintln!("Server insert error: {}", e);
			StatusCode::INTERNAL_SERVER_ERROR
		})?;
		Ok(StatusCode::CREATED)
	})
	.await
	.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
}

pub async fn delete_server(
	State(pool): State<DbPool>,
	Path(id): Path<String>,
) -> Result<StatusCode, StatusCode> {
	tokio::task::spawn_blocking(move || {
		let conn = pool.get().map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
		match conn.execute("DELETE FROM servers WHERE id = ?1", params![id]) {
			Ok(rows) if rows > 0 => Ok(StatusCode::NO_CONTENT),
			Ok(_) => Ok(StatusCode::NOT_FOUND),
			Err(_) => Err(StatusCode::INTERNAL_SERVER_ERROR),
		}
	})
	.await
	.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
}

pub async fn get_server_status(
	State(pool): State<DbPool>,
	Path(id): Path<String>,
) -> Result<Json<ServerStatus>, StatusCode> {
	let host = tokio::task::spawn_blocking(move || -> Result<String, StatusCode> {
		let conn = pool.get().map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
		let mut stmt = conn
			.prepare("SELECT host FROM servers WHERE id = ?1")
			.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
		stmt.query_row(params![id], |row| row.get::<_, String>(0))
			.map_err(|_| StatusCode::NOT_FOUND)
	})
	.await
	.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)??;

	let uname = crate::telemetry::get_uname();

	let (ping_ms, ping_error) = match crate::telemetry::ping_host(&host).await {
		Ok(dur) => (Some(dur.as_secs_f64() * 1000.0), None),
		Err(e) => (None, Some(e)),
	};

	Ok(Json(ServerStatus {
		host,
		ping_ms,
		ping_error,
		uname,
	}))
}