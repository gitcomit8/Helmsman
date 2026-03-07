# Helmsman Daemon — API Reference

The Helmsman daemon is an HTTP/WebSocket server that exposes a REST API for managing and executing commands, scheduling jobs, and monitoring remote servers. It runs on port `3000` by default.

---

## Contents

1. [Base URL & Transport](#1-base-url--transport)
2. [Authentication](#2-authentication)
3. [Device Pairing](#3-device-pairing)
4. [Health](#4-health)
5. [Commands](#5-commands)
6. [Command Execution — REST](#6-command-execution--rest)
7. [Command Execution — WebSocket Streaming](#7-command-execution--websocket-streaming)
8. [Jobs (Cron Scheduler)](#8-jobs-cron-scheduler)
9. [Servers (Subsystem Monitoring)](#9-servers-subsystem-monitoring)
10. [Data Schemas](#10-data-schemas)
11. [Error Reference](#11-error-reference)

---

## 1. Base URL & Transport

```
http://<host>:3000
ws://<host>:3000
```

All REST endpoints communicate over plain HTTP. One endpoint (`/commands/{id}/stream`) communicates over WebSocket (`ws://`). TLS termination is expected to be handled at the network boundary (reverse proxy, VPN, etc.).

---

## 2. Authentication

All endpoints **except** `GET /status` and `POST /pair` require a bearer token.

### Header format

```
Authorization: Bearer <token>
```

Tokens are UUIDs (v4) issued by `POST /pair`. They are persisted in SQLite and survive daemon restarts. Multiple tokens can be active simultaneously (one per pairing event).

### Responses when authentication fails

| Condition | Status |
|---|---|
| `Authorization` header missing | `401 Unauthorized` |
| `Bearer` prefix absent | `401 Unauthorized` |
| Token not found in database | `401 Unauthorized` |

There is currently no token expiry or revocation endpoint. To invalidate a token, delete it directly from the `tokens` table in `helmsman.db`.

---

## 3. Device Pairing

On every daemon startup, a cryptographically random **6-digit one-time code** is generated and printed to stdout:

```
Pairing code: 042817
```

This code must be submitted to `POST /pair` to receive a bearer token. The code is consumed on first successful use and cannot be reused. A new code is generated on the next restart.

---

### `POST /pair`

Exchange the one-time pairing code for a bearer token.

**Authentication:** None required.

**Request body**

```json
{
  "code": "042817"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `code` | string | yes | The 6-digit OTP printed to daemon stdout on startup |

**Success response — `200 OK`**

```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000"
}
```

| Field | Type | Description |
|---|---|---|
| `token` | string | UUID v4 bearer token. Include in all subsequent requests as `Authorization: Bearer <token>` |

**Error responses**

| Status | Condition |
|---|---|
| `403 Forbidden` | Code is wrong, or the OTP has already been consumed |
| `500 Internal Server Error` | Database failure persisting the token |

**Notes**
- The OTP is cleared immediately after a successful pairing. Subsequent calls with the same code return `403`.
- To re-pair, restart the daemon. A new OTP is printed to stdout.
- Tokens survive daemon restarts because they are stored in SQLite.

---

## 4. Health

### `GET /status`

Returns a plain-text liveness indicator.

**Authentication:** None required.

**Response — `200 OK`**

```
Daemon Operational
```

Content-Type is `text/plain`. Use this endpoint for load balancer health checks or client connectivity probes.

---

## 5. Commands

A **Command** is a registered shell command that the daemon can execute on demand or on a cron schedule. Commands are stored persistently in SQLite.

### Object: `CommandSpec`

```json
{
  "id": "backup-db",
  "name": "Backup Database",
  "command": ["pg_dump", "-U", "postgres", "mydb"],
  "working_dir": "/opt/app",
  "timeout_seconds": 120
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | string | yes | Unique identifier. Used in all subsequent API calls referencing this command. Alphanumeric, hyphens allowed. |
| `name` | string | yes | Human-readable display name. |
| `command` | string[] | yes | The command to run, as an argv array. The first element is the executable; subsequent elements are arguments. Must not be empty. |
| `working_dir` | string \| null | no | Absolute path to set as the working directory before execution. If `null`, the daemon's own working directory is used. |
| `timeout_seconds` | number \| null | no | Maximum wall-clock seconds the process is allowed to run. If the process exceeds this limit it is killed and the run returns an error. If `null`, no timeout is applied. |

---

### `GET /commands`

List all registered commands.

**Authentication:** Required.

**Response — `200 OK`**

```json
[
  {
    "id": "backup-db",
    "name": "Backup Database",
    "command": ["pg_dump", "-U", "postgres", "mydb"],
    "working_dir": "/opt/app",
    "timeout_seconds": 120
  },
  {
    "id": "echo-hello",
    "name": "Echo Hello",
    "command": ["echo", "hello"],
    "working_dir": null,
    "timeout_seconds": null
  }
]
```

Returns an empty array `[]` if no commands are registered.

---

### `POST /commands`

Register a new command, or replace an existing one with the same `id`.

**Authentication:** Required.

**Request body:** `CommandSpec` object (see schema above).

```json
{
  "id": "restart-nginx",
  "name": "Restart Nginx",
  "command": ["systemctl", "restart", "nginx"],
  "working_dir": null,
  "timeout_seconds": 30
}
```

**Success response — `201 Created`**

Empty body.

**Error responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | Request body is not valid JSON or is missing required fields |
| `500 Internal Server Error` | Database write failure |

**Notes**
- Uses `INSERT OR REPLACE`, so posting a command with an existing `id` overwrites it entirely.
- Deleting a command that is referenced by a scheduled job will also delete the job (foreign key `ON DELETE CASCADE`).

---

### `DELETE /commands/{id}`

Remove a registered command.

**Authentication:** Required.

**Path parameters**

| Parameter | Description |
|---|---|
| `id` | The `id` of the command to delete |

**Success response — `204 No Content`**

Empty body.

**Error responses**

| Status | Condition |
|---|---|
| `404 Not Found` | No command with this `id` exists |
| `500 Internal Server Error` | Database failure |

---

## 6. Command Execution — REST

### `POST /commands/{id}/run`

Execute a registered command synchronously and return the full output once the process exits.

**Authentication:** Required.

**Path parameters**

| Parameter | Description |
|---|---|
| `id` | The `id` of the command to execute |

**Request body:** None.

**Success response — `200 OK`**

```json
{
  "exit_code": 0,
  "stdout": "backup complete\n",
  "stderr": "",
  "duration_ms": 4821
}
```

| Field | Type | Description |
|---|---|---|
| `exit_code` | number \| null | The process exit code. `null` if the process was killed by a signal. |
| `stdout` | string | Full standard output captured from the process. |
| `stderr` | string | Full standard error captured from the process. |
| `duration_ms` | number | Wall-clock time in milliseconds from process spawn to exit. |

**Error responses**

| Status | Condition |
|---|---|
| `404 Not Found` | No command with this `id` exists |
| `500 Internal Server Error` | Process could not be spawned, or the timeout was exceeded |

**Notes**
- This endpoint blocks until the process finishes. For long-running commands, prefer the WebSocket streaming endpoint.
- If `timeout_seconds` is set on the command and the process exceeds it, the process is killed and `500` is returned with the message `"Process execution exceeded timeout threshold"` in the daemon logs. The response body for this case is also `500`.
- stdout and stderr are captured as UTF-8. Non-UTF-8 bytes are replaced with the Unicode replacement character.

---

## 7. Command Execution — WebSocket Streaming

### `GET /commands/{id}/stream`

Execute a registered command and stream its output line-by-line over a WebSocket connection in real time.

**Authentication:** Required. Pass the bearer token as a header during the HTTP upgrade handshake:

```
Authorization: Bearer <token>
```

> Most WebSocket clients (browsers, `websocat`) support custom headers on the initial HTTP upgrade request.

**Path parameters**

| Parameter | Description |
|---|---|
| `id` | The `id` of the command to execute |

**Protocol**

The connection is established via a standard HTTP → WebSocket upgrade (`101 Switching Protocols`). The server immediately spawns the process and begins streaming.

Each message is a UTF-8 text frame in one of three formats:

| Format | Description |
|---|---|
| `stdout: <line>` | A line of text written to the process's standard output |
| `stderr: <line>` | A line of text written to the process's standard error |
| `exit: <code>` | The process has exited. `<code>` is the integer exit code, or `-1` if killed by a signal |
| `error: <message>` | The process could not be spawned (e.g. executable not found). The connection closes after this frame. |

The server closes the WebSocket connection cleanly after the `exit:` frame is sent.

**Example session (`websocat`)**

```bash
websocat "ws://localhost:3000/commands/count/stream" \
  -H "Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000"

# Server sends:
stdout: line 1
stdout: line 2
stderr: warning: something
stdout: line 3
exit: 0
```

**Error handling**

| Condition | Behaviour |
|---|---|
| Command `id` not found | HTTP `404` is returned during upgrade — connection is never established |
| Process fails to spawn | `error: <reason>` text frame is sent, then connection closes |
| Client disconnects early | The spawned process continues to run but output is discarded |

**Notes**
- stdout and stderr are interleaved in arrival order. There is no guaranteed ordering between a stdout line and a stderr line written at the same instant.
- Lines are newline-delimited. The newline character itself is stripped from the frame content.
- There is no timeout enforcement on streaming runs. Set `timeout_seconds` on the command definition if needed and use `POST /commands/{id}/run` instead, which enforces the timeout.

---

## 8. Jobs (Cron Scheduler)

A **Job** links a `CommandSpec` to a cron schedule. The daemon runs an internal scheduler that wakes every 30 seconds, checks for due jobs, and dispatches them using the same executor as `POST /commands/{id}/run`.

### Object: `Job`

```json
{
  "id": "nightly-backup",
  "name": "Nightly Database Backup",
  "schedule": "0 0 2 * * *",
  "command_id": "backup-db",
  "last_run": "2026-03-07T02:00:00Z",
  "next_run": "2026-03-08T02:00:00Z"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | string | Unique identifier for this job |
| `name` | string | Human-readable display name |
| `schedule` | string | Cron expression (6-field with seconds: `sec min hour day month weekday`) |
| `command_id` | string | `id` of the `CommandSpec` to execute |
| `last_run` | string \| null | ISO 8601 UTC timestamp of the last execution. `null` if it has never run. |
| `next_run` | string | ISO 8601 UTC timestamp of the next scheduled execution |

### Cron expression format

The daemon uses 6-field cron syntax (with a leading seconds field):

```
┌──────────── second (0–59)
│ ┌────────── minute (0–59)
│ │ ┌──────── hour (0–23)
│ │ │ ┌────── day of month (1–31)
│ │ │ │ ┌──── month (1–12)
│ │ │ │ │ ┌── day of week (0–6, Sunday=0)
│ │ │ │ │ │
* * * * * *
```

| Expression | Meaning |
|---|---|
| `0 * * * * *` | Every minute, on the minute |
| `0 0 * * * *` | Every hour, on the hour |
| `0 0 2 * * *` | Every day at 02:00:00 UTC |
| `0 30 9 * * 1` | Every Monday at 09:30:00 UTC |
| `0 */15 * * * *` | Every 15 minutes |

---

### `GET /jobs`

List all scheduled jobs.

**Authentication:** Required.

**Response — `200 OK`**

```json
[
  {
    "id": "nightly-backup",
    "name": "Nightly Database Backup",
    "schedule": "0 0 2 * * *",
    "command_id": "backup-db",
    "last_run": "2026-03-07T02:00:00Z",
    "next_run": "2026-03-08T02:00:00Z"
  }
]
```

Returns an empty array `[]` if no jobs are scheduled.

---

### `POST /jobs`

Schedule a new job.

**Authentication:** Required.

**Request body**

```json
{
  "id": "nightly-backup",
  "name": "Nightly Database Backup",
  "schedule": "0 0 2 * * *",
  "command_id": "backup-db"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | string | yes | Unique identifier for the job |
| `name` | string | yes | Human-readable display name |
| `schedule` | string | yes | 6-field cron expression |
| `command_id` | string | yes | Must reference an existing command `id` |

**Success response — `201 Created`**

Empty body. The `next_run` timestamp is computed from the cron expression at creation time.

**Error responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | `schedule` is not a valid cron expression, or body is malformed |
| `500 Internal Server Error` | `command_id` does not reference an existing command (foreign key violation), or database failure |

**Notes**
- Uses `INSERT OR REPLACE`. Posting a job with an existing `id` replaces it and resets `last_run` to `null`.
- The referenced command must exist at creation time. Deleting the command later will cascade-delete this job.
- `next_run` is calculated from `Utc::now()` at the time of the request.

---

### `DELETE /jobs/{id}`

Remove a scheduled job.

**Authentication:** Required.

**Path parameters**

| Parameter | Description |
|---|---|
| `id` | The `id` of the job to delete |

**Success response — `204 No Content`**

Empty body.

**Error responses**

| Status | Condition |
|---|---|
| `404 Not Found` | No job with this `id` exists |
| `500 Internal Server Error` | Database failure |

---

### Scheduler behaviour

- The scheduler wakes on a **30-second fixed interval** and processes all jobs where `next_run <= now`.
- If the daemon is offline when a job was scheduled to run, the job fires on the next scheduler tick after restart (it will be overdue).
- After each execution, `last_run` is set to the time of dispatch and `next_run` is advanced to the following occurrence per the cron expression.
- Job execution output is logged to daemon stderr but not persisted. Use `POST /commands/{id}/run` or the WebSocket stream if you need to capture output.
- There is no concurrency limit. If two jobs are due at the same tick they run concurrently.

---

## 9. Servers (Subsystem Monitoring)

A **Server** is a host that the daemon can probe for reachability (ICMP ping) and OS identity (`uname -a`).

> **Capability requirement:** ICMP ping requires `CAP_NET_RAW` on Linux. Either run the daemon as root or grant the capability to the binary:
> ```bash
> sudo setcap cap_net_raw+ep target/release/hm-daemon
> ```

### Object: `Server`

```json
{
  "id": "pi-home",
  "name": "Home Raspberry Pi",
  "host": "192.168.1.42"
}
```

| Field | Type | Description |
|---|---|---|
| `id` | string | Unique identifier |
| `name` | string | Human-readable display name |
| `host` | string | IPv4 address or resolvable hostname |

---

### `GET /servers`

List all registered servers.

**Authentication:** Required.

**Response — `200 OK`**

```json
[
  {
    "id": "pi-home",
    "name": "Home Raspberry Pi",
    "host": "192.168.1.42"
  },
  {
    "id": "vps-de",
    "name": "VPS Frankfurt",
    "host": "203.0.113.10"
  }
]
```

---

### `POST /servers`

Register a server.

**Authentication:** Required.

**Request body**

```json
{
  "id": "pi-home",
  "name": "Home Raspberry Pi",
  "host": "192.168.1.42"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | string | yes | Unique identifier |
| `name` | string | yes | Display name |
| `host` | string | yes | IPv4 address or hostname. DNS resolution is performed at probe time, not at registration. |

**Success response — `201 Created`**

Empty body.

**Error responses**

| Status | Condition |
|---|---|
| `400 Bad Request` | Body is malformed or missing required fields |
| `500 Internal Server Error` | Database failure |

**Notes**
- Uses `INSERT OR REPLACE`. Posting with an existing `id` overwrites the entry.
- The `host` value is not validated at registration time. Invalid or unresolvable hosts will fail at probe time with an error in the status response.

---

### `DELETE /servers/{id}`

Remove a registered server.

**Authentication:** Required.

**Path parameters**

| Parameter | Description |
|---|---|
| `id` | The `id` of the server to remove |

**Success response — `204 No Content`**

Empty body.

**Error responses**

| Status | Condition |
|---|---|
| `404 Not Found` | No server with this `id` exists |
| `500 Internal Server Error` | Database failure |

---

### `GET /servers/{id}/status`

Probe a registered server. Performs an ICMP ping and collects OS identity from the **local** machine.

**Authentication:** Required.

**Path parameters**

| Parameter | Description |
|---|---|
| `id` | The `id` of the server to probe |

**Response — `200 OK`**

```json
{
  "host": "192.168.1.42",
  "ping_ms": 1.243,
  "ping_error": null,
  "uname": "Linux raspberrypi 6.1.21-v8+ #1642 SMP PREEMPT Mon Apr  3 17:24:16 BST 2023 aarch64 GNU/Linux"
}
```

| Field | Type | Description |
|---|---|---|
| `host` | string | The host value stored for this server |
| `ping_ms` | number \| null | Round-trip ICMP ping latency in milliseconds. `null` if the ping failed. |
| `ping_error` | string \| null | Error message if the ping failed. `null` on success. |
| `uname` | string | Output of `uname -a` run on the **daemon's local machine**, not the remote host. Useful for identifying the Pi's own OS. |

**Error responses**

| Status | Condition |
|---|---|
| `404 Not Found` | No server with this `id` exists |
| `500 Internal Server Error` | Database failure acquiring the host |

**Notes**
- A failed ping (host unreachable, timeout, no `CAP_NET_RAW`) does **not** return an HTTP error. The response is still `200 OK` with `ping_ms: null` and a description in `ping_error`. This allows clients to distinguish between "server exists but is unreachable" and "server ID not found".
- Hostname resolution is synchronous and blocks the handler thread. Prefer IP addresses for registered hosts to avoid DNS latency.
- `uname` is always the daemon host's identity, not the monitored server's. It is included to identify the platform the daemon is running on.

---

## 10. Data Schemas

### CommandSpec

```json
{
  "id":              "string (required)",
  "name":            "string (required)",
  "command":         ["string", "..."] ,
  "working_dir":     "string | null",
  "timeout_seconds": "number | null"
}
```

### JobResult

```json
{
  "exit_code":   "number | null",
  "stdout":      "string",
  "stderr":      "string",
  "duration_ms": "number"
}
```

### Job

```json
{
  "id":         "string",
  "name":       "string",
  "schedule":   "string (6-field cron)",
  "command_id": "string",
  "last_run":   "string (ISO 8601) | null",
  "next_run":   "string (ISO 8601)"
}
```

### JobCreateRequest

```json
{
  "id":         "string (required)",
  "name":       "string (required)",
  "schedule":   "string (required)",
  "command_id": "string (required)"
}
```

### Server

```json
{
  "id":   "string",
  "name": "string",
  "host": "string"
}
```

### ServerStatus

```json
{
  "host":        "string",
  "ping_ms":     "number | null",
  "ping_error":  "string | null",
  "uname":       "string"
}
```

### PairRequest

```json
{
  "code": "string (required)"
}
```

### PairResponse

```json
{
  "token": "string (UUID v4)"
}
```

---

## 11. Error Reference

All error responses use standard HTTP status codes. The response body for errors is empty unless stated otherwise.

| Code | Meaning | Common causes |
|---|---|---|
| `200 OK` | Success (with body) | — |
| `201 Created` | Resource created | POST to commands, jobs, servers |
| `204 No Content` | Success (no body) | DELETE operations |
| `400 Bad Request` | Client-side input error | Malformed JSON, invalid cron expression |
| `401 Unauthorized` | Missing or invalid token | All protected routes when auth fails |
| `403 Forbidden` | Valid request but action denied | Wrong or consumed pairing OTP |
| `404 Not Found` | Resource does not exist | Unknown `id` in path parameters |
| `500 Internal Server Error` | Daemon-side failure | DB connection error, process spawn failure, timeout exceeded |

---

## Quick Reference

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/status` | No | Liveness check |
| `POST` | `/pair` | No | Exchange OTP for bearer token |
| `GET` | `/commands` | Yes | List all commands |
| `POST` | `/commands` | Yes | Register a command |
| `DELETE` | `/commands/{id}` | Yes | Delete a command |
| `POST` | `/commands/{id}/run` | Yes | Execute command, return full output |
| `GET` | `/commands/{id}/stream` | Yes | Execute command, stream output over WebSocket |
| `GET` | `/jobs` | Yes | List all scheduled jobs |
| `POST` | `/jobs` | Yes | Schedule a new job |
| `DELETE` | `/jobs/{id}` | Yes | Remove a scheduled job |
| `GET` | `/servers` | Yes | List all monitored servers |
| `POST` | `/servers` | Yes | Register a server |
| `DELETE` | `/servers/{id}` | Yes | Remove a server |
| `GET` | `/servers/{id}/status` | Yes | Ping server and return uname |
