use crate::state::DbPool;
use axum::{
	body::Body,
	extract::State,
	http::{Request, StatusCode},
	middleware::Next,
	response::Response,
};
use rusqlite::params;

pub async fn auth_middleware(
	State(pool): State<DbPool>,
	req: Request<Body>,
	next: Next,
) -> Result<Response, StatusCode> {
	let token = req
		.headers()
		.get("Authorization")
		.and_then(|v| v.to_str().ok())
		.and_then(|v| v.strip_prefix("Bearer "))
		.map(|s| s.to_string())
		.ok_or(StatusCode::UNAUTHORIZED)?;

	let valid = tokio::task::spawn_blocking(move || -> Result<bool, ()> {
		let conn = pool.get().map_err(|_| ())?;
		let count: i64 = conn
			.query_row(
				"SELECT COUNT(*) FROM tokens WHERE token = ?1",
				params![token],
				|row| row.get(0),
			)
			.map_err(|_| ())?;
		Ok(count > 0)
	})
	.await
	.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
	.map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

	if !valid {
		return Err(StatusCode::UNAUTHORIZED);
	}

	Ok(next.run(req).await)
}
