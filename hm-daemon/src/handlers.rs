use crate::{models::CommandSpec, state::DbPool};
use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};
use rusqlite::params;

pub async fn list_commands(State(pool): State<DbPool>) -> Json<Vec<CommandSpec>> {
    // Dispatch blocking I/O to a dedicated thread
    let commands = tokio::task::spawn_blocking(move || {
        let conn = pool.get().unwrap();
        let mut stmt = conn
            .prepare("SELECT id, name, command, working_dir, timeout_seconds FROM commands")
            .unwrap();

        let command_iter = stmt
            .query_map([], |row| {
                let cmd_str: String = row.get(2)?;
                Ok(CommandSpec {
                    id: row.get(0)?,
                    name: row.get(1)?,
                    command: serde_json::from_str(&cmd_str).unwrap(),
                    working_dir: row.get(3)?,
                    timeout_seconds: row.get(4)?,
                })
            })
            .unwrap();

        command_iter
            .filter_map(Result::ok)
            .collect::<Vec<CommandSpec>>()
    })
    .await
    .unwrap();

    Json(commands)
}

pub async fn create_command(
    State(pool): State<DbPool>,
    Json(payload): Json<CommandSpec>,
) -> StatusCode {
    tokio::task::spawn_blocking(move || {
        let conn = pool.get().unwrap();
        let cmd_json = serde_json::to_string(&payload.command).unwrap();

        let result = conn.execute(
            "INSERT OR REPLACE INTO commands (id, name, command, working_dir, timeout_seconds) VALUES (?1, ?2, ?3, ?4, ?5)",
            params![payload.id, payload.name, cmd_json, payload.working_dir, payload.timeout_seconds],
        );

        match result {
            Ok(_) => StatusCode::CREATED,
            Err(_) => StatusCode::INTERNAL_SERVER_ERROR,
        }
    }).await.unwrap()
}

pub async fn delete_command(State(pool): State<DbPool>, Path(id): Path<String>) -> StatusCode {
    tokio::task::spawn_blocking(move || {
        let conn = pool.get().unwrap();
        let rows_affected = conn
            .execute("DELETE FROM commands WHERE id = ?1", params![id])
            .unwrap();

        if rows_affected > 0 {
            StatusCode::NO_CONTENT
        } else {
            StatusCode::NOT_FOUND
        }
    })
    .await
    .unwrap()
}
