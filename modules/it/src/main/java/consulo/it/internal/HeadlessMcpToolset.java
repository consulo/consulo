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
package consulo.it.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;

/**
 * Toolset used by the MCP integration tests. It covers the argument shapes the dispatcher has to
 * handle — required, defaulted, and failing — without depending on any IDE subsystem.
 */
@ExtensionImpl
@Singleton
public class HeadlessMcpToolset implements McpToolset {
    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("it_echo")
            .description("Echoes the given text back, repeated the given number of times.")
            .readOnly()
            .idempotent()
            .param(McpParameter.string("text", "Text to echo."))
            .param(McpParameter.integer("times", "How many times to repeat.").defaultValue(1))
            .handler(HeadlessMcpToolset::echo);

        registrar.tool("it_fail")
            .description("Always fails, to exercise error reporting.")
            .readOnly()
            .handler(context -> CompletableFuture.failedFuture(new McpToolException("expected failure")));

        registrar.tool("it_read_action")
            .description("Reports the thread it ran on, to prove the read-action path works.")
            .readOnly()
            .requiresProject()
            .handler(context -> McpToolActions.readAction(context.getProject(), () -> McpToolCallResult.text("read")));

        registrar.tool("it_write_action")
            .description("Reports the thread it ran on, to prove the write-action path works.")
            .requiresProject()
            .handler(context -> McpToolActions.writeAction(context.getProject(), () -> McpToolCallResult.text("write")));

        registrar.tool("it_ui_action")
            .description("Runs on the UI thread, to prove the UIAction path has a UIAccess bound.")
            .requiresProject()
            .handler(context -> McpToolActions.uiAction(context.getProject(), () -> McpToolCallResult.text("ui")));

        registrar.tool("it_project_name")
            .description("Returns the name of the resolved project.")
            .readOnly()
            .requiresProject()
            .handler(context -> CompletableFuture.completedFuture(McpToolCallResult.text(context.getProject().getName())));
    }

    private static CompletableFuture<McpToolCallResult> echo(McpToolCallContext context) {
        String text = context.getString("text");
        int times = context.getInt("times");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(text);
        }
        return CompletableFuture.completedFuture(McpToolCallResult.text(builder.toString()));
    }
}
