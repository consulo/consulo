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
package consulo.ai;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import consulo.application.Application;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Contributes the MCP servers every agent session gets - the server of the IDE, which lets an agent search, navigate
 * and refactor with the indexes of the IDE. The dependency runs MCP to AI, like {@link AIToolProvider}.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AIMcpServerProvider {
    /**
     * Empty while the servers do not run.
     */
    List<AIMcpServer> getServers();

    /**
     * The skills of the IDE for an agent working in the directory, as a plugin directory of this session only - the
     * skills under {@code skills/<id>/SKILL.md} beside the manifests the agents read. Null when the agent gets none.
     *
     * @param agent {@value #AGENT_CLAUDE}, {@value #AGENT_CODEX}, {@value #AGENT_CURSOR} or another agent
     */
    default @Nullable Path getSkillPlugin(Path workingDirectory, String agent) {
        return null;
    }

    String AGENT_CLAUDE = "claude";
    String AGENT_CODEX = "codex";
    String AGENT_CURSOR = "cursor";

    /**
     * The skill plugins every provider has for the agent in the directory.
     */
    static List<Path> skillPlugins(Path workingDirectory, String agent) {
        List<Path> plugins = new ArrayList<>();
        Application.get().getExtensionPoint(AIMcpServerProvider.class).forEach(provider -> {
            Path plugin = provider.getSkillPlugin(workingDirectory, agent);
            if (plugin != null) {
                plugins.add(plugin);
            }
        });
        return plugins;
    }
}
