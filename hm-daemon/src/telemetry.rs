use std::net::IpAddr;
use std::str::FromStr;
use std::time::Duration;

pub async fn ping_host(host: &str) -> Result<Duration, String> {
	let addr = resolve_host(host)?;
	let payload = [0u8; 8];
	surge_ping::ping(addr, &payload)
		.await
		.map(|(_, dur)| dur)
		.map_err(|e| e.to_string())
}

pub fn get_uname() -> String {
	std::process::Command::new("uname")
		.arg("-a")
		.output()
		.map(|o| String::from_utf8_lossy(&o.stdout).trim().to_string())
		.unwrap_or_else(|e| e.to_string())
}

fn resolve_host(host: &str) -> Result<IpAddr, String> {
	if let Ok(addr) = IpAddr::from_str(host) {
		return Ok(addr);
	}

	use std::net::ToSocketAddrs;
	let target = format!("{}:0", host);
	target
		.to_socket_addrs()
		.map_err(|e| e.to_string())?
		.find(|a| a.is_ipv4())
		.map(|a| a.ip())
		.ok_or_else(|| format!("Could not resolve host: {}", host))
}
