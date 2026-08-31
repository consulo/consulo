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
package consulo.mcpServer.impl.internal.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import consulo.ai.AITool;
import consulo.ai.AIToolProvider;
import consulo.ai.AIToolResult;
import consulo.annotation.component.ExtensionImpl;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolDescriptor;
import consulo.mcp.tool.McpToolSchema;
import consulo.mcpServer.impl.internal.McpToolCallContextImpl;
import consulo.mcpServer.impl.internal.McpToolRegistration;
import consulo.mcpServer.impl.internal.McpToolRegistry;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Exposes the IDE's own MCP toolsets to in-IDE AI providers, so the same tool implementations serve
 * both an external agent over the wire and the built-in chat.
 *
 * @author VISTALL
 * @since 2026-08-04
 */
@ExtensionImpl
@Singleton
public class LocalToolsetAIToolProvider implements AIToolProvider {
    private record McpBackedTool(McpToolRegistration registration) implements AITool {
        @Override
        public String getName() {
            return registration.getDescriptor().getName();
        }

        @Override
        public String getDescription() {
            return registration.getDescriptor().getDescription();
        }

        @Override
        public String getInputSchema() {
            return McpToolSchema.of(registration.getDescriptor().getParameters()).toString();
        }

        @Override
        public CompletableFuture<AIToolResult> call(Project project, String argumentsJson) {
            McpToolDescriptor descriptor = registration.getDescriptor();

            JsonObject arguments;
            try {
                arguments = JsonParser.parseString(argumentsJson).getAsJsonObject();
            }
            catch (RuntimeException e) {
                return CompletableFuture.completedFuture(AIToolResult.error("Arguments are not a JSON object: " + e.getMessage()));
            }

            // the chat always has a project, so project-scoped tools can be served unconditionally
            McpToolCallContextImpl context = new McpToolCallContextImpl(descriptor, arguments, project);

            try {
                return registration.getHandler().call(context).handle(LocalToolsetAIToolProvider::toResult);
            }
            catch (Throwable e) {
                return CompletableFuture.completedFuture(AIToolResult.error(String.valueOf(e.getMessage())));
            }
        }
    }

    private final McpToolRegistry myToolRegistry;

    @Inject
    public LocalToolsetAIToolProvider(McpToolRegistry toolRegistry) {
        myToolRegistry = toolRegistry;
    }

    @Override
    public List<AITool> getTools(Project project) {
        List<AITool> tools = new ArrayList<>();
        for (McpToolRegistration registration : myToolRegistry.getTools()) {
            tools.add(new McpBackedTool(registration));
        }
        return tools;
    }

    private static AIToolResult toResult(McpToolCallResult result, Throwable throwable) {
        if (throwable != null) {
            return AIToolResult.error(String.valueOf(throwable.getMessage()));
        }
        return new AIToolResult(String.join("\n", result.getContent()), result.isError());
    }
}
