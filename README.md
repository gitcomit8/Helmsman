# Helmsman

Helmsman is a lightweight, self-hosted remote command execution and job scheduling system designed for developers and infrastructure operators who want to control their servers from an Android device. It consists of two components: **hm-daemon**, a high-performance HTTP/WebSocket server written in Rust, and **hm-client**, a native Android application written in Kotlin. You deploy the daemon on any Linux server, pair your Android device using a one-time code, and you can then remotely execute shell commands (with real-time streaming output), schedule recurring tasks via cron expressions, and monitor server availability — all secured by bearer token authentication backed by SQLite persistence.

## Table of Contents

1. [Architecture](#architecture)
2. [Components](#components)
3. [Technology Stack](#technology-stack)
4. [Features](#features)
5. [Installation & Build](#installation--build)
6. [Configuration & Setup](#configuration--setup)
7. [Usage](#usage)
8. [API Reference](#api-reference)
9. [Security](#security)
10. [Project Structure](#project-structure)

---

## Architecture

Helmsman follows a **client-server architecture**:

```
┌──────────────────────┐
│   Android Client     │
│   (hm-client)        │
│                      │
│ - Command execution  │
│ - Job management     │
│ - Real-time updates  │
└──────┬───────────────┘
       │
       │  HTTP / WebSocket (REST API)
       │  Bearer Token Authentication
       │
┌──────▼──────────────────┐
│   Helmsman Daemon        │
│   (hm-daemon)            │
│                          │
│ - HTTP/WebSocket Server  │
│ - Command Executor       │
│ - Cron Scheduler         │
│ - Authentication Layer   │
│ - SQLite Database        │
└──────────────────────────┘
       │
       │  Subprocess execution
       │  ICMP ping
       │  System calls (uname)
       │
┌──────▼──────────────────┐
│   Host System            │
│                          │
│ - Shell commands         │
│ - System metrics         │
└──────────────────────────┘
```

### Data Flow

1. **Command Registration**: Client sends `POST /commands` → Stored in SQLite
2. **Synchronous Execution**: `POST /commands/{id}/run` → Daemon spawns process → Returns full output + exit code
3. **Streaming Execution**: `GET /commands/{id}/stream` → Upgraded to WebSocket → Lines streamed in real time
4. **Job Scheduling**: Client creates job via `POST /jobs` → Scheduler polls every 30 s → Auto-executes on schedule
5. **Server Monitoring**: Client requests `GET /servers/{id}/status` → Daemon pings host and collects `uname` output

---

## Components

### hm-daemon (Rust / Tokio)

The core server process that runs continuously on port 3000.

| File | Responsibility |
|---|---|
| `src/main.rs` | Application entry point, HTTP router setup, OTP generation |
| `src/handlers.rs` | All endpoint handlers (commands, jobs, servers, auth) |
| `src/executor.rs` | Subprocess execution with timeout enforcement |
| `src/scheduler.rs` | 30-second polling loop; dispatches overdue jobs |
| `src/auth.rs` | Bearer token middleware |
| `src/state.rs` | SQLite pool initialization and schema creation |
| `src/models.rs` | Shared data structures (`CommandSpec`, `Job`, `Server`, etc.) |
| `src/telemetry.rs` | ICMP ping and system info (`uname`) collection |

**Database Schema:**

```sql
commands
├── id             TEXT PRIMARY KEY
├── name           TEXT NOT NULL
├── command        TEXT NOT NULL   -- JSON array of args
├── working_dir    TEXT
└── timeout_seconds INTEGER

jobs
├── id             TEXT PRIMARY KEY
├── name           TEXT NOT NULL
├── schedule       TEXT NOT NULL   -- 6-field cron expression
├── command_id     TEXT            -- FK → commands.id ON DELETE CASCADE
├── last_run       TEXT            -- ISO 8601 or NULL
└── next_run       TEXT NOT NULL   -- ISO 8601

servers
├── id             TEXT PRIMARY KEY
├── name           TEXT NOT NULL
└── host           TEXT NOT NULL

tokens
├── token          TEXT PRIMARY KEY  -- UUID v4
└── created_at     TEXT NOT NULL     -- ISO 8601
```

### hm-client (Kotlin / Jetpack Compose)

Native Android application for interacting with the daemon.

| File | Responsibility |
|---|---|
| `app/src/main/java/helmsman/client/MainActivity.kt` | Main activity with Compose UI |
| `app/src/main/java/helmsman/client/ui/theme/` | Material 3 theming |
| `app/build.gradle.kts` | Gradle dependencies (Retrofit, Room, OkHttp) |
| `app/src/main/AndroidManifest.xml` | App permissions and activity metadata |

- **UI**: Jetpack Compose (declarative, Material 3)
- **Networking**: Retrofit + OkHttp (REST and WebSocket)
- **Local Storage**: Android Room database
- **Async**: Kotlin Coroutines
- **Target SDK**: 36 · Min SDK: 30
- **Required Permission**: `android.permission.INTERNET`

---

## Technology Stack

### Backend (hm-daemon)

| Technology | Purpose | Version |
|---|---|---|
| **Rust** | Language | Edition 2024 |
| **Tokio** | Async runtime | 1.50.0 |
| **Axum** | Web framework | 0.8.8 |
| **rusqlite** | SQLite driver | 0.38.0 |
| **r2d2** | Connection pooling | 0.8.10 |
| **Serde** | Serialization | 1.0.228 |
| **cron** | Cron expression parser | 0.15.0 |
| **surge-ping** | ICMP ping | 0.8.1 |
| **uuid** | UUID v4 generation | 1.0 |
| **chrono** | Date/time handling | 0.4.44 |

### Frontend (hm-client)

| Technology | Purpose |
|---|---|
| **Kotlin** | Language |
| **Jetpack Compose** | Declarative UI framework |
| **Retrofit** | Type-safe HTTP client |
| **OkHttp** | HTTP + WebSocket transport |
| **Android Room** | Local SQLite ORM |
| **Kotlin Coroutines** | Structured async / await |
| **Android SDK** | Platform (target 36, min 30) |

---

## Features

### Command Execution

- **Register commands** with name, argument list, working directory, and timeout
- **Synchronous execution** — blocks until process exits, returns `stdout`, `stderr`, exit code, and duration in ms
- **Real-time streaming** — upgrades HTTP connection to WebSocket and streams output line-by-line as the process runs; connection closes cleanly on exit

### Job Scheduling

- **6-field cron syntax** including a seconds field: `sec min hour day month weekday`
- **Scheduler reliability** — wakes every 30 seconds; if a job was missed it runs on the next wake
- **Job management** — create, list, delete; view `last_run` and `next_run` timestamps
- Jobs execute concurrently when multiple are due in the same scheduler interval

**Common cron patterns:**

| Expression | Schedule |
|---|---|
| `0 * * * * *` | Every minute |
| `0 0 * * * *` | Every hour |
| `0 0 0 * * *` | Daily at midnight |
| `0 0 2 * * *` | Daily at 2 AM UTC |
| `0 30 9 * * 1` | Monday at 9:30 AM |
| `0 */15 * * * *` | Every 15 minutes |
| `0 0 0 1 * *` | First day of each month |

### Server Monitoring

- Register servers by hostname or IPv4 address
- **ICMP ping** — returns latency in milliseconds; graceful error message on failure, not an HTTP error
- **OS identification** — returns `uname -a` from the daemon host (useful for multi-platform fleets)

### Authentication

- **One-time pairing code** — a 6-digit OTP is printed to stdout on daemon startup; valid for a single exchange per process lifetime
- **Bearer token** — exchange the OTP for a persistent UUID v4 token; stored in SQLite and survives restarts
- **Public endpoints** — `GET /status` (health check) and `POST /pair` (token exchange) require no auth
- All other endpoints return `401 Unauthorized` without a valid token

---

## Installation & Build

### Prerequisites

**hm-daemon:**
- Rust 1.70+ (install via [rustup.rs](https://rustup.rs/))
- Standard C build tools (`gcc`/`clang`, `make`)
- On Linux: `CAP_NET_RAW` capability for ICMP ping (see below)

**hm-client:**
- Android Studio 2024.1+
- Android SDK with API level 36 platform
- Gradle 8.7+ (included via wrapper)

### Building hm-daemon

```bash
cd hm-daemon

# Development build
cargo build

# Optimised release binary
cargo build --release
# → target/release/hm-daemon
```

**Grant ICMP capability on Linux** (required for server ping):

```bash
sudo setcap cap_net_raw+ep target/release/hm-daemon
getcap target/release/hm-daemon
# hm-daemon = cap_net_raw+ep
```

### Building hm-client

```bash
cd hm-client

# Debug APK
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Release APK (requires a signing keystore)
./gradlew assembleRelease
```

Install on a connected device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Configuration & Setup

### Running the Daemon

```bash
cd hm-daemon
./target/release/hm-daemon

# Expected output:
# Pairing code: 483921
# Helmsman Daemon listening on: 0.0.0.0:3000
```

The daemon binds to `0.0.0.0:3000` and creates `helmsman.db` in the current working directory on first run. There is no config file — port and database path are currently compile-time defaults.

### Reverse Proxy (Recommended for Production)

Helmsman does not handle TLS itself. Terminate TLS at the proxy and forward to the daemon:

```nginx
# Nginx example
location / {
    proxy_pass http://127.0.0.1:3000;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Authorization $http_authorization;
}
```

### Running as a systemd Service

```ini
[Unit]
Description=Helmsman Daemon
After=network.target

[Service]
Type=simple
ExecStart=/opt/helmsman/hm-daemon
WorkingDirectory=/opt/helmsman
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

### Pairing the Android Client

1. Start the daemon and note the 6-digit pairing code printed to stdout
2. Open the Helmsman Android app
3. Enter the daemon's hostname/IP and port (`3000` by default)
4. Enter the pairing code
5. The app receives and stores a bearer token — you're ready to go

---

## Usage

All examples below use `curl`. Replace the bearer token with your own.

### Execute a Command

**Register:**

```bash
curl -X POST http://localhost:3000/commands \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "id": "disk-usage",
    "name": "Disk Usage",
    "command": ["df", "-h"],
    "working_dir": "/",
    "timeout_seconds": 10
  }'
```

**Run synchronously:**

```bash
curl -X POST http://localhost:3000/commands/disk-usage/run \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000"

# Response:
{
  "exit_code": 0,
  "stdout": "Filesystem      Size  Used Avail Use% Mounted on\n...",
  "stderr": "",
  "duration_ms": 38
}
```

**Stream in real time:**

```bash
websocat "ws://localhost:3000/commands/disk-usage/stream" \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000"

# Output (per line):
# stdout: Filesystem      Size  Used Avail Use% Mounted on
# stdout: /dev/sda1       120G   44G   70G  39% /
# exit: 0
```

### Schedule a Job

**Create a nightly backup at 2 AM UTC:**

```bash
curl -X POST http://localhost:3000/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "id": "nightly-backup",
    "name": "Database Backup",
    "schedule": "0 0 2 * * *",
    "command_id": "backup-db"
  }'
```

**List all jobs:**

```bash
curl http://localhost:3000/jobs \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000"

# Response:
[
  {
    "id": "nightly-backup",
    "name": "Database Backup",
    "schedule": "0 0 2 * * *",
    "command_id": "backup-db",
    "last_run": "2026-03-07T02:00:00Z",
    "next_run": "2026-03-08T02:00:00Z"
  }
]
```

### Monitor a Server

**Register:**

```bash
curl -X POST http://localhost:3000/servers \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000" \
  -d '{
    "id": "nas",
    "name": "Home NAS",
    "host": "192.168.1.42"
  }'
```

**Check status:**

```bash
curl http://localhost:3000/servers/nas/status \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000"

# Response:
{
  "host": "192.168.1.42",
  "ping_ms": 1.834,
  "ping_error": null,
  "uname": "Linux nas 6.1.21-v8+ #1642 SMP PREEMPT Mon Apr  3 17:24:16 BST 2023 aarch64 GNU/Linux"
}
```

---

## API Reference

Full endpoint documentation is in [API.md](API.md), including detailed request/response schemas, error codes, and WebSocket protocol specification.

### Endpoints Summary

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/status` | — | Health check (plain text) |
| `POST` | `/pair` | — | Exchange OTP for bearer token |
| `GET` | `/commands` | ✅ | List all registered commands |
| `POST` | `/commands` | ✅ | Register a new command |
| `DELETE` | `/commands/{id}` | ✅ | Delete a command (cascades to jobs) |
| `POST` | `/commands/{id}/run` | ✅ | Execute command synchronously |
| `GET` | `/commands/{id}/stream` | ✅ | Stream command output via WebSocket |
| `GET` | `/jobs` | ✅ | List scheduled jobs |
| `POST` | `/jobs` | ✅ | Create a scheduled job |
| `DELETE` | `/jobs/{id}` | ✅ | Delete a job |
| `GET` | `/servers` | ✅ | List registered servers |
| `POST` | `/servers` | ✅ | Register a server |
| `DELETE` | `/servers/{id}` | ✅ | Delete a server |
| `GET` | `/servers/{id}/status` | ✅ | Ping server and retrieve uname |

### Authentication

All protected endpoints require:

```
Authorization: Bearer <UUID-v4-token>
```

Obtain a token by calling `POST /pair` with the OTP printed at daemon startup:

```bash
curl -X POST http://localhost:3000/pair \
  -H "Content-Type: application/json" \
  -d '{"code": "483921"}'

# Response:
{"token": "550e8400-e29b-41d4-a716-446655440000"}
```

---

## Security

### Important Caveats

> ⚠️ **No application-level TLS** — deploy behind an Nginx/HAProxy/Caddy reverse proxy with TLS.
>
> ⚠️ **No token expiry** — tokens are permanent until manually deleted from the `tokens` table in `helmsman.db`.
>
> ⚠️ **Process execution privileges** — commands run as the same OS user as the daemon. Use a dedicated low-privilege account.
>
> ⚠️ **ICMP requires `CAP_NET_RAW`** — either grant the capability or run as root (not recommended for production).

### Best Practices

- Run the daemon as a dedicated non-root system user
- Store `helmsman.db` in a directory readable only by that user (`chmod 700`)
- Always place behind a TLS-terminating reverse proxy in production
- Set `timeout_seconds` on all commands to prevent runaway processes
- Restrict which commands are registered to the minimum necessary
- Avoid putting secrets in command arguments — they will be visible in the database

### Revoking a Token

```bash
sqlite3 helmsman.db "DELETE FROM tokens WHERE token = '<token>';"
```

Restart the daemon after deletion to invalidate any in-flight sessions.

---

## Troubleshooting

**Daemon won't start — port already in use:**
```bash
lsof -i :3000
# Kill conflicting process or change the binding port
```

**ICMP ping returns an error:**
```bash
# Verify capability is set
getcap target/release/hm-daemon

# Grant if missing
sudo setcap cap_net_raw+ep target/release/hm-daemon
```

**WebSocket connection rejected by reverse proxy:**
- Ensure proxy passes `Upgrade` and `Connection` headers (see Nginx example above)
- Confirm the client sends the `Authorization` header during the HTTP handshake

**Command times out unexpectedly:**
- Increase `timeout_seconds` on the command definition
- Check server load: `uptime`, `top`
- Review daemon logs for subprocess errors

---

## Project Structure

```
Helmsman/
├── hm-daemon/                  # Rust HTTP/WebSocket server
│   ├── src/
│   │   ├── main.rs             # Entry point and router
│   │   ├── handlers.rs         # Request handlers
│   │   ├── executor.rs         # Command subprocess execution
│   │   ├── scheduler.rs        # Cron job scheduler
│   │   ├── auth.rs             # Bearer token middleware
│   │   ├── state.rs            # DB pool and schema
│   │   ├── models.rs           # Shared data structures
│   │   └── telemetry.rs        # Ping and system info
│   ├── Cargo.toml
│   └── Cargo.lock
│
├── hm-client/                  # Kotlin Android app
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/helmsman/client/   # Kotlin source
│   │   │   └── res/                    # Android resources
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew
│
└── API.md                      # Full API documentation
```
