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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.json.JsonService;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.http.HttpMethod;
import consulo.logging.Logger;
import consulo.mcpServer.impl.internal.oauth.McpOAuthException;
import consulo.mcpServer.impl.internal.oauth.McpOAuthService;
import consulo.mcpServer.impl.internal.oauth.McpOAuthTokens;
import consulo.mcpServer.impl.internal.setting.McpOAuthClientState;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@ExtensionImpl
public class McpOAuthRequestHandler extends HttpRequestHandler {
    private static final Logger LOG = Logger.getInstance(McpOAuthRequestHandler.class);

    public static final String PROTECTED_RESOURCE_PATH = "/.well-known/oauth-protected-resource";
    private static final String PROTECTED_RESOURCE_MCP_PATH = PROTECTED_RESOURCE_PATH + "/mcp";
    private static final String AUTHORIZATION_SERVER_PATH = "/.well-known/oauth-authorization-server";
    private static final String OPENID_CONFIGURATION_PATH = "/.well-known/openid-configuration";
    private static final String REGISTER_PATH = "/oauth/register";
    private static final String AUTHORIZE_PATH = "/oauth/authorize";
    private static final String TOKEN_PATH = "/oauth/token";

    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String TEXT_CONTENT_TYPE = "text/plain; charset=utf-8";

    private final McpServerSettings mySettings;
    private final McpOAuthService myOAuthService;

    @Inject
    public McpOAuthRequestHandler(McpServerSettings settings, McpOAuthService oAuthService) {
        mySettings = settings;
        myOAuthService = oAuthService;
    }

    @Override
    public boolean isSupported(HttpRequest request) {
        if (!mySettings.isEnabled() || request.localPort() != mySettings.getPort()) {
            return false;
        }
        HttpMethod method = request.method();
        return switch (request.path()) {
            case PROTECTED_RESOURCE_PATH, PROTECTED_RESOURCE_MCP_PATH, AUTHORIZATION_SERVER_PATH, OPENID_CONFIGURATION_PATH, AUTHORIZE_PATH ->
                method == HttpMethod.GET;
            case REGISTER_PATH, TOKEN_PATH -> method == HttpMethod.POST;
            default -> false;
        };
    }

    @Override
    public HttpResponse process(HttpRequest request) throws IOException {
        String baseUrl = baseUrl(request, mySettings.getPort());
        return switch (request.path()) {
            case PROTECTED_RESOURCE_PATH, PROTECTED_RESOURCE_MCP_PATH -> json(HttpURLConnection.HTTP_OK, protectedResource(baseUrl));
            case AUTHORIZATION_SERVER_PATH, OPENID_CONFIGURATION_PATH -> json(HttpURLConnection.HTTP_OK, authorizationServer(baseUrl));
            case REGISTER_PATH -> register(request.getContentAsString(StandardCharsets.UTF_8));
            case AUTHORIZE_PATH -> authorize(request);
            case TOKEN_PATH -> token(parseForm(request.getContentAsString(StandardCharsets.UTF_8)));
            default -> HttpResponse.notFound();
        };
    }

    public static String baseUrl(HttpRequest request, int port) {
        String host = request.getHeaderValue("Host");
        return "http://" + (host == null || host.isBlank() ? "127.0.0.1:" + port : host);
    }

    public static String resourceMetadataUrl(HttpRequest request, int port) {
        return baseUrl(request, port) + PROTECTED_RESOURCE_PATH;
    }

    private static JsonObject protectedResource(String baseUrl) {
        JsonObject result = new JsonObject();
        result.addProperty("resource", baseUrl + "/mcp");
        result.add("authorization_servers", array(List.of(baseUrl)));
        result.add("bearer_methods_supported", array(List.of("header")));
        return result;
    }

    private static JsonObject authorizationServer(String baseUrl) {
        JsonObject result = new JsonObject();
        result.addProperty("issuer", baseUrl);
        result.addProperty("authorization_endpoint", baseUrl + AUTHORIZE_PATH);
        result.addProperty("token_endpoint", baseUrl + TOKEN_PATH);
        result.addProperty("registration_endpoint", baseUrl + REGISTER_PATH);
        result.add("response_types_supported", array(List.of("code")));
        result.add("grant_types_supported", array(List.of("authorization_code", "refresh_token")));
        result.add("code_challenge_methods_supported", array(List.of("S256")));
        result.add("token_endpoint_auth_methods_supported", array(List.of("none")));
        return result;
    }

    private HttpResponse register(String body) {
        JsonObject request;
        try {
            JsonElement element = JsonParser.parseString(body);
            if (!element.isJsonObject()) {
                return error(new McpOAuthException(McpOAuthService.ERROR_INVALID_REQUEST, "Registration request must be a JSON object."));
            }
            request = element.getAsJsonObject();
        }
        catch (RuntimeException e) {
            return error(new McpOAuthException(McpOAuthService.ERROR_INVALID_REQUEST, "Malformed JSON."));
        }

        List<String> redirectUris = new ArrayList<>();
        JsonElement redirectElement = request.get("redirect_uris");
        if (redirectElement != null && redirectElement.isJsonArray()) {
            for (JsonElement element : redirectElement.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    redirectUris.add(element.getAsString());
                }
            }
        }
        JsonElement nameElement = request.get("client_name");
        String clientName = nameElement != null && nameElement.isJsonPrimitive() ? nameElement.getAsString() : "";

        try {
            McpOAuthClientState client = myOAuthService.register(clientName, redirectUris);
            JsonObject result = new JsonObject();
            result.addProperty("client_id", client.CLIENT_ID);
            result.addProperty("client_name", client.CLIENT_NAME);
            result.add("redirect_uris", array(client.redirectUris()));
            result.add("grant_types", array(List.of("authorization_code", "refresh_token")));
            result.add("response_types", array(List.of("code")));
            result.addProperty("token_endpoint_auth_method", "none");
            result.addProperty("client_id_issued_at", System.currentTimeMillis() / 1000);
            return json(HttpURLConnection.HTTP_CREATED, result);
        }
        catch (McpOAuthException e) {
            return error(e);
        }
    }

    private HttpResponse authorize(HttpRequest request) {
        String clientId = request.getParameterValue("client_id");
        String redirectUri = request.getParameterValue("redirect_uri");
        String state = request.getParameterValue("state");
        if (clientId == null || redirectUri == null || !"code".equals(request.getParameterValue("response_type"))) {
            return text(HttpURLConnection.HTTP_BAD_REQUEST, "client_id, redirect_uri and response_type=code are required.");
        }

        String codeChallenge = notNull(request.getParameterValue("code_challenge"));
        String codeChallengeMethod = notNull(request.getParameterValue("code_challenge_method"));

        CompletableFuture<HttpResponse> response = myOAuthService.authorize(clientId, redirectUri, codeChallenge, codeChallengeMethod)
            .thenApply(code -> redirect(redirectUri, Map.of("code", code), state))
            .exceptionally(throwable -> {
                Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
                if (cause instanceof McpOAuthException oAuthException) {
                    if (McpOAuthService.ERROR_ACCESS_DENIED.equals(oAuthException.getError())) {
                        return redirect(redirectUri, Map.of("error", oAuthException.getError(), "error_description", String.valueOf(cause.getMessage())), state);
                    }
                    return text(HttpURLConnection.HTTP_BAD_REQUEST, oAuthException.getError() + ": " + oAuthException.getMessage());
                }
                if (cause instanceof TimeoutException) {
                    return redirect(redirectUri, Map.of("error", McpOAuthService.ERROR_ACCESS_DENIED, "error_description", "No answer from the user."), state);
                }
                LOG.error("MCP authorization has failed", cause);
                return text(HttpURLConnection.HTTP_INTERNAL_ERROR, "Authorization has failed.");
            });
        return HttpResponse.deferred(response);
    }

    private HttpResponse token(Map<String, String> form) {
        try {
            String grantType = notNull(form.get("grant_type"));
            McpOAuthTokens tokens = switch (grantType) {
                case "authorization_code" -> myOAuthService.exchangeCode(notNull(form.get("client_id")),
                                                                         notNull(form.get("code")),
                                                                         notNull(form.get("redirect_uri")),
                                                                         notNull(form.get("code_verifier")));
                case "refresh_token" -> myOAuthService.refresh(form.get("client_id"), notNull(form.get("refresh_token")));
                default -> throw new McpOAuthException("unsupported_grant_type", "Unsupported grant type: " + grantType);
            };

            JsonObject result = new JsonObject();
            result.addProperty("access_token", tokens.accessToken());
            result.addProperty("token_type", "Bearer");
            result.addProperty("expires_in", tokens.expiresInSeconds());
            result.addProperty("refresh_token", tokens.refreshToken());
            return json(HttpURLConnection.HTTP_OK, result).withHeader("Cache-Control", "no-store");
        }
        catch (McpOAuthException e) {
            return error(e);
        }
    }

    private static HttpResponse redirect(String redirectUri, Map<String, String> parameters, @Nullable String state) {
        StringBuilder location = new StringBuilder(redirectUri).append(redirectUri.contains("?") ? '&' : '?');
        Map<String, String> all = new HashMap<>(parameters);
        if (state != null) {
            all.put("state", state);
        }
        boolean first = true;
        for (Map.Entry<String, String> entry : all.entrySet()) {
            if (!first) {
                location.append('&');
            }
            first = false;
            location.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return HttpResponse.create(HttpURLConnection.HTTP_MOVED_TEMP, null, null).withHeader("Location", location.toString());
    }

    private static HttpResponse error(McpOAuthException exception) {
        JsonObject result = new JsonObject();
        result.addProperty("error", exception.getError());
        result.addProperty("error_description", String.valueOf(exception.getMessage()));
        int code = McpOAuthService.ERROR_INVALID_CLIENT.equals(exception.getError()) ? HttpURLConnection.HTTP_UNAUTHORIZED : HttpURLConnection.HTTP_BAD_REQUEST;
        return json(code, result).withHeader("Cache-Control", "no-store");
    }

    private static HttpResponse json(int code, JsonObject body) {
        return HttpResponse.create(code, JSON_CONTENT_TYPE, JsonService.getInstance().toJson(body).getBytes(StandardCharsets.UTF_8));
    }

    private static HttpResponse text(int code, String body) {
        return HttpResponse.create(code, TEXT_CONTENT_TYPE, body.getBytes(StandardCharsets.UTF_8));
    }

    private static JsonArray array(List<String> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> result = new HashMap<>();
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            result.put(URLDecoder.decode(key, StandardCharsets.UTF_8), URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return result;
    }

    private static String notNull(@Nullable String value) {
        return value == null ? "" : value;
    }
}
