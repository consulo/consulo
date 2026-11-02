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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.credentialStorage.CredentialAttributes;
import consulo.credentialStorage.CredentialAttributesUtil;
import consulo.credentialStorage.Credentials;
import consulo.credentialStorage.PasswordSafe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class McpAuthTokenManager {
    public static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER = "token";
    private static final CredentialAttributes ATTRIBUTES =
        new CredentialAttributes(CredentialAttributesUtil.generateServiceName("MCP Server", USER), USER);

    private final PasswordSafe myPasswordSafe;
    private final SecureRandom myRandom = new SecureRandom();

    private volatile @Nullable String myToken;

    @Inject
    public McpAuthTokenManager(PasswordSafe passwordSafe) {
        myPasswordSafe = passwordSafe;
    }

    public CompletableFuture<String> getToken() {
        String token = myToken;
        if (token != null) {
            return CompletableFuture.completedFuture(token);
        }
        return myPasswordSafe.getAsync(ATTRIBUTES).thenApply(this::useStoredOrGenerate);
    }

    public String regenerate() {
        String token = generate();
        myPasswordSafe.set(ATTRIBUTES, new Credentials(USER, token));
        myToken = token;
        return token;
    }

    public static String toAuthorizationValue(String token) {
        return BEARER_PREFIX + token;
    }

    public static @Nullable String extractBearer(@Nullable String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String bearer = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return bearer.isEmpty() ? null : bearer;
    }

    public static boolean matches(String token, @Nullable String bearer) {
        return bearer != null && MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), bearer.getBytes(StandardCharsets.UTF_8));
    }

    private synchronized String useStoredOrGenerate(@Nullable Credentials credentials) {
        String current = myToken;
        if (current != null) {
            return current;
        }
        String stored = credentials == null ? null : credentials.getPasswordAsString();
        if (stored == null || stored.isEmpty()) {
            return regenerate();
        }
        myToken = stored;
        return stored;
    }

    private String generate() {
        byte[] bytes = new byte[32];
        myRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
