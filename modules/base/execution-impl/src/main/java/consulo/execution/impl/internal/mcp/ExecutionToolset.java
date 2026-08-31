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
package consulo.execution.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.process.ExecutionException;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class ExecutionToolset implements McpToolset {
    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_run_configurations")
            .description("Lists the run configurations of the currently open project, each with its configuration type.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .handler(ExecutionToolset::getRunConfigurations);

        registrar.tool("execute_run_configuration")
            .description("Starts a run configuration by name using the Run executor. Returns once the launch has been requested, not when "
                + "the process ends.")
            .requiresProject()
            .param(McpParameter.string("name", "Name of the run configuration to start."))
            .handler(ExecutionToolset::executeRunConfiguration);
    }

    private static CompletableFuture<McpToolCallResult> getRunConfigurations(McpToolCallContext context) {
        Project project = context.getProject();

        return McpToolActions.readAction(project, () -> {
            StringBuilder builder = new StringBuilder();
            for (RunnerAndConfigurationSettings settings : RunManager.getInstance(project).getAllSettings()) {
                builder.append(settings.getName())
                    .append(" [")
                    .append(settings.getType().getDisplayName().get())
                    .append("]\n");
            }
            return McpToolCallResult.text(builder.toString());
        });
    }

    private static CompletableFuture<McpToolCallResult> executeRunConfiguration(McpToolCallContext context) {
        Project project = context.getProject();
        String name = context.getString("name");

        return McpToolActions.uiAction(project, () -> {
            RunnerAndConfigurationSettings settings = RunManager.getInstance(project).findConfigurationByName(name);
            if (settings == null) {
                return McpToolCallResult.error("No such run configuration: " + name);
            }

            try {
                ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings).buildAndExecute();
            }
            catch (ExecutionException e) {
                throw new McpToolException("Cannot start '" + name + "': " + e.getMessage());
            }
            return McpToolCallResult.text("Started run configuration: " + name);
        });
    }
}
