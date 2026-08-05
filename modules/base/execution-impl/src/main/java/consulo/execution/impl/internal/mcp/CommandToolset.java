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
import consulo.execution.RunContentExecutor;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpProjectPaths;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.process.local.ProcessHandlerFactory;
import consulo.process.ProcessOutputTypes;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Runs a command as a process and shows it in the Run tool window. Consulo has no cross-platform
 * terminal widget, so the Run console stands in for one - it works on every frontend, and the user
 * still sees what the agent started.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
@Singleton
public class CommandToolset implements McpToolset {
    private static final int MAX_OUTPUT_LENGTH = 100_000;

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("execute_command")
            .description("Runs a command as a process in the project directory and returns its exit code and output. The "
                + "process is shown in the Run tool window, so the user can watch and stop it. The command is not passed "
                + "through a shell, so pipes, redirects and shell built-ins do not work - pass the executable and its "
                + "arguments separately.")
            .openWorld()
            .requiresProject()
            .param(McpParameter.string("command", "Executable to run."))
            .param(McpParameter.stringArray("arguments", "Arguments passed to the executable.").optional())
            .param(McpParameter.string("workDirectory", "Project-relative working directory. Empty means the project root.")
                       .defaultValue(""))
            .handler(CommandToolset::executeCommand);
    }

    private static CompletableFuture<McpToolCallResult> executeCommand(McpToolCallContext context) {
        Project project = context.getProject();
        String command = context.getString("command");
        List<String> arguments = context.getStringList("arguments");
        String workDirectory = context.getString("workDirectory");

        CompletableFuture<McpToolCallResult> result = new CompletableFuture<>();

        McpToolActions.uiAction(project, () -> {
            VirtualFile directory = McpProjectPaths.resolve(project, workDirectory);

            GeneralCommandLine commandLine = new GeneralCommandLine();
            commandLine.setExePath(command);
            commandLine.addParameters(arguments);
            commandLine.withWorkDirectory(directory.getPath());

            ProcessHandler processHandler;
            try {
                processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine);
            }
            catch (ExecutionException e) {
                throw new McpToolException("Cannot start '" + command + "': " + e.getMessage());
            }

            StringBuilder output = new StringBuilder();
            processHandler.addProcessListener(new ProcessListener() {
                @Override
                public void onTextAvailable(ProcessEvent event, Key outputType) {
                    if (outputType == ProcessOutputTypes.SYSTEM || output.length() >= MAX_OUTPUT_LENGTH) {
                        return;
                    }
                    output.append(event.getText());
                }

                @Override
                public void processTerminated(ProcessEvent event) {
                    result.complete(describe(event.getExitCode(), output));
                }
            });

            // shows the console and starts the process
            new RunContentExecutor(project, processHandler)
                .withTitle(command)
                .withActivateToolWindow(false)
                .run();

            return McpToolCallResult.success();
        }).exceptionally(throwable -> {
            result.completeExceptionally(throwable);
            return null;
        });

        return result;
    }

    private static McpToolCallResult describe(int exitCode, StringBuilder output) {
        StringBuilder builder = new StringBuilder("exit code: ").append(exitCode).append('\n');
        if (output.length() >= MAX_OUTPUT_LENGTH) {
            builder.append(output, 0, MAX_OUTPUT_LENGTH).append("\n... output truncated");
        }
        else {
            builder.append(output);
        }
        return exitCode == 0 ? McpToolCallResult.text(builder.toString()) : McpToolCallResult.error(builder.toString());
    }
}
