/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace rs remote_agent
namespace java consulo.enviroment.remoteAgent.protocol

// ============================================================
// Exceptions
// ============================================================

exception AgentException {
    1: required string message
}

exception PermissionException {
    1: required string message,
    2: required string permission   // the permission that was denied (e.g. "fs", "process")
}

// ============================================================
// Process Management
// ============================================================

struct ProcessInfo {
    1: required i64 pid,
    2: required bool alive,
    3: optional string command
}

struct ProcessOutput {
    1: optional binary stdoutData,
    2: optional binary stderrData,
    3: optional i32 exitCode
}

// ============================================================
// File Operations
// ============================================================

struct DownloadInfo {
    1: required string transferId,
    2: required i64 fileSize
}

struct FileInfo {
    1: required string name,
    2: required string path,
    3: required i64 size,
    4: optional i64 lastModified,
    5: required bool directory,
    6: required bool symlink,
    7: required bool hidden,
    8: required bool readable,
    9: required bool writable,
    10: required bool executable,
    11: optional string userName
}

// ============================================================
// System / Environment
// ============================================================

struct SystemInfo {
    1: required string osName,
    2: required string osVersion,
    3: required string arch,
    4: required string hostname,
    // Console encoding for decoding process output (e.g. "UTF-8", "CP866", "CP1251")
    7: required string consoleEncoding,
    // System locale (e.g. "en_US.UTF-8", "uk_UA.UTF-8")
    8: required string locale
}

struct StatInfo {
    1: required i32 cpuCount,
    2: required double cpuLoad,       // CPU usage 0.0–1.0
    3: required i64 totalMemory,      // total RAM in bytes
    4: required i64 usedMemory        // used RAM in bytes
}

struct UserInfo {
    1: required string userName,
    2: required string homePath
}

// ============================================================
// Agent Identity
// ============================================================

struct AgentInfo {
    1: required string agentId,       // e.g. "rust-remote-agent"
    2: required string version,       // e.g. "0.1.0"
    3: required list<string> permissions  // expanded permission groups, e.g. ["fs", "process", "http"]
}

// ============================================================
// HTTP Client
// ============================================================

struct HttpRequest {
    1: required string method,
    2: required string url,
    3: optional map<string, string> headers,
    4: optional binary body
}

struct HttpResponse {
    1: required i32 statusCode,
    2: required binary body,
    3: optional map<string, string> headers
}

// ============================================================
// WebSocket Proxy
// ============================================================

struct WebSocketMessage {
    1: optional binary binaryData,
    2: optional string textData
}

struct WebSocketData {
    1: required list<WebSocketMessage> messages,
    2: required bool connected
}

// ============================================================
// Service
// ============================================================

service RemoteAgentService {

    // --- Agent Identity ---

    AgentInfo getAgentInfo(),

    // --- Workspace ---

    // Returns the workspace root directory path.
    string getWorkspacePath(),

    // --- Process Management ---

    ProcessInfo startProcess(
        1: required string command,
        2: required list<string> arguments,
        3: optional string workingDirectory,
        4: optional map<string, string> environment
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    // force=false: SIGTERM / Ctrl+C (graceful)
    // force=true:  SIGKILL / TerminateProcess (hard)
    bool killProcess(
        1: required i64 pid,
        2: required bool force
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    bool isProcessAlive(
        1: required i64 pid
    ) throws (1: PermissionException permissionError),

    list<ProcessInfo> listProcesses()
        throws (1: PermissionException permissionError),

    // Returns new output since last read (streaming poll).
    // ANSI escape codes are preserved as raw bytes.
    ProcessOutput readProcessOutput(
        1: required i64 pid
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    // --- File Operations ---

    binary readFile(
        1: required string path
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    void writeFile(
        1: required string path,
        2: required binary data
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    bool deleteFile(
        1: required string path
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    list<FileInfo> listDirectory(
        1: required string path
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    bool fileExists(
        1: required string path
    ) throws (1: PermissionException permissionError),

    void createDirectory(
        1: required string path,
        2: required bool recursive
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    list<FileInfo> listRoots()
        throws (1: PermissionException permissionError),

    // Set POSIX permissions (octal mode, e.g. 0755).
    // On Windows this is a no-op that returns false.
    bool setPermissions(
        1: required string path,
        2: required i32 mode
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    // --- File Transfer (chunked) ---

    // Upload: host -> agent
    string beginUpload(
        1: required string path,
        2: required i64 fileSize
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    void uploadChunk(
        1: required string transferId,
        2: required binary data
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    void finishUpload(
        1: required string transferId
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    void cancelUpload(
        1: required string transferId
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    // Download: agent -> host
    DownloadInfo beginDownload(
        1: required string path
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    binary downloadChunk(
        1: required string transferId,
        2: required i32 chunkSize
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    void finishDownload(
        1: required string transferId
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    // --- Environment / System Info ---

    string getEnvVariable(
        1: required string name
    ),

    map<string, string> getEnvVariables(),

    SystemInfo getSystemInfo(),

    StatInfo getStat()
        throws (1: AgentException error, 2: PermissionException permissionError),

    UserInfo getUserInfo()
        throws (1: PermissionException permissionError),

    // --- HTTP Client ---

    HttpResponse executeHttpRequest(
        1: required HttpRequest request
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    // --- WebSocket Proxy ---

    string connectWebSocket(
        1: required string url,
        2: optional map<string, string> headers
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    WebSocketData readWebSocketData(
        1: required string sessionId
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    void sendWebSocketData(
        1: required string sessionId,
        2: optional binary binaryData,
        3: optional string textData
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    void closeWebSocket(
        1: required string sessionId
    ) throws (1: AgentException error, 2: PermissionException permissionError),

    // --- Utility ---

    i32 findFreePort() throws (1: AgentException error)
}
