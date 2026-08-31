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

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpSessionManager {
    private final ConcurrentMap<String, McpSession> mySessions = new ConcurrentHashMap<>();

    public McpSession create(String protocolVersion) {
        McpSession session = new McpSession(UUID.randomUUID().toString(), protocolVersion);
        mySessions.put(session.getId(), session);
        return session;
    }

    public @Nullable McpSession find(@Nullable String sessionId) {
        return sessionId == null ? null : mySessions.get(sessionId);
    }

    public boolean terminate(@Nullable String sessionId) {
        return sessionId != null && mySessions.remove(sessionId) != null;
    }
}
