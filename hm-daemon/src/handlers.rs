use crate::{models::CommandSpec, state::SharedState};
use axum::{
    extract::{Path, State},
    http::StatusCode,
    Json,
};

pub async fn list_commands(State(state): State<SharedState>) -> Json<Vec<CommandSpec>> {
    let db = state.read().unwrap();
    let commands: Vec<CommandSpec> = db.values().cloned().collect();
    Json(commands)
}

pub async fn create_command(
    State(state): State<SharedState>,
    Json(payload): Json<CommandSpec>,
) -> StatusCode {
    let mut db = state.write().unwrap();
    db.insert(payload.id.clone(), payload);
    StatusCode::CREATED
}

pub async fn delete_command(
    State(state): State<SharedState>,
    Path(id): Path<String>,
) -> StatusCode {
    let mut db = state.write().unwrap();
    if db.remove(&id).is_some() {
        StatusCode::NO_CONTENT
    } else {
        StatusCode::NOT_FOUND
    }
}
