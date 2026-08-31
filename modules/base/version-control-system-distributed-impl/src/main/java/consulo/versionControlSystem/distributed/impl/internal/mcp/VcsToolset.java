/*
 * Copyright 2000-2026 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class VcsToolset implements McpToolset {
    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_repositories")
            .description("Lists the version control repositories the IDE has registered for the project, each with its VCS, current branch "
                + "and state such as NORMAL, MERGING or REBASING. Reads the IDE's model rather than running the VCS command line.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .handler(VcsToolset::getRepositories);
    }

    private static CompletableFuture<McpToolCallResult> getRepositories(McpToolCallContext context) {
        Project project = context.getProject();

        return McpToolActions.readAction(project, () -> {
            StringBuilder builder = new StringBuilder();

            for (Repository repository : VcsRepositoryManager.getInstance(project).getRepositories()) {
                builder.append(McpProjectPaths.relativePath(project, repository.getRoot()))
                    .append(" [")
                    .append(repository.getVcs().getDisplayName())
                    .append("] branch: ")
                    .append(repository.getCurrentBranchName())
                    .append(", state: ")
                    .append(repository.getState())
                    .append('\n');
            }

            return McpToolCallResult.text(builder.toString());
        });
    }
}
