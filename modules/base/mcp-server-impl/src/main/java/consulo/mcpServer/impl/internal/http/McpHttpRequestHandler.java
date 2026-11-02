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
package consulo.mcpServer.impl.internal.http;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.json.JsonService;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.builtinWebServer.http.HttpResponseStream;
import consulo.http.HttpMethod;
import consulo.logging.Logger;
import consulo.mcp.protocol.JsonRpcErrorCodes;
import consulo.mcp.protocol.JsonRpcRequest;
import consulo.mcp.protocol.JsonRpcResponse;
import consulo.mcp.protocol.model.McpProtocol;
import consulo.mcpServer.impl.internal.McpAuthTokenManager;
import consulo.mcpServer.impl.internal.McpAuthenticator;
import consulo.mcpServer.impl.internal.McpDispatcher;
import consulo.mcpServer.impl.internal.McpPrincipal;
import consulo.mcpServer.impl.internal.McpProjectResolver;
import consulo.mcpServer.impl.internal.McpSession;
import consulo.mcpServer.impl.internal.McpSessionManager;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Streamable HTTP transport. A POST answers with either a single JSON object or a one-event SSE
 * stream, depending on what the client accepts.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
public class McpHttpRequestHandler extends HttpRequestHandler {
    private static final Logger LOG = Logger.getInstance(McpHttpRequestHandler.class);

    private static final String PATH = "/mcp";
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String SSE_CONTENT_TYPE = "text/event-stream; charset=utf-8";
    private static final String ORIGIN_HEADER = "Origin";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final int HTTP_UNSUPPORTED_MEDIA_TYPE = 415;
    private static final int HTTP_PAYLOAD_TOO_LARGE = 413;
    private static final int MAX_BODY_LENGTH = 4 * 1024 * 1024;

    private final McpDispatcher myDispatcher;
    private final McpSessionManager mySessionManager;
    private final McpServerSettings mySettings;
    private final McpAuthenticator myAuthenticator;

    @Inject
    public McpHttpRequestHandler(McpDispatcher dispatcher,
                                 McpSessionManager sessionManager,
                                 McpServerSettings settings,
                                 McpAuthenticator authenticator) {
        myDispatcher = dispatcher;
        mySessionManager = sessionManager;
        mySettings = settings;
        myAuthenticator = authenticator;
    }

    @Override
    public boolean isSupported(HttpRequest request) {
        // the built-in server shares one handler chain across every bound port, so MCP has to claim only its own
        if (!mySettings.isEnabled() || request.localPort() != mySettings.getPort() || !PATH.equals(request.path())) {
            return false;
        }
        HttpMethod method = request.method();
        return method == HttpMethod.POST || method == HttpMethod.DELETE || method == HttpMethod.GET;
    }

    @Override
    public HttpResponse process(HttpRequest request) throws IOException {
        if (request.getHeaderValue(ORIGIN_HEADER) != null) {
            return HttpResponse.create(HttpURLConnection.HTTP_FORBIDDEN, null, null);
        }

        HttpMethod method = request.method();
        if (method == HttpMethod.GET) {
            return HttpResponse.create(HttpURLConnection.HTTP_BAD_METHOD, null, null).withHeader("Allow", "POST, DELETE");
        }
        if (method == HttpMethod.POST && !isJson(request.getHeaderValue(CONTENT_TYPE_HEADER))) {
            return HttpResponse.create(HTTP_UNSUPPORTED_MEDIA_TYPE, null, null);
        }

        String body = method == HttpMethod.POST ? request.getContentAsString(StandardCharsets.UTF_8) : "";
        if (body.length() > MAX_BODY_LENGTH) {
            return HttpResponse.create(HTTP_PAYLOAD_TOO_LARGE, null, null);
        }
        String protocolVersion = request.getHeaderValue(McpProtocol.PROTOCOL_VERSION_HEADER);
        if (protocolVersion != null && !McpProtocol.SUPPORTED_VERSIONS.contains(protocolVersion)) {
            return text(HttpURLConnection.HTTP_BAD_REQUEST, "Unsupported " + McpProtocol.PROTOCOL_VERSION_HEADER + ": " + protocolVersion);
        }
        String bearer = McpAuthTokenManager.extractBearer(request.getHeaderValue(McpAuthTokenManager.AUTHORIZATION_HEADER));
        String resourceMetadata = McpOAuthRequestHandler.resourceMetadataUrl(request, mySettings.getPort());
        String sessionId = request.getHeaderValue(McpProtocol.SESSION_ID_HEADER);
        String projectPath = request.getHeaderValue(McpProjectResolver.PROJECT_PATH_HEADER);
        boolean sse = wantsEventStream(request);

        CompletableFuture<HttpResponse> response = myAuthenticator.authenticate(bearer)
            .thenApplyAsync(principal -> principal != null
                ? handle(principal, method, sessionId, projectPath, body, sse)
                : HttpResponse.create(HttpURLConnection.HTTP_UNAUTHORIZED, null, null)
                    .withHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + resourceMetadata + "\""),
                AppExecutorUtil.getAppExecutorService())
            .exceptionally(throwable -> {
                LOG.error("MCP request has failed", throwable);
                return HttpResponse.create(HttpURLConnection.HTTP_INTERNAL_ERROR, null, null);
            });
        return HttpResponse.deferred(response);
    }

    private HttpResponse handle(McpPrincipal principal,
                                HttpMethod method,
                                @Nullable String sessionId,
                                @Nullable String projectPath,
                                String body,
                                boolean sse) {
        if (method == HttpMethod.DELETE) {
            McpSession session = mySessionManager.find(sessionId);
            if (session == null) {
                return text(HttpURLConnection.HTTP_NOT_FOUND, "Unknown MCP session.");
            }
            if (!session.belongsTo(principal)) {
                return text(HttpURLConnection.HTTP_FORBIDDEN, "The MCP session belongs to another client.");
            }
            mySessionManager.terminate(session.getId());
            return HttpResponse.create(HttpURLConnection.HTTP_NO_CONTENT, null, null);
        }

        JsonElement payload;
        try {
            payload = JsonParser.parseString(body);
        }
        catch (RuntimeException e) {
            return jsonResponse(JsonRpcResponse.error(null, JsonRpcErrorCodes.PARSE_ERROR, "Malformed JSON: " + e.getMessage()));
        }

        List<JsonRpcRequest> requests = new ArrayList<>();
        boolean batch = payload.isJsonArray();
        for (JsonElement element : batch ? payload.getAsJsonArray() : List.of(payload)) {
            if (!element.isJsonObject()) {
                return jsonResponse(JsonRpcResponse.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "A JSON-RPC request must be an object"));
            }
            JsonRpcRequest rpcRequest = JsonService.getInstance().fromJson(element.toString(), JsonRpcRequest.class);
            if (rpcRequest == null) {
                return jsonResponse(JsonRpcResponse.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "Empty request"));
            }
            requests.add(rpcRequest);
        }
        if (requests.isEmpty()) {
            return jsonResponse(JsonRpcResponse.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "Empty batch"));
        }

        boolean initialize = requests.stream().anyMatch(request -> McpProtocol.METHOD_INITIALIZE.equals(request.method));
        if (initialize && requests.size() > 1) {
            return jsonResponse(JsonRpcResponse.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "initialize cannot be batched"));
        }

        McpSession session;
        String newSessionId = null;
        if (initialize) {
            session = myDispatcher.openSession(requests.get(0), principal, projectPath);
            newSessionId = session.getId();
        }
        else {
            if (sessionId == null) {
                return text(HttpURLConnection.HTTP_BAD_REQUEST, "Missing " + McpProtocol.SESSION_ID_HEADER + " header, call initialize first.");
            }
            session = mySessionManager.find(sessionId);
            if (session == null) {
                return text(HttpURLConnection.HTTP_NOT_FOUND, "Unknown or expired MCP session, call initialize again.");
            }
            if (!session.belongsTo(principal)) {
                return text(HttpURLConnection.HTTP_FORBIDDEN, "The MCP session belongs to another client.");
            }
        }

        if (requests.stream().allMatch(JsonRpcRequest::isNotification)) {
            return HttpResponse.create(HttpURLConnection.HTTP_ACCEPTED, null, null);
        }

        List<CompletableFuture<@Nullable JsonRpcResponse>> results = new ArrayList<>();
        for (JsonRpcRequest rpcRequest : requests) {
            JsonElement id = rpcRequest.id;
            results.add(myDispatcher.dispatch(rpcRequest, session, projectPath).exceptionally(throwable -> {
                LOG.error("MCP request has failed", throwable);
                return JsonRpcResponse.error(id, JsonRpcErrorCodes.INTERNAL_ERROR, String.valueOf(throwable.getMessage()));
            }));
        }
        CompletableFuture<String> json = CompletableFuture.allOf(results.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            JsonArray array = new JsonArray();
            for (CompletableFuture<@Nullable JsonRpcResponse> result : results) {
                JsonRpcResponse response = result.join();
                if (response != null) {
                    array.add(toTree(response));
                }
            }
            return batch ? array.toString() : array.isEmpty() ? "" : array.get(0).toString();
        });

        HttpResponse response = HttpResponse.streaming(sse ? SSE_CONTENT_TYPE : JSON_CONTENT_TYPE, stream -> writeWhenReady(stream, json, sse));
        if (newSessionId != null) {
            response = response.withHeader(McpProtocol.SESSION_ID_HEADER, newSessionId);
        }
        return response;
    }

    private static JsonObject toTree(JsonRpcResponse response) {
        JsonObject tree = JsonParser.parseString(JsonService.getInstance().toJson(response)).getAsJsonObject();
        if (!tree.has("id")) {
            tree.add("id", JsonNull.INSTANCE);
        }
        return tree;
    }

    private static HttpResponse text(int code, String message) {
        return HttpResponse.create(code, "text/plain; charset=utf-8", message.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isJson(@Nullable String contentType) {
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("application/json");
    }

    private static void writeWhenReady(HttpResponseStream stream, CompletableFuture<String> json, boolean sse) {
        json.whenComplete((text, throwable) -> {
            try {
                if (throwable != null) {
                    LOG.error("MCP request has failed", throwable);
                }
                else if (!text.isEmpty()) {
                    stream.write(sse ? toEvent(text) : text);
                }
            }
            finally {
                stream.close();
            }
        });
    }

    /**
     * Every line of the payload needs its own {@code data:} prefix, and the shared JSON service is
     * configured for pretty printing.
     */
    private static String toEvent(String json) {
        StringBuilder event = new StringBuilder("event: message\n");
        for (String line : json.split("\n", -1)) {
            event.append("data: ").append(line).append('\n');
        }
        return event.append('\n').toString();
    }

    private static boolean wantsEventStream(HttpRequest request) {
        String accept = request.getHeaderValue("Accept");
        return accept != null && accept.contains("text/event-stream") && !accept.contains("application/json");
    }

    private static HttpResponse jsonResponse(JsonRpcResponse response) {
        return HttpResponse.ok(JSON_CONTENT_TYPE, toTree(response).toString().getBytes(StandardCharsets.UTF_8));
    }
}
