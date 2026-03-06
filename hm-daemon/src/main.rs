mod models;
mod state;

use crate::state::new_shared_state;
use axum::{routing::get, Router};

#[tokio::main]
async fn main() {
    let app_state = new_shared_state();

    let app = Router::new()
        .route("/status", get(|| async { "Daemon Operational" }))
        .with_state(app_state);

    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await.unwrap();
    println!("Server is listening on: {}", listener.local_addr().unwrap());

    axum::serve(listener, app).await.unwrap();
}
