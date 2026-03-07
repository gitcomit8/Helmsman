use crate::executor::execute_command;
use crate::models::CommandSpec;
use crate::state::DbPool;
use chrono::{DateTime, Utc};
use cron::Schedule;
use rusqlite::params;
use std::str::FromStr;
use tokio::time::{sleep, Duration};

pub async fn run_scheduler(pool: DbPool) {
    loop {
        sleep(Duration::from_secs(30)).await;
        dispatch_due_jobs(&pool).await;
    }
}

async fn dispatch_due_jobs(pool: &DbPool) {
    let now = Utc::now();

    let due_jobs: Vec<(String, String, String, String)> = {
        let conn = match pool.get() {
            Ok(c) => c,
            Err(e) => {
                eprintln!("Scheduler pool error: {}", e);
                return;
            }
        };

        let mut stmt = match conn.prepare(
            "SELECT j.id, j.schedule, j.command_id, c.command
             FROM jobs j
             JOIN commands c ON j.command_id = c.id
             WHERE j.next_run <= ?1",
        ) {
            Ok(s) => s,
            Err(e) => {
                eprintln!("Scheduler query prepare error: {}", e);
                return;
            }
        };

        let now_str = now.to_rfc3339();
        let rows = stmt.query_map(params![now_str], |row| {
            Ok((
                row.get::<_, String>(0)?,
                row.get::<_, String>(1)?,
                row.get::<_, String>(2)?,
                row.get::<_, String>(3)?,
            ))
        });

        match rows {
            Ok(iter) => iter.filter_map(|r| r.ok()).collect(),
            Err(e) => {
                eprintln!("Scheduler row error: {}", e);
                return;
            }
        }
    };

    for (job_id, schedule_str, command_id, cmd_json) in due_jobs {
        let next_run = Schedule::from_str(&schedule_str)
            .ok()
            .and_then(|s| s.upcoming(Utc).next());

        if let Some(next) = next_run {
            update_job_times(pool, &job_id, now, next);
        }

        let command: Vec<String> = serde_json::from_str(&cmd_json).unwrap_or_default();
        let spec = CommandSpec {
            id: command_id,
            name: String::new(),
            command,
            working_dir: None,
            timeout_seconds: None,
        };

        tokio::spawn(async move {
            match execute_command(&spec).await {
                Ok(result) => eprintln!(
                    "Scheduler: job {} exited with {:?}",
                    job_id, result.exit_code
                ),
                Err(e) => eprintln!("Scheduler: job {} failed: {}", job_id, e),
            }
        });
    }
}

fn update_job_times(pool: &DbPool, job_id: &str, last_run: DateTime<Utc>, next_run: DateTime<Utc>) {
    let conn = match pool.get() {
        Ok(c) => c,
        Err(e) => {
            eprintln!("Scheduler update pool error: {}", e);
            return;
        }
    };

    if let Err(e) = conn.execute(
        "UPDATE jobs SET last_run = ?1, next_run = ?2 WHERE id = ?3",
        params![last_run.to_rfc3339(), next_run.to_rfc3339(), job_id],
    ) {
        eprintln!("Scheduler update error: {}", e);
    }
}
