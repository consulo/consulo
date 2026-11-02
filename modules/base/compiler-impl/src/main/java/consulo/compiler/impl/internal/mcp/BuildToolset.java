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
package consulo.compiler.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.compiler.CompilerManager;
import consulo.compiler.CompilerMessage;
import consulo.compiler.CompilerMessageCategory;
import consulo.compiler.impl.internal.CompileContextImpl;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolGroup;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class BuildToolset implements McpToolset {
    private static final int MAX_PROBLEMS = 200;
    private static final long BUILD_TIMEOUT_MINUTES = 60;

    @Override
    public McpToolGroup getGroup() {
        return McpToolGroup.BUILD_AND_RUN;
    }

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_project_modules")
            .group(McpToolGroup.PROJECT_AND_FILES)
            .description("Lists the names of all modules of the currently open project, as configured in the project model.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .handler(BuildToolset::getProjectModules);

        registrar.tool("get_project_dependencies")
            .group(McpToolGroup.PROJECT_AND_FILES)
            .description("Lists the dependencies of every project module, or of one named module. Reports the module's configured order "
                + "entries - other modules, libraries and the SDK - in classpath order, not a resolved transitive graph.")
            .readOnly()
            .idempotent()
            .requiresProject()
            .param(McpParameter.string("moduleName", "Restrict the result to this module. Empty means all modules.")
                       .defaultValue(""))
            .handler(BuildToolset::getProjectDependencies);

        registrar.tool("build_project")
            .description("Compiles the project with the IDE's incremental Make, so only what changed since the last build is recompiled. "
                + "Waits for the build to finish and reports the error and warning counts followed by each message as "
                + "'path:line:column SEVERITY message' (at most " + MAX_PROBLEMS + "). The call fails when the build has errors.")
            .requiresProject()
            .handler(BuildToolset::buildProject);
    }

    private static CompletableFuture<McpToolCallResult> getProjectModules(McpToolCallContext context) {
        Project project = context.getProject();

        return McpToolActions.readAction(project, () -> {
            StringBuilder builder = new StringBuilder();
            for (Module module : ModuleManager.getInstance(project).getModules()) {
                builder.append(module.getName()).append('\n');
            }
            return McpToolCallResult.text(builder.toString());
        });
    }

    private static CompletableFuture<McpToolCallResult> getProjectDependencies(McpToolCallContext context) {
        Project project = context.getProject();
        String moduleName = context.getString("moduleName");

        return McpToolActions.readAction(project, () -> {
            StringBuilder builder = new StringBuilder();

            for (Module module : ModuleManager.getInstance(project).getModules()) {
                if (!moduleName.isEmpty() && !moduleName.equals(module.getName())) {
                    continue;
                }

                builder.append(module.getName()).append('\n');
                for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
                    builder.append("  ").append(orderEntry.getPresentableName()).append('\n');
                }
            }

            if (builder.isEmpty()) {
                return McpToolCallResult.error("No such module: " + moduleName);
            }
            return McpToolCallResult.text(builder.toString());
        });
    }

    private static CompletableFuture<McpToolCallResult> buildProject(McpToolCallContext context) {
        Project project = context.getProject();

        CompletableFuture<McpToolCallResult> result = new CompletableFuture<>();

        McpToolActions.uiAction(project, () -> {
            CompilerManager.getInstance(project).make((aborted, errors, warnings, compileContext) -> {
                if (aborted) {
                    result.complete(McpToolCallResult.error("Build was aborted. When the IDE reported a configuration problem "
                        + "(missing SDK, output path), it has to be fixed in the IDE first."));
                    return;
                }
                List<CompilerMessage> problems = compileContext instanceof CompileContextImpl impl ? impl.getProblems() : List.of();
                String text = "errors: " + errors + "\nwarnings: " + warnings + "\n" + describe(project, problems);
                result.complete(errors > 0 ? McpToolCallResult.error(text) : McpToolCallResult.text(text));
            });
            return McpToolCallResult.success();
        }).exceptionally(throwable -> {
            result.completeExceptionally(throwable);
            return null;
        });

        return result.orTimeout(BUILD_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    private static String describe(Project project, List<CompilerMessage> problems) {
        String basePath = project.getBasePath();
        Path base = basePath == null ? null : Path.of(basePath);

        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (CompilerMessage problem : problems) {
            if (count++ >= MAX_PROBLEMS) {
                builder.append("... ").append(problems.size() - MAX_PROBLEMS).append(" more\n");
                break;
            }
            Path file = problem.getFile();
            if (file != null) {
                builder.append(base != null && file.startsWith(base) ? base.relativize(file).toString().replace('\\', '/') : file.toString());
                if (problem.getLine() > 0) {
                    builder.append(':').append(problem.getLine());
                    if (problem.getColumn() > 0) {
                        builder.append(':').append(problem.getColumn());
                    }
                }
                builder.append(' ');
            }
            builder.append(problem.getCategory() == CompilerMessageCategory.ERROR ? "ERROR " : "WARNING ")
                .append(problem.getMessage().get())
                .append('\n');
        }
        return builder.toString();
    }
}
