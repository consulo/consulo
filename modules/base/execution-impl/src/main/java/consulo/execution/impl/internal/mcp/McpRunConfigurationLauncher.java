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
package consulo.execution.impl.internal.mcp;

import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.event.ExecutionListener;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.ui.RunContentDescriptor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.mcp.tool.McpToolCallResult;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessOutputTypes;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
final class McpRunConfigurationLauncher {
    @RequiredUIAccess
    static CompletableFuture<McpToolCallResult> launch(Project project,
                                                       RunnerAndConfigurationSettings settings,
                                                       Executor executor,
                                                       int timeoutMillis,
                                                       boolean waitForExit) {
        CompletableFuture<McpRunExecution> started = new CompletableFuture<>();
        Disposable listenerDisposable = Disposable.newDisposable("McpRunConfigurationLauncher");

        ExecutionEnvironment environment;
        try {
            environment = ExecutionEnvironmentBuilder.create(executor, settings).activeTarget().build();
        }
        catch (ExecutionException e) {
            Disposer.dispose(listenerDisposable);
            return CompletableFuture.completedFuture(McpToolCallResult.error("Cannot start '" + settings.getName() + "': " + e.getMessage()));
        }

        project.getMessageBus().connect(listenerDisposable).subscribe(ExecutionListener.class, new ExecutionListener() {
            @Override
            public void processNotStarted(String executorId, ExecutionEnvironment env) {
                if (env == environment) {
                    started.completeExceptionally(new IllegalStateException("The run configuration did not start; see the Run tool window."));
                }
            }
        });

        environment.setCallback(descriptor -> attach(settings.getName(), descriptor, started));
        ProgramRunnerUtil.executeConfiguration(environment, false, true);

        return started.whenComplete((value, error) -> Disposer.dispose(listenerDisposable))
            .thenCompose(execution -> waitForExit
                ? execution.exitCode().<@Nullable Integer>thenApply(code -> code).completeOnTimeout(null, timeoutMillis, TimeUnit.MILLISECONDS)
                    .thenApply(code -> describe(execution, code))
                : CompletableFuture.completedFuture(describe(execution, null)))
            .exceptionally(error -> McpToolCallResult.error("Cannot run '" + settings.getName() + "': " + rootMessage(error)));
    }

    private static void attach(String name, @Nullable RunContentDescriptor descriptor, CompletableFuture<McpRunExecution> started) {
        ProcessHandler processHandler = descriptor == null ? null : descriptor.getProcessHandler();
        if (processHandler == null) {
            started.completeExceptionally(new IllegalStateException("The process has failed to start."));
            return;
        }

        McpRunOutputCollector collector;
        try {
            collector = new McpRunOutputCollector(name);
        }
        catch (IOException e) {
            started.completeExceptionally(e);
            return;
        }

        CompletableFuture<Integer> exitCode = new CompletableFuture<>();
        processHandler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(ProcessEvent event, Key outputType) {
                if (outputType != ProcessOutputTypes.SYSTEM) {
                    collector.append(event.getText());
                }
            }

            @Override
            public void processTerminated(ProcessEvent event) {
                collector.close();
                exitCode.complete(event.getExitCode());
            }
        });
        if (processHandler.isProcessTerminated()) {
            Integer code = processHandler.getExitCode();
            collector.close();
            exitCode.complete(code == null ? -1 : code);
        }
        started.complete(new McpRunExecution(collector, exitCode));
    }

    private static McpToolCallResult describe(McpRunExecution execution, @Nullable Integer exitCode) {
        McpRunOutputCollector collector = execution.collector();
        StringBuilder builder = new StringBuilder();
        if (exitCode != null) {
            builder.append("exit code: ").append(exitCode).append('\n');
        }
        else {
            builder.append("the process is still running\n");
        }
        builder.append("output:\n").append(collector.getPreview());
        if (!builder.toString().endsWith("\n")) {
            builder.append('\n');
        }
        if (exitCode == null || collector.isTruncated()) {
            builder.append(collector.isTruncated() ? "output truncated, " : "").append("full output: ").append(collector.getOutputPath()).append('\n');
        }
        else {
            collector.deleteOutputFile();
        }
        return McpToolCallResult.text(builder.toString());
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        return String.valueOf(cause.getMessage());
    }

    private McpRunConfigurationLauncher() {
    }
}
