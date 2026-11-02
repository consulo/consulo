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
package consulo.mcpServer.impl.internal;

import org.jspecify.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public final class McpSession {
    private final String myId;
    private final String myProtocolVersion;
    private final McpPrincipal myPrincipal;
    private final String myClientName;
    private final String myClientVersion;
    private final @Nullable String myProjectPath;
    private final Set<String> myApprovedTools = ConcurrentHashMap.newKeySet();

    private volatile long myLastAccess = System.currentTimeMillis();

    McpSession(String id,
               String protocolVersion,
               McpPrincipal principal,
               String clientName,
               String clientVersion,
               @Nullable String projectPath) {
        myId = id;
        myProtocolVersion = protocolVersion;
        myPrincipal = principal;
        myClientName = clientName;
        myClientVersion = clientVersion;
        myProjectPath = projectPath;
    }

    public String getId() {
        return myId;
    }

    public String getProtocolVersion() {
        return myProtocolVersion;
    }

    public McpPrincipal getPrincipal() {
        return myPrincipal;
    }

    public boolean belongsTo(McpPrincipal principal) {
        return myPrincipal.id().equals(principal.id());
    }

    public String getClientName() {
        return myClientName;
    }

    public String getClientVersion() {
        return myClientVersion;
    }

    public @Nullable String getProjectPath() {
        return myProjectPath;
    }

    public boolean isToolApproved(String toolName) {
        return myApprovedTools.contains(toolName);
    }

    public void approveTool(String toolName) {
        myApprovedTools.add(toolName);
    }

    long getLastAccess() {
        return myLastAccess;
    }

    void touch() {
        myLastAccess = System.currentTimeMillis();
    }
}
