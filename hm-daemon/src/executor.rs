use crate::models::{CommandSpec, JobResult};
use std::process::Stdio;
use std::time::{Duration, Instant};
use tokio::process::Command;
use tokio::time::timeout;

pub async fn execute_command(spec: &CommandSpec) -> Result<JobResult, String> {
	let start = Instant::now();

	if spec.command.is_empty() {
		return Err("Command vector is empty".into());
	}

	let mut cmd = Command::new(&spec.command[0]);
	cmd.args(&spec.command[1..]);
	if let Some(ref dir) = spec.working_dir {
		cmd.current_dir(dir);
	}

	cmd.stdout(Stdio::piped());
	cmd.stderr(Stdio::piped());

	let exec = cmd.output();

	let output = if let Some(secs) = spec.timeout_seconds {
		match timeout(Duration::from_secs(secs as u64), exec).await {
			Ok(res) => res.map_err(|e| e.to_string())?,
			Err(_) => return Err("Process excution exceeded timeout threshold".into()),
		}
	} else {
		exec.await.map_err(|e| e.to_string())?
	};

	Ok(JobResult {
		exit_code: output.status.code(),
		stdout: String::from_utf8_lossy(&output.stdout).into_owned(),
		stderr: String::from_utf8_lossy(&output.stderr).into_owned(),
		duration_ms: start.elapsed().as_millis(),
	})
}