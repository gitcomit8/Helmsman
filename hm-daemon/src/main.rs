mod handlers;
mod models;
mod state;

use crate::state::init_db_pool;
use axum::{
    routing::{delete, get},
    Router,
};

#[tokio::main]
async fn main() {
    let db_pool = init_db_pool();
    let app = Router::new()
        .route("/status", get(|| async { "Daemon Operational" }))
        .route(
            "/commands",
            get(handlers::list_commands).post(handlers::create_command),
        )
        .route("/commands/{id}", delete(handlers::delete_command))
        .with_state(db_pool);

    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await.unwrap();
    println!(
        "Helmsman Daemon listening on: {}",
        listener.local_addr().unwrap()
    );

    axum::serve(listener, app).await.unwrap();
}
