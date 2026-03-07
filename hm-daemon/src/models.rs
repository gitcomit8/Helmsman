use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct CommandSpec {
	pub id: String,
	pub name: String,
	pub command: Vec<String>,
	pub working_dir: Option<String>,
	pub timeout_seconds: Option<u64>,
}

#[derive(Debug, Serialize, Clone)]
pub struct JobResult {
	pub exit_code: Option<i32>,
	pub stdout: String,
	pub stderr: String,
	pub duration_ms: u128,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Job {
	pub id: String,
	pub name: String,
	pub schedule: String,
	pub command_id: String,
	pub last_run: Option<DateTime<Utc>>,
	pub next_run: DateTime<Utc>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub struct Server {
	pub id: String,
	pub name: String,
	pub host: String,
}

#[derive(Debug, Deserialize)]
pub struct ServerCreateRequest {
	pub id: String,
	pub name: String,
	pub host: String,
}

#[derive(Debug, Serialize)]
pub struct ServerStatus {
	pub host: String,
	pub ping_ms: Option<f64>,
	pub ping_error: Option<String>,
	pub uname: String,
}

#[derive(Debug, Deserialize)]
pub struct JobCreateRequest {
	pub id: String,
	pub name: String,
	pub schedule: String,
	pub command_id: String,
}