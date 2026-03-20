/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

namespace * consulo.externalSystem.thrift

// ============ Enums ============

enum ThriftTaskType {
  RESOLVE_PROJECT = 0,
  REFRESH_TASKS_LIST = 1,
  EXECUTE_TASK = 2
}

enum ThriftSourceType {
  SOURCE = 0,
  TEST = 1,
  EXCLUDED = 2,
  SOURCE_GENERATED = 3,
  TEST_GENERATED = 4,
  RESOURCE = 5,
  TEST_RESOURCE = 6
}

enum ThriftLibraryPathType {
  BINARY = 0,
  SOURCE = 1,
  DOC = 2
}

enum ThriftLibraryLevel {
  PROJECT = 0,
  MODULE = 1
}

enum ThriftDependencyScope {
  COMPILE = 0,
  TEST = 1,
  RUNTIME = 2,
  PROVIDED = 3
}

// ============ Core Structs ============

struct ThriftProjectSystemId {
  1: required string id
}

struct ThriftTaskId {
  1: required ThriftTaskType type,
  2: required string projectId,
  3: required ThriftProjectSystemId projectSystemId,
  4: required i64 id
}

struct ThriftTaskNotificationEvent {
  1: required ThriftTaskId id,
  2: required string description
}

exception ThriftExternalSystemException {
  1: required string message,
  2: required string originalReason,
  3: required list<string> quickFixes,
  4: optional string filePath,
  5: optional i32 line,
  6: optional i32 column
}

// ============ Project Data Model ============

struct ThriftSourceRoot {
  1: required string path,
  2: optional string packagePrefix
}

struct ThriftContentRootData {
  1: required string rootPath,
  2: required map<ThriftSourceType, list<ThriftSourceRoot>> data
}

struct ThriftLibraryData {
  1: required string externalName,
  2: required string internalName,
  3: required ThriftProjectSystemId owner,
  4: required bool unresolved,
  5: required map<ThriftLibraryPathType, list<string>> paths
}

struct ThriftTaskData {
  1: required string name,
  2: required ThriftProjectSystemId owner,
  3: required string linkedExternalProjectPath,
  4: optional string description
}

struct ThriftModuleData {
  1: required string id,
  2: required string externalName,
  3: required string internalName,
  4: required ThriftProjectSystemId owner,
  5: required string externalConfigPath,
  6: optional string moduleDirPath,
  7: optional string group,
  8: optional string version,
  9: required list<string> artifacts,
  10: required bool inheritProjectCompileOutputPath,
  11: required map<ThriftSourceType, string> compileOutputPaths
}

struct ThriftProjectData {
  1: required string externalName,
  2: required string internalName,
  3: required ThriftProjectSystemId owner,
  4: required string linkedExternalProjectPath,
  5: optional string ideProjectFileDirectoryPath
}

struct ThriftModuleDependencyData {
  1: required ThriftProjectSystemId owner,
  2: required string ownerModuleId,
  3: required string targetModuleId,
  4: required ThriftDependencyScope scope,
  5: required bool exported
}

struct ThriftLibraryDependencyData {
  1: required ThriftProjectSystemId owner,
  2: required string ownerModuleId,
  3: required string targetLibraryName,
  4: required ThriftDependencyScope scope,
  5: required bool exported,
  6: required ThriftLibraryLevel level,
  7: required ThriftLibraryData target
}

// ============ DataNode Tree ============

// A node in the project data tree.
// 'key' identifies the data type (e.g. "ProjectData", "ModuleData", etc.)
// For core types, the corresponding typed field carries the data.
// For plugin-specific types, 'extensionData' carries serialized bytes.
struct ThriftDataNode {
  1: required string key,
  2: required list<ThriftDataNode> children,

  // Core data - exactly one is set based on 'key'
  10: optional ThriftProjectData projectData,
  11: optional ThriftModuleData moduleData,
  12: optional ThriftLibraryData libraryData,
  13: optional ThriftContentRootData contentRootData,
  14: optional ThriftModuleDependencyData moduleDependencyData,
  15: optional ThriftLibraryDependencyData libraryDependencyData,
  16: optional ThriftTaskData taskData,

  // Extension point: plugin-specific data as serialized bytes
  20: optional binary extensionData
}

// ============ Execution Settings ============

// Base settings common to all external systems.
// Plugin-specific settings are passed as string key-value pairs in 'properties'.
struct ThriftExecutionSettings {
  1: required i64 remoteProcessIdleTtlMs,
  2: required bool verboseProcessing,
  3: required map<string, string> properties
}

// ============ Progress Events (polling model) ============

enum ThriftProgressEventType {
  QUEUED = 0,
  START = 1,
  STATUS_CHANGE = 2,
  TASK_OUTPUT = 3,
  END = 4,
  SUCCESS = 5,
  FAILURE = 6
}

struct ThriftProgressEvent {
  1: required ThriftProgressEventType type,
  2: required ThriftTaskId taskId,
  3: optional string description,       // for STATUS_CHANGE
  4: optional string output,            // for TASK_OUTPUT
  5: optional bool stdOut,              // for TASK_OUTPUT
  6: optional ThriftExternalSystemException failure  // for FAILURE
}

// ============ Service ============

// Single service: IDE -> External process
// IDE polls for progress events instead of receiving callbacks
service ExternalSystemFacadeService {
  ThriftDataNode resolveProjectInfo(
    1: ThriftTaskId id,
    2: string projectPath,
    3: bool isPreviewMode,
    4: ThriftExecutionSettings settings
  ) throws (1: ThriftExternalSystemException ex),

  void executeTasks(
    1: ThriftTaskId id,
    2: list<string> taskNames,
    3: string projectPath,
    4: ThriftExecutionSettings settings,
    5: list<string> vmOptions,
    6: list<string> scriptParameters,
    7: string debuggerSetup
  ) throws (1: ThriftExternalSystemException ex),

  bool cancelTask(1: ThriftTaskId id),
  bool isTaskInProgress(1: ThriftTaskId id),
  void applySettings(1: ThriftExecutionSettings settings),

  // Poll for progress events. Returns all buffered events since last poll.
  list<ThriftProgressEvent> pollProgressEvents()
}
