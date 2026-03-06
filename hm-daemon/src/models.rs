use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct CommandSpec {
    pub id: String,
    pub name: String,
    pub command: Vec<String>,
    pub working_dir: Option<String>,
    pub timeout_seconds: Option<i64>,
}
