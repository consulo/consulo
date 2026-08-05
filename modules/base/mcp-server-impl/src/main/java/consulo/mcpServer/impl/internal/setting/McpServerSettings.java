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
package consulo.mcpServer.impl.internal.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.inject.Singleton;

import java.util.HashSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "McpServerSettings", storages = @Storage("mcp.server.xml"))
public class McpServerSettings implements PersistentStateComponent<McpServerSettings> {
    public static final int DEFAULT_PORT = 64342;

    /**
     * Exposing the IDE to local agents is opt-in.
     */
    public boolean ENABLED = false;

    public int PORT = DEFAULT_PORT;

    /**
     * Names of tools the user has switched off. Stored as the exceptions rather than the allowed set,
     * so tools added by a later release are exposed by default.
     */
    public Set<String> DISABLED_TOOLS = new HashSet<>();

    public boolean isEnabled() {
        return ENABLED;
    }

    public void setEnabled(boolean enabled) {
        ENABLED = enabled;
    }

    public boolean isToolEnabled(String toolName) {
        return !DISABLED_TOOLS.contains(toolName);
    }

    public void setToolEnabled(String toolName, boolean enabled) {
        if (enabled) {
            DISABLED_TOOLS.remove(toolName);
        }
        else {
            DISABLED_TOOLS.add(toolName);
        }
    }

    public int getPort() {
        return PORT;
    }

    public void setPort(int port) {
        PORT = port;
    }

    @Override
    public McpServerSettings getState() {
        return this;
    }

    @Override
    public void loadState(McpServerSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
