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
package consulo.externalService.impl.internal.auth;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.builtinWebServer.BuiltInServerManager;
import consulo.externalService.impl.internal.WebServiceApi;
import consulo.platform.Platform;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.RandomStringUtils;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logs the user in to the hub through the browser. The hub sends the browser back to the built-in server with the token
 * the login was started with, which is how the answer finds the request it belongs to.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class ExternalServiceLoginService {
    private final BuiltInServerManager myBuiltInServerManager;

    private final Map<String, CompletableFuture<HubCredentials>> myPending = new ConcurrentHashMap<>();

    @Inject
    public ExternalServiceLoginService(BuiltInServerManager builtInServerManager) {
        myBuiltInServerManager = builtInServerManager;
    }

    /**
     * Opens the login page of the hub in the browser.
     *
     * @return completes with the name of the user once the browser came back, or fails when the login did - cancelling it
     * forgets the request
     */
    public CompletableFuture<HubCredentials> login() {
        String token = RandomStringUtils.secure().nextAlphabetic(48);

        CompletableFuture<HubCredentials> future = new CompletableFuture<>();
        myPending.put(token, future);
        future.whenComplete((credentials, error) -> myPending.remove(token));

        String redirectUrl = "http://localhost:" + myBuiltInServerManager.getPort() + "/redirectAuth";

        String url = WebServiceApi.LINK_CONSULO.buildUrl() +
            "?token=" + token +
            "&host=" + URLEncoder.encode(getHostName(), StandardCharsets.UTF_8) +
            "&redirect=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);

        Platform.current().openInBrowser(url);
        return future;
    }

    /**
     * @return whether a login was waiting for this token
     */
    public boolean complete(String token, @Nullable HubCredentials credentials, @Nullable Throwable error) {
        CompletableFuture<HubCredentials> future = myPending.get(token);
        if (future == null) {
            return false;
        }

        if (error != null || credentials == null) {
            future.completeExceptionally(error != null ? error : new IllegalStateException("No user"));
        }
        else {
            future.complete(credentials);
        }
        return true;
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException ignored) {
            return "Unknown";
        }
    }
}
