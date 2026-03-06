use r2d2::Pool;
use r2d2_sqlite::SqliteConnectionManager;

pub type DbPool = Pool<SqliteConnectionManager>;

pub fn init_db_pool() -> DbPool {
    let manager = SqliteConnectionManager::file("helmsman.db");
    let pool = Pool::new(manager).expect("Failed to initialize SQLite connection pool");
    let conn = pool
        .get()
        .expect("Failed to get SQLite connection from pool");
    conn.execute(
        "CREATE TABLE IF NOT EXISTS commands(
		id TEXT PRIMARY KEY,
		name TEXT NOT NULL,
		working_dir TEXT,
		timeout_seconds INTEGER)",
        (),
    )
    .expect("Failed to execute schema DDL");
    pool
}
