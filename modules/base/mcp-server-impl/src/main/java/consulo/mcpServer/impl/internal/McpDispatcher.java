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
import consulo.mcpServer.impl.internal.activity.McpToolApprovalService;
import consulo.mcpServer.impl.internal.activity.McpToolCallLog;
import consulo.mcpServer.impl.internal.activity.McpToolCallRecord;
import consulo.mcpServer.impl.internal.activity.McpToolCallStatus;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import consulo.project.Project;
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
    private static final String UNKNOWN_CLIENT = "unknown client";
    private static final String DENIED_MESSAGE = "The user denied this tool call.";

    private final Application myApplication;
    private final McpToolRegistry myToolRegistry;
    private final McpSessionManager mySessionManager;
    private final McpProjectResolver myProjectResolver;
    private final McpServerSettings mySettings;
    private final McpToolCallLog myCallLog;
    private final McpToolApprovalService myApprovalService;

    @Inject
    public McpDispatcher(Application application,
                         McpToolRegistry toolRegistry,
                         McpSessionManager sessionManager,
                         McpProjectResolver projectResolver,
                         McpServerSettings settings,
                         McpToolCallLog callLog,
                         McpToolApprovalService approvalService) {
        myApplication = application;
        myToolRegistry = toolRegistry;
        mySessionManager = sessionManager;
        myProjectResolver = projectResolver;
        mySettings = settings;
        myCallLog = callLog;
        myApprovalService = approvalService;
    }

    public CompletableFuture<@Nullable JsonRpcResponse> dispatch(JsonRpcRequest request,
                                                                 @Nullable McpSession session,
                                                                 @Nullable String callProjectPath) {
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
                case McpProtocol.METHOD_TOOLS_CALL -> callTool(request, session, callProjectPath);
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

    public McpSession openSession(JsonRpcRequest request, McpPrincipal principal, @Nullable String projectPath) {
        InitializeParams params = decode(request.params, InitializeParams.class);
        ImplementationInfo clientInfo = params == null ? null : params.clientInfo;
        String clientName = clientInfo == null || clientInfo.name == null ? "unknown" : clientInfo.name;
        String clientVersion = clientInfo == null || clientInfo.version == null ? "" : clientInfo.version;
        return mySessionManager.create(negotiate(request), principal, clientName, clientVersion, projectPath);
    }

    private static String negotiate(JsonRpcRequest request) {
        InitializeParams params = decode(request.params, InitializeParams.class);
        String requested = params == null ? null : params.protocolVersion;
        return requested != null && McpProtocol.SUPPORTED_VERSIONS.contains(requested) ? requested : McpProtocol.LATEST_VERSION;
    }

    private InitializeResult initialize(JsonRpcRequest request) {
        String negotiated = negotiate(request);

        ServerCapabilities capabilities = new ServerCapabilities();
        capabilities.tools = new ServerCapabilities.Tools(false);

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

    private CompletableFuture<@Nullable JsonRpcResponse> callTool(JsonRpcRequest request,
                                                                  @Nullable McpSession session,
                                                                  @Nullable String callProjectPath) {
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
                JsonElement projectPathArgument = arguments.get(McpProjectResolver.PROJECT_PATH_ARGUMENT);
                String argumentPath = projectPathArgument != null && projectPathArgument.isJsonPrimitive()
                    ? projectPathArgument.getAsString()
                    : null;
                project = myProjectResolver.resolve(argumentPath, callProjectPath, session == null ? null : session.getProjectPath());
            }
            catch (McpToolException e) {
                return CompletableFuture.completedFuture(
                    JsonRpcResponse.success(request.id, toolFailure(e.getMessage())));
            }
        }

        McpToolCallContextImpl context = new McpToolCallContextImpl(registration.getDescriptor(), arguments, project);

        String clientName = session == null ? UNKNOWN_CLIENT : session.getPrincipal().displayName();
        String argumentsText = arguments.toString();
        Project callProject = project;
        McpToolCallRecord record = myCallLog.start(clientName, callProject == null ? null : callProject.getName(), name, argumentsText);

        CompletableFuture<Boolean> approval;
        if (myApprovalService.requiresApproval(registration.getDescriptor()) && !myApprovalService.isPreApproved(session, name)) {
            myCallLog.changeStatus(record, McpToolCallStatus.WAITING_FOR_APPROVAL);
            approval = myApprovalService.askApproval(session, callProject, clientName, name, argumentsText)
                .exceptionally(throwable -> {
                    LOG.error("MCP tool approval has failed", throwable);
                    return Boolean.FALSE;
                });
        }
        else {
            approval = CompletableFuture.completedFuture(Boolean.TRUE);
        }

        return approval.thenCompose(allowed -> {
            if (!Boolean.TRUE.equals(allowed)) {
                myCallLog.finish(record, McpToolCallStatus.DENIED, DENIED_MESSAGE);
                return CompletableFuture.completedFuture(JsonRpcResponse.success(request.id, toolFailure(DENIED_MESSAGE)));
            }
            myCallLog.changeStatus(record, McpToolCallStatus.RUNNING);
            return runTool(request, registration, context, record);
        });
    }

    private CompletableFuture<@Nullable JsonRpcResponse> runTool(JsonRpcRequest request,
                                                                 McpToolRegistration registration,
                                                                 McpToolCallContextImpl context,
                                                                 McpToolCallRecord record) {
        String name = registration.getDescriptor().getName();
        CompletableFuture<McpToolCallResult> call;
        try {
            call = registration.getHandler().call(context);
        }
        catch (Throwable e) {
            String failure = describe(name, e);
            myCallLog.finish(record, McpToolCallStatus.FAILED, failure);
            return CompletableFuture.completedFuture(JsonRpcResponse.success(request.id, toolFailure(failure)));
        }

        return call.handle((result, throwable) -> {
            if (throwable != null) {
                String failure = describe(name, throwable);
                myCallLog.finish(record, McpToolCallStatus.FAILED, failure);
                return JsonRpcResponse.success(request.id, toolFailure(failure));
            }
            if (result == null) {
                LOG.error("MCP tool '" + name + "' completed without a result");
                myCallLog.finish(record, McpToolCallStatus.FAILED, "Tool returned no result");
                return JsonRpcResponse.success(request.id, toolFailure("Tool returned no result"));
            }
            List<ContentBlock> content = new ArrayList<>();
            for (String text : result.getContent()) {
                content.add(ContentBlock.text(text));
            }
            myCallLog.finish(record, result.isError() ? McpToolCallStatus.FAILED : McpToolCallStatus.SUCCEEDED, String.join("\n", result.getContent()));
            return JsonRpcResponse.success(request.id, new ToolsCallResult(content, result.isError()));
        });
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
