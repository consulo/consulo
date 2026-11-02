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

import consulo.annotation.component.ExtensionImpl;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolGroup;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@ExtensionImpl
@Singleton
public class OpenProjectToolset implements McpToolset {
    private final McpProjectResolver myProjectResolver;

    @Inject
    public OpenProjectToolset(McpProjectResolver projectResolver) {
        myProjectResolver = projectResolver;
    }

    @Override
    public McpToolGroup getGroup() {
        return McpToolGroup.PROJECT_AND_FILES;
    }

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_open_projects")
            .description("Lists the projects open in the IDE, one per line as 'name: absolute base path'. Pass one of these paths as '"
                + McpProjectResolver.PROJECT_PATH_ARGUMENT + "' to the other tools when several projects are open.")
            .readOnly()
            .idempotent()
            .handler(this::getOpenProjects);
    }

    private CompletableFuture<McpToolCallResult> getOpenProjects(McpToolCallContext context) {
        StringBuilder builder = new StringBuilder();
        for (Project project : myProjectResolver.getOpenProjects()) {
            String basePath = project.getBasePath();
            builder.append(project.getName()).append(": ").append(basePath == null ? "(no base directory)" : basePath).append('\n');
        }
        return CompletableFuture.completedFuture(McpToolCallResult.text(builder.isEmpty() ? "No project is open." : builder.toString()));
    }
}
