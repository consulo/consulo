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
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "McpServerSettings", storages = @Storage("mcp.server"))
public class McpServerSettings implements PersistentStateComponent<McpServerSettingsState> {
    public static final int DEFAULT_PORT = 64342;

    private volatile McpServerSettingsState myState = new McpServerSettingsState();

    @Override
    public McpServerSettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(McpServerSettingsState state) {
        myState = state;
    }

    public boolean isEnabled() {
        return myState.ENABLED;
    }

    public void setEnabled(boolean enabled) {
        myState.ENABLED = enabled;
    }

    public int getPort() {
        return myState.PORT;
    }

    public void setPort(int port) {
        myState.PORT = port;
    }

    public boolean isRequireApproval() {
        return myState.REQUIRE_APPROVAL;
    }

    public void setRequireApproval(boolean requireApproval) {
        myState.REQUIRE_APPROVAL = requireApproval;
    }

    public boolean isStaticTokenEnabled() {
        return myState.STATIC_TOKEN_ENABLED;
    }

    public void setStaticTokenEnabled(boolean staticTokenEnabled) {
        myState.STATIC_TOKEN_ENABLED = staticTokenEnabled;
    }

    public synchronized boolean isToolEnabled(String toolName) {
        return !disabledTools().contains(toolName);
    }

    public synchronized void setToolEnabled(String toolName, boolean enabled) {
        if (enabled) {
            disabledTools().remove(toolName);
        }
        else {
            disabledTools().add(toolName);
        }
    }

    public synchronized Set<String> copyDisabledTools() {
        return new HashSet<>(disabledTools());
    }

    public synchronized void replaceDisabledTools(Set<String> disabledTools) {
        myState.DISABLED_TOOLS = new HashSet<>(disabledTools);
    }

    public synchronized boolean isAlwaysAllowed(String principalId, String toolName) {
        return alwaysAllowedTools().contains(grantKey(principalId, toolName));
    }

    public synchronized void allowAlways(String principalId, String toolName) {
        alwaysAllowedTools().add(grantKey(principalId, toolName));
    }

    public synchronized void forgetApprovals() {
        alwaysAllowedTools().clear();
    }

    public synchronized void forgetApprovals(String principalId) {
        String prefix = principalId + "\t";
        alwaysAllowedTools().removeIf(key -> key.startsWith(prefix));
    }

    public List<McpOAuthClientState> oauthClients() {
        List<McpOAuthClientState> clients = myState.OAUTH_CLIENTS;
        if (clients == null) {
            clients = new ArrayList<>();
            myState.OAUTH_CLIENTS = clients;
        }
        return clients;
    }

    private Set<String> disabledTools() {
        Set<String> tools = myState.DISABLED_TOOLS;
        if (tools == null) {
            tools = new HashSet<>();
            myState.DISABLED_TOOLS = tools;
        }
        return tools;
    }

    private Set<String> alwaysAllowedTools() {
        Set<String> tools = myState.ALWAYS_ALLOWED_TOOLS;
        if (tools == null) {
            tools = new HashSet<>();
            myState.ALWAYS_ALLOWED_TOOLS = tools;
        }
        return tools;
    }

    private static String grantKey(String principalId, String toolName) {
        return principalId + "\t" + toolName;
    }
}
