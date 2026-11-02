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
package consulo.mcpServer.impl.internal.oauth;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.mcpServer.impl.internal.McpPrincipal;
import consulo.mcpServer.impl.internal.McpSessionManager;
import consulo.mcpServer.impl.internal.setting.McpOAuthClientState;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import consulo.mcpServer.localize.McpServerLocalize;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpOAuthService {
    public static final String ERROR_INVALID_REQUEST = "invalid_request";
    public static final String ERROR_INVALID_CLIENT = "invalid_client";
    public static final String ERROR_INVALID_GRANT = "invalid_grant";
    public static final String ERROR_ACCESS_DENIED = "access_denied";
    public static final String ERROR_INVALID_REDIRECT_URI = "invalid_redirect_uri";

    private static final long ACCESS_TOKEN_TTL_SECONDS = TimeUnit.HOURS.toSeconds(1);
    private static final long CODE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(2);
    private static final long CONSENT_TIMEOUT_MINUTES = 5;
    private static final int MAX_PENDING_CLIENTS = 64;

    private final Application myApplication;
    private final ProjectManager myProjectManager;
    private final McpServerSettings mySettings;
    private final McpSessionManager mySessionManager;
    private final SecureRandom myRandom = new SecureRandom();
    private final Object myLock = new Object();

    private final ConcurrentMap<String, McpOAuthClientState> myPendingClients = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, McpOAuthCode> myCodes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, McpOAuthAccessToken> myAccessTokens = new ConcurrentHashMap<>();

    @Inject
    public McpOAuthService(Application application,
                           ProjectManager projectManager,
                           McpServerSettings settings,
                           McpSessionManager sessionManager) {
        myApplication = application;
        myProjectManager = projectManager;
        mySettings = settings;
        mySessionManager = sessionManager;
    }

    public McpOAuthClientState register(String clientName, List<String> redirectUris) {
        if (redirectUris.isEmpty()) {
            throw new McpOAuthException(ERROR_INVALID_REDIRECT_URI, "At least one redirect URI is required.");
        }
        for (String redirectUri : redirectUris) {
            if (!isAllowedRedirectUri(redirectUri)) {
                throw new McpOAuthException(ERROR_INVALID_REDIRECT_URI, "Redirect URI must be a loopback http URI or a private scheme: " + redirectUri);
            }
        }
        if (myPendingClients.size() >= MAX_PENDING_CLIENTS) {
            myPendingClients.clear();
        }

        String name = clientName.isBlank() ? "Unnamed MCP client" : clientName.trim();
        McpOAuthClientState client = McpOAuthClientState.create(UUID.randomUUID().toString(), name, redirectUris);
        myPendingClients.put(client.CLIENT_ID, client);
        return client;
    }

    public CompletableFuture<String> authorize(String clientId, String redirectUri, String codeChallenge, String codeChallengeMethod) {
        McpOAuthClientState client = findClient(clientId);
        if (client == null) {
            return CompletableFuture.failedFuture(new McpOAuthException(ERROR_INVALID_CLIENT, "Unknown client: " + clientId));
        }
        if (!client.redirectUris().contains(redirectUri)) {
            return CompletableFuture.failedFuture(new McpOAuthException(ERROR_INVALID_REQUEST, "Redirect URI is not registered for this client."));
        }
        if (!"S256".equals(codeChallengeMethod) || codeChallenge.isBlank()) {
            return CompletableFuture.failedFuture(new McpOAuthException(ERROR_INVALID_REQUEST, "PKCE with code_challenge_method=S256 is required."));
        }

        UIAccess uiAccess = findUIAccess();
        if (uiAccess == null) {
            return CompletableFuture.failedFuture(new McpOAuthException(ERROR_ACCESS_DENIED, "No IDE window is open to approve the client."));
        }

        String clientName = client.CLIENT_NAME;
        return CompletableFuture.supplyAsync(() -> MessageBoxBuilder.<Boolean>create()
                .asQuestion()
                .title(McpServerLocalize.oauthConsentTitle())
                .text(McpServerLocalize.oauthConsentText(clientName, myApplication.getName()))
                .detail(McpServerLocalize.oauthConsentDetail(redirectUri))
                .button(MessageButtonRole.YES, McpServerLocalize.oauthConsentAllow(), Boolean.TRUE)
                .button(MessageButtonRole.NO, McpServerLocalize.oauthConsentDeny(), Boolean.FALSE)
                .asDefaultButton()
                .asExitButton()
                .showAsync(), uiAccess)
            .thenCompose(future -> future)
            .orTimeout(CONSENT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            .thenApply(allowed -> {
                if (!Boolean.TRUE.equals(allowed)) {
                    throw new McpOAuthException(ERROR_ACCESS_DENIED, "The user denied access.");
                }
                approve(client);
                String code = generateSecret();
                myCodes.put(code, new McpOAuthCode(clientId, redirectUri, codeChallenge, System.currentTimeMillis() + CODE_TTL_MILLIS));
                return code;
            });
    }

    public McpOAuthTokens exchangeCode(String clientId, String code, String redirectUri, String codeVerifier) {
        McpOAuthCode authorizationCode = myCodes.remove(code);
        if (authorizationCode == null || authorizationCode.expiresAt() < System.currentTimeMillis()) {
            throw new McpOAuthException(ERROR_INVALID_GRANT, "Authorization code is invalid or expired.");
        }
        if (!authorizationCode.clientId().equals(clientId) || !authorizationCode.redirectUri().equals(redirectUri)) {
            throw new McpOAuthException(ERROR_INVALID_GRANT, "Authorization code was issued to another client or redirect URI.");
        }
        if (!MessageDigest.isEqual(sha256(codeVerifier).getBytes(StandardCharsets.US_ASCII),
                                   authorizationCode.codeChallenge().getBytes(StandardCharsets.US_ASCII))) {
            throw new McpOAuthException(ERROR_INVALID_GRANT, "PKCE verification failed.");
        }

        synchronized (myLock) {
            McpOAuthClientState client = findApprovedClient(clientId);
            if (client == null) {
                throw new McpOAuthException(ERROR_INVALID_GRANT, "Client authorization was revoked.");
            }
            McpOAuthTokens tokens = issueTokens(client);
            persist();
            return tokens;
        }
    }

    public McpOAuthTokens refresh(@Nullable String clientId, String refreshToken) {
        String hash = sha256(refreshToken);
        synchronized (myLock) {
            for (McpOAuthClientState client : mySettings.oauthClients()) {
                if (client.REFRESH_TOKEN_HASH != null
                    && MessageDigest.isEqual(client.REFRESH_TOKEN_HASH.getBytes(StandardCharsets.US_ASCII), hash.getBytes(StandardCharsets.US_ASCII))
                    && (clientId == null || clientId.equals(client.CLIENT_ID))) {
                    McpOAuthTokens tokens = issueTokens(client);
                    persist();
                    return tokens;
                }
            }
        }
        throw new McpOAuthException(ERROR_INVALID_GRANT, "Refresh token is invalid or was revoked.");
    }

    public @Nullable McpOAuthClientState findClientByAccessToken(String accessToken) {
        McpOAuthAccessToken token = myAccessTokens.get(accessToken);
        if (token == null) {
            return null;
        }
        if (token.expiresAt() < System.currentTimeMillis()) {
            myAccessTokens.remove(accessToken);
            return null;
        }
        synchronized (myLock) {
            McpOAuthClientState client = findApprovedClient(token.clientId());
            if (client == null) {
                myAccessTokens.remove(accessToken);
                return null;
            }
            client.LAST_USED_AT = System.currentTimeMillis();
            return client;
        }
    }

    public List<McpOAuthClientState> getClients() {
        synchronized (myLock) {
            return new ArrayList<>(mySettings.oauthClients());
        }
    }

    public void revoke(String clientId) {
        synchronized (myLock) {
            mySettings.oauthClients().removeIf(client -> client.CLIENT_ID.equals(clientId));
        }
        myAccessTokens.values().removeIf(token -> token.clientId().equals(clientId));
        myCodes.values().removeIf(code -> code.clientId().equals(clientId));
        String principalId = McpPrincipal.oauthId(clientId);
        mySessionManager.terminateFor(principalId);
        mySettings.forgetApprovals(principalId);
        persist();
    }

    private void persist() {
        AppExecutorUtil.getAppExecutorService().execute(myApplication::saveSettings);
    }

    private McpOAuthTokens issueTokens(McpOAuthClientState client) {
        long now = System.currentTimeMillis();
        String accessToken = generateSecret();
        String refreshToken = generateSecret();
        myAccessTokens.put(accessToken, new McpOAuthAccessToken(client.CLIENT_ID, now + TimeUnit.SECONDS.toMillis(ACCESS_TOKEN_TTL_SECONDS)));
        client.REFRESH_TOKEN_HASH = sha256(refreshToken);
        client.LAST_USED_AT = now;
        return new McpOAuthTokens(accessToken, refreshToken, ACCESS_TOKEN_TTL_SECONDS);
    }

    private void approve(McpOAuthClientState client) {
        synchronized (myLock) {
            myPendingClients.remove(client.CLIENT_ID);
            if (findApprovedClient(client.CLIENT_ID) == null) {
                mySettings.oauthClients().add(client);
            }
            client.APPROVED_AT = System.currentTimeMillis();
        }
        persist();
    }

    private @Nullable McpOAuthClientState findClient(String clientId) {
        McpOAuthClientState pending = myPendingClients.get(clientId);
        if (pending != null) {
            return pending;
        }
        synchronized (myLock) {
            return findApprovedClient(clientId);
        }
    }

    private @Nullable McpOAuthClientState findApprovedClient(String clientId) {
        for (McpOAuthClientState client : mySettings.oauthClients()) {
            if (client.CLIENT_ID.equals(clientId)) {
                return client;
            }
        }
        return null;
    }

    private @Nullable UIAccess findUIAccess() {
        for (Project project : myProjectManager.getOpenProjects()) {
            if (!project.isDisposed() && project.getUIAccess().isValid()) {
                return project.getUIAccess();
            }
        }
        return null;
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        myRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static boolean isAllowedRedirectUri(String redirectUri) {
        URI uri;
        try {
            uri = URI.create(redirectUri);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || uri.getFragment() != null) {
            return false;
        }
        String lowerScheme = scheme.toLowerCase(Locale.ROOT);
        if (lowerScheme.equals("http")) {
            String host = uri.getHost();
            return host != null && (host.equals("127.0.0.1") || host.equalsIgnoreCase("localhost") || host.equals("[::1]"));
        }
        return !Set.of("https", "file", "javascript", "data", "vbscript", "ftp", "ws", "wss").contains(lowerScheme);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
