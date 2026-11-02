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
package consulo.mcpServer.impl.internal.activity;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
public final class McpToolCallRecord {
    private final long myStartedAt;
    private final String myClientName;
    private final @Nullable String myProjectName;
    private final String myToolName;
    private final String myArguments;

    private volatile McpToolCallStatus myStatus = McpToolCallStatus.RUNNING;
    private volatile long myFinishedAt;
    private volatile String myResult = "";

    public McpToolCallRecord(long startedAt, String clientName, @Nullable String projectName, String toolName, String arguments) {
        myStartedAt = startedAt;
        myClientName = clientName;
        myProjectName = projectName;
        myToolName = toolName;
        myArguments = arguments;
    }

    public long getStartedAt() {
        return myStartedAt;
    }

    public String getClientName() {
        return myClientName;
    }

    public @Nullable String getProjectName() {
        return myProjectName;
    }

    public String getToolName() {
        return myToolName;
    }

    public String getArguments() {
        return myArguments;
    }

    public McpToolCallStatus getStatus() {
        return myStatus;
    }

    public long getFinishedAt() {
        return myFinishedAt;
    }

    public String getResult() {
        return myResult;
    }

    void setStatus(McpToolCallStatus status) {
        myStatus = status;
    }

    void finish(McpToolCallStatus status, String result, long finishedAt) {
        myResult = result;
        myFinishedAt = finishedAt;
        myStatus = status;
    }
}
