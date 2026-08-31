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
import consulo.mcpServer.impl.internal.McpDispatcher;
import consulo.mcpServer.impl.internal.McpSession;
import consulo.mcpServer.impl.internal.McpSessionManager;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
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

    private final McpDispatcher myDispatcher;
    private final McpSessionManager mySessionManager;
    private final McpServerSettings mySettings;

    @Inject
    public McpHttpRequestHandler(McpDispatcher dispatcher, McpSessionManager sessionManager, McpServerSettings settings) {
        myDispatcher = dispatcher;
        mySessionManager = sessionManager;
        mySettings = settings;
    }

    @Override
    public boolean isSupported(HttpRequest request) {
        // the built-in server shares one handler chain across every bound port, so MCP has to claim only its own
        if (!mySettings.isEnabled() || request.localPort() != mySettings.getPort() || !PATH.equals(request.path())) {
            return false;
        }
        HttpMethod method = request.method();
        return method == HttpMethod.POST || method == HttpMethod.DELETE;
    }

    @Override
    public HttpResponse process(HttpRequest request) throws IOException {
        String sessionId = request.getHeaderValue(McpProtocol.SESSION_ID_HEADER);

        if (request.method() == HttpMethod.DELETE) {
            mySessionManager.terminate(sessionId);
            return HttpResponse.create(HttpURLConnection.HTTP_NO_CONTENT, null, null);
        }

        JsonRpcRequest rpcRequest;
        try {
            rpcRequest = JsonService.getInstance().fromJson(request.getContentAsString(StandardCharsets.UTF_8), JsonRpcRequest.class);
        }
        catch (Exception e) {
            return jsonResponse(JsonRpcResponse.error(null, JsonRpcErrorCodes.PARSE_ERROR, "Malformed JSON: " + e.getMessage()));
        }

        if (rpcRequest == null) {
            return jsonResponse(JsonRpcResponse.error(null, JsonRpcErrorCodes.INVALID_REQUEST, "Empty request"));
        }

        McpSession session = mySessionManager.find(sessionId);
        String newSessionId = null;
        if (McpProtocol.METHOD_INITIALIZE.equals(rpcRequest.method)) {
            session = myDispatcher.openSession(rpcRequest);
            newSessionId = session.getId();
        }

        CompletableFuture<@Nullable JsonRpcResponse> result = myDispatcher.dispatch(rpcRequest, session);

        boolean sse = wantsEventStream(request);
        HttpResponse response = HttpResponse.streaming(sse ? SSE_CONTENT_TYPE : JSON_CONTENT_TYPE,
                                                       stream -> writeWhenReady(stream, result, sse));
        if (newSessionId != null) {
            response = response.withHeader(McpProtocol.SESSION_ID_HEADER, newSessionId);
        }
        return response;
    }

    private static void writeWhenReady(HttpResponseStream stream, CompletableFuture<@Nullable JsonRpcResponse> result, boolean sse) {
        result.whenComplete((response, throwable) -> {
            try {
                if (throwable != null) {
                    LOG.error("MCP request has failed", throwable);
                    response = JsonRpcResponse.error(null, JsonRpcErrorCodes.INTERNAL_ERROR, String.valueOf(throwable.getMessage()));
                }
                if (response != null) {
                    String json = JsonService.getInstance().toJson(response);
                    stream.write(sse ? toEvent(json) : json);
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
        return HttpResponse.ok(JSON_CONTENT_TYPE, JsonService.getInstance().toJson(response).getBytes(StandardCharsets.UTF_8));
    }
}
