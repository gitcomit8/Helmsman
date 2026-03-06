use r2d2::Pool;
use r2d2_sqlite::SqliteConnectionManager;

pub type DbPool = Pool<SqliteConnectionManager>;

pub fn init_db_pool() -> DbPool {
	let manager = SqliteConnectionManager::file("helmsman.db");
	let pool = Pool::new(manager).expect("Failed to initialize SQLite connection pool");

	let conn = pool.get().expect("Failed to acquire connection from pool");

	conn.execute("PRAGMA foreign_keys = ON;", ()).unwrap();

	conn.execute(
		"CREATE TABLE IF NOT EXISTS commands (
			id TEXT PRIMARY KEY,
			name TEXT NOT NULL,
			command TEXT NOT NULL,
			working_directory TEXT,
			timeout_seconds INTEGER
		)",
		(),
	).expect("Failed to execute commands schema DDL");

	conn.execute(
		"CREATE TABLE IF NOT EXISTS jobs (
			id TEXT PRIMARY KEY,
			name TEXT NOT NULL,
			schedule TEXT NOT NULL,
			command_id TEXT NOT NULL,
			last_run TEXT,
			next_run TEXT NOT NULL,
			FOREIGN KEY(command_id) REFERENCES commands(id) ON DELETE CASCADE
		)",
		(),
	).expect("Failed to execute jobs schema DDL");

	pool
}