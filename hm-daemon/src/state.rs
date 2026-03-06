use crate::models::CommandSpec;
use std::collections::HashMap;
use std::sync::{Arc, RwLock};

// Type alias for thread-safe shared state
pub type SharedState = Arc<RwLock<HashMap<String, CommandSpec>>>;

// Factory fn to init the registry
pub fn new_shared_state() -> SharedState {
    Arc::new(RwLock::new(HashMap::new()))
}
