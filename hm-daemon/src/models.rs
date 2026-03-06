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