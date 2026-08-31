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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.internal.ApplicationInfo;
import consulo.application.json.JsonService;
import consulo.logging.Logger;
import consulo.mcp.protocol.JsonRpcErrorCodes;
import consulo.mcp.protocol.JsonRpcRequest;
import consulo.mcp.protocol.JsonRpcResponse;
import consulo.mcp.protocol.model.*;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolException;
import consulo.mcp.tool.McpToolSchema;
import consulo.project.Project;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import consulo.project.ProjectManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Turns a decoded JSON-RPC message into a response. Notifications resolve to {@code null}, which the
 * transport renders as an empty accepted response.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpDispatcher {
    private static final Logger LOG = Logger.getInstance(McpDispatcher.class);

    private final Application myApplication;
    private final McpToolRegistry myToolRegistry;
    private final McpSessionManager mySessionManager;
    private final ProjectManager myProjectManager;
    private final McpServerSettings mySettings;

    @Inject
    public McpDispatcher(Application application,
                         McpToolRegistry toolRegistry,
                         McpSessionManager sessionManager,
                         ProjectManager projectManager,
                         McpServerSettings settings) {
        myApplication = application;
        myToolRegistry = toolRegistry;
        mySessionManager = sessionManager;
        myProjectManager = projectManager;
        mySettings = settings;
    }

    public CompletableFuture<@Nullable JsonRpcResponse> dispatch(JsonRpcRequest request, @Nullable McpSession session) {
        String method = request.method;
        if (method == null) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error(request.id, JsonRpcErrorCodes.INVALID_REQUEST, "Missing 'method'"));
        }

        if (request.isNotification()) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            return switch (method) {
                case McpProtocol.METHOD_INITIALIZE -> completed(request, initialize(request));
                case McpProtocol.METHOD_PING -> completed(request, new JsonObject());
                case McpProtocol.METHOD_TOOLS_LIST -> completed(request, listTools());
                case McpProtocol.METHOD_TOOLS_CALL -> callTool(request, session);
                default -> CompletableFuture.completedFuture(
                    JsonRpcResponse.error(request.id, JsonRpcErrorCodes.METHOD_NOT_FOUND, "Unknown method: " + method));
            };
        }
        catch (McpToolException e) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error(request.id, JsonRpcErrorCodes.INVALID_PARAMS, String.valueOf(e.getMessage())));
        }
        catch (Throwable e) {
            LOG.error("MCP method '" + method + "' has failed", e);
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error(request.id, JsonRpcErrorCodes.INTERNAL_ERROR, String.valueOf(e.getMessage())));
        }
    }

    /**
     * Called before dispatch, because the session id has to be on the response headers that are
     * flushed ahead of the asynchronously produced body.
     */
    public McpSession openSession(JsonRpcRequest request) {
        return mySessionManager.create(negotiate(request));
    }

    private static String negotiate(JsonRpcRequest request) {
        InitializeParams params = decode(request.params, InitializeParams.class);
        String requested = params == null ? null : params.protocolVersion;
        return requested != null && McpProtocol.SUPPORTED_VERSIONS.contains(requested) ? requested : McpProtocol.LATEST_VERSION;
    }

    private InitializeResult initialize(JsonRpcRequest request) {
        String negotiated = negotiate(request);

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.tools = new ServerCapabilities.Tools(true);

        ImplementationInfo serverInfo = new ImplementationInfo(myApplication.getName().get(),
                                                              ApplicationInfo.getInstance().getFullVersion());
        return new InitializeResult(negotiated, capabilities, serverInfo);
    }

    private ToolsListResult listTools() {
        List<ToolDefinition> definitions = new ArrayList<>();
        for (McpToolRegistration registration : myToolRegistry.getTools()) {
            if (mySettings.isToolEnabled(registration.getDescriptor().getName())) {
                definitions.add(toDefinition(registration));
            }
        }
        return new ToolsListResult(definitions);
    }

    private static ToolDefinition toDefinition(McpToolRegistration registration) {
        ToolDefinition definition = new ToolDefinition(registration.getDescriptor().getName(),
                                                       McpToolSchema.of(registration.getDescriptor().getParameters()));
        definition.description = registration.getDescriptor().getDescription();

        String title = registration.getDescriptor().getTitle().get();
        if (!title.isEmpty()) {
            definition.title = title;
        }

        ToolAnnotations annotations = new ToolAnnotations();
        annotations.readOnlyHint = registration.getDescriptor().getReadOnlyHint();
        annotations.destructiveHint = registration.getDescriptor().getDestructiveHint();
        annotations.idempotentHint = registration.getDescriptor().getIdempotentHint();
        annotations.openWorldHint = registration.getDescriptor().getOpenWorldHint();
        if (!annotations.isEmpty()) {
            definition.annotations = annotations;
        }
        return definition;
    }

    private CompletableFuture<@Nullable JsonRpcResponse> callTool(JsonRpcRequest request, @Nullable McpSession session) {
        ToolsCallParams params = decode(request.params, ToolsCallParams.class);
        if (params == null || params.name == null) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error(request.id, JsonRpcErrorCodes.INVALID_PARAMS, "Missing tool name"));
        }
        String name = params.name;

        McpToolRegistration registration = myToolRegistry.findTool(name);
        // a disabled tool is not advertised, so an agent asking for it is asking for something that does not exist
        if (registration == null || !mySettings.isToolEnabled(name)) {
            return CompletableFuture.completedFuture(
                JsonRpcResponse.error(request.id, JsonRpcErrorCodes.INVALID_PARAMS, "Unknown tool: " + name));
        }

        JsonObject arguments = params.arguments == null ? new JsonObject() : params.arguments;

        Project project = null;
        if (registration.isRequiresProject()) {
            try {
                project = resolveProject();
            }
            catch (McpToolException e) {
                return CompletableFuture.completedFuture(
                    JsonRpcResponse.success(request.id, toolFailure(e.getMessage())));
            }
        }

        McpToolCallContextImpl context = new McpToolCallContextImpl(registration.getDescriptor(), arguments, project);

        CompletableFuture<McpToolCallResult> call;
        try {
            call = registration.getHandler().call(context);
        }
        catch (Throwable e) {
            return CompletableFuture.completedFuture(JsonRpcResponse.success(request.id, toolFailure(describe(name, e))));
        }

        return call.handle((result, throwable) -> {
            if (throwable != null) {
                return JsonRpcResponse.success(request.id, toolFailure(describe(name, throwable)));
            }
            List<ContentBlock> content = new ArrayList<>();
            for (String text : result.getContent()) {
                content.add(ContentBlock.text(text));
            }
            return JsonRpcResponse.success(request.id, new ToolsCallResult(content, result.isError()));
        });
    }

    private Project resolveProject() {
        Project[] projects = myProjectManager.getOpenProjects();
        if (projects.length == 1) {
            return projects[0];
        }
        if (projects.length == 0) {
            throw new McpToolException("No project is open in the IDE.");
        }

        StringBuilder message = new StringBuilder("Several projects are open, cannot pick one:");
        for (Project project : projects) {
            message.append("\n - ").append(project.getName());
        }
        throw new McpToolException(message.toString());
    }

    private static String describe(String toolName, Throwable throwable) {
        Throwable cause = throwable instanceof java.util.concurrent.CompletionException && throwable.getCause() != null
            ? throwable.getCause()
            : throwable;
        if (cause instanceof McpToolException) {
            return String.valueOf(cause.getMessage());
        }
        LOG.warn("MCP tool '" + toolName + "' has failed", cause);
        return "MCP tool call has failed: " + cause;
    }

    private static ToolsCallResult toolFailure(@Nullable String message) {
        return new ToolsCallResult(List.of(ContentBlock.text(String.valueOf(message))), true);
    }

    private static CompletableFuture<@Nullable JsonRpcResponse> completed(JsonRpcRequest request, Object result) {
        return CompletableFuture.completedFuture(JsonRpcResponse.success(request.id, result));
    }

    private static <T> @Nullable T decode(@Nullable JsonElement params, Class<T> clazz) {
        if (params == null || !params.isJsonObject()) {
            return null;
        }
        return JsonService.getInstance().fromJson(params.toString(), clazz);
    }
}
