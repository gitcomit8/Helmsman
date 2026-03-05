use axum::{routing::get, Router};

#[tokio::main]
async fn main() {
    // Init foundational router for monitoring data
    let app = Router::new().route("/status", get(|| async { "Daemon Operational" }));

    // Bind to all nics on port 3000
    let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await.unwrap();
    println!("Server is listening on: {}", listener.local_addr().unwrap());

    axum::serve(listener, app).await.unwrap();
}
