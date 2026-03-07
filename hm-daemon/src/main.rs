mod handlers;
mod models;
mod scheduler;
mod state;
mod executor;
mod telemetry;
mod auth;

use crate::state::{init_db_pool, AppState};
use axum::middleware;
use axum::routing::post;
use axum::{
	routing::{delete, get},
	Router,
};
use rand::Rng;
use std::sync::{Arc, Mutex};

#[tokio::main]
async fn main() {
	let db_pool = init_db_pool();

	tokio::spawn(scheduler::run_scheduler(db_pool.clone()));

	let otp: String = rand::thread_rng()
		.gen_range(0u32..=999999)
		.to_string()
		.chars()
		.collect::<String>();
	let otp = format!("{:0>6}", otp);
	println!("Pairing code: {}", otp);

	let app_state = AppState {
		pool: db_pool,
		pairing_otp: Arc::new(Mutex::new(Some(otp))),
	};

	let protected = Router::new()
		.route(
			"/commands",
			get(handlers::list_commands).post(handlers::create_command),
		)
		.route("/commands/{id}", delete(handlers::delete_command))
		.route("/commands/{id}/run", post(handlers::run_command))
		.route("/commands/{id}/stream", get(handlers::stream_command))
		.route("/jobs", get(handlers::list_jobs).post(handlers::create_job))
		.route("/jobs/{id}", delete(handlers::delete_job))
		.route("/servers", get(handlers::list_servers).post(handlers::create_server))
		.route("/servers/{id}", delete(handlers::delete_server))
		.route("/servers/{id}/status", get(handlers::get_server_status))
		.layer(middleware::from_fn_with_state(
			app_state.clone(),
			auth::auth_middleware,
		));

	let app = Router::new()
		.route("/status", get(|| async { "Daemon Operational" }))
		.route("/pair", post(handlers::pair))
		.merge(protected)
		.with_state(app_state);

	let listener = tokio::net::TcpListener::bind("0.0.0.0:3000").await.unwrap();
	println!(
		"Helmsman Daemon listening on: {}",
		listener.local_addr().unwrap()
	);

	axum::serve(listener, app).await.unwrap();
}
