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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpSessionManager {
    private static final long IDLE_TIMEOUT_MILLIS = TimeUnit.HOURS.toMillis(2);

    private final ConcurrentMap<String, McpSession> mySessions = new ConcurrentHashMap<>();

    public McpSession create(String protocolVersion,
                             McpPrincipal principal,
                             String clientName,
                             String clientVersion,
                             @Nullable String projectPath) {
        expireIdle();
        McpSession session = new McpSession(UUID.randomUUID().toString(), protocolVersion, principal, clientName, clientVersion, projectPath);
        mySessions.put(session.getId(), session);
        return session;
    }

    public @Nullable McpSession find(@Nullable String sessionId) {
        if (sessionId == null) {
            return null;
        }
        McpSession session = mySessions.get(sessionId);
        if (session == null) {
            return null;
        }
        if (session.getLastAccess() < System.currentTimeMillis() - IDLE_TIMEOUT_MILLIS) {
            mySessions.remove(sessionId);
            return null;
        }
        session.touch();
        return session;
    }

    public boolean terminate(String sessionId) {
        return mySessions.remove(sessionId) != null;
    }

    public void terminateFor(String principalId) {
        mySessions.values().removeIf(session -> session.getPrincipal().id().equals(principalId));
    }

    public void clear() {
        mySessions.clear();
    }

    private void expireIdle() {
        long threshold = System.currentTimeMillis() - IDLE_TIMEOUT_MILLIS;
        mySessions.values().removeIf(session -> session.getLastAccess() < threshold);
    }
}
