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
package consulo.ide.impl.internal.mcp;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.container.boot.ContainerPathManager;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.platform.Platform;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
@Singleton
public class DiagnosticsToolset implements McpToolset {
    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("get_ide_diagnostics")
            .description("Reports the IDE name and version, the operating system and JVM, and the home, config, system and log directory "
                + "paths. Use it to diagnose environment issues or to locate the log files.")
            .readOnly()
            .idempotent()
            .handler(DiagnosticsToolset::getIdeDiagnostics);
    }

    /**
     * Reads only static application state, so it needs neither a project nor a read action - which is
     * also why it is the one tool that works with no project open.
     */
    private static CompletableFuture<McpToolCallResult> getIdeDiagnostics(McpToolCallContext context) {
        Platform platform = Platform.current();
        ContainerPathManager pathManager = ContainerPathManager.get();

            StringBuilder builder = new StringBuilder();
        builder.append("name: ").append(Application.get().getName().get()).append('\n');
        builder.append("version: ").append(ApplicationInfo.getInstance().getFullVersion()).append('\n');
        builder.append("os: ").append(platform.os().name()).append(' ').append(platform.os().version()).append('\n');
        builder.append("arch: ").append(platform.jvm().arch()).append('\n');
        builder.append("jvm: ").append(platform.jvm().version()).append('\n');
        builder.append("home: ").append(pathManager.getHomePath()).append('\n');
        builder.append("config: ").append(pathManager.getConfigPath()).append('\n');
        builder.append("system: ").append(pathManager.getSystemPath()).append('\n');
        builder.append("logs: ").append(pathManager.getLogPath()).append('\n');
        return CompletableFuture.completedFuture(McpToolCallResult.text(builder.toString()));
    }
}
