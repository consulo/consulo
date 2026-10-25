// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.accountManagement.oauth;

import consulo.platform.Platform;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The basic service that implements general authorization flow methods
 *
 * @param <T> Service credentials, must implement the OAuthCredentials interface
 */
public abstract class OAuthServiceBase<T extends OAuthCredentials> implements OAuthService<T> {
    protected final AtomicReference<@Nullable OAuthRequestWithResult<T>> myCurrentRequest = new AtomicReference<>();

    @Override
    public CompletableFuture<T> authorize(OAuthRequest<T> request) {
        if (!myCurrentRequest.compareAndSet(null, new OAuthRequestWithResult<>(request, new CompletableFuture<>()))) {
            OAuthRequestWithResult<T> current = myCurrentRequest.get();
            if (current != null) {
                return current.result();
            }
            return authorize(request);
        }

        OAuthRequestWithResult<T> current = myCurrentRequest.get();
        assert current != null;
        CompletableFuture<T> result = current.result();
        result.whenComplete((credentials, error) -> myCurrentRequest.set(null));
        startAuthorization(request);

        return result;
    }

    private static @Nullable String getAuthorizationCode(Map<String, List<String>> parameters) {
        return getFirstParameter(parameters, "code");
    }

    private static @Nullable String getFirstParameter(Map<String, List<String>> parameters, String name) {
        List<String> values = parameters.get(name);
        return values == null ? null : ContainerUtil.getFirstItem(values);
    }

    private boolean handleServerCallback(String path, Map<String, List<String>> parameters) {
        OAuthRequestWithResult<T> request = myCurrentRequest.get();
        if (request == null) {
            return false;
        }

        if (!path.equals(request.request().getAuthorizationCodeUrl().getPath())) {
            return false;
        }
        // Handle Authorization Response — https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2
        String code = getAuthorizationCode(parameters);
        if (code == null) {
            String error = getFirstParameter(parameters, "error");
            String errorDescription = getFirstParameter(parameters, "error_description");
            String message = errorDescription != null ? errorDescription : error != null ? error : "Unknown error";
            request.result().completeExceptionally(new RuntimeException(message));
            return false;
        }

        processCode(request, code);
        CompletableFuture<T> result = request.result();
        return result.isDone() && !result.isCancelled() && !result.isCompletedExceptionally();
    }

    @Override
    public @Nullable OAuthResult<T> handleOAuthServerCallback(String path, Map<String, List<String>> parameters) {
        OAuthRequestWithResult<T> current = myCurrentRequest.get();
        if (current == null) {
            return null;
        }
        boolean isAccepted = handleServerCallback(path, parameters);
        return new OAuthResult<>(current.request(), isAccepted);
    }

    @Override
    public @Nullable OAuthResultCredentials<T> handleOAuthServerCredentialsCallback(String path, Map<String, List<String>> parameters) {
        OAuthRequestWithResult<T> current = myCurrentRequest.get();
        if (current == null) {
            return null;
        }
        boolean isAccepted = handleServerCallback(path, parameters);
        if (isAccepted) {
            return OAuthResultCredentials.success(current.request(), current.result().join());
        }
        return OAuthResultCredentials.failure(
            current.request(),
            new CallbackNotReadyException("Server callback is not ready or completed exceptionally")
        );
    }

    protected void startAuthorization(OAuthRequest<T> request) {
        String authUrl = request.getAuthUrlWithParameters().toExternalForm();
        Platform.current().openInBrowser(authUrl);
    }

    private void processCode(OAuthRequestWithResult<T> request, String code) {
        CompletableFuture<T> result = request.result();
        try {
            AcquireCredentialsResult<T> acquireResult = request.request().getCredentialsAcquirer().acquireCredentials(code);
            switch (acquireResult) {
                case SuccessAcquireCredentialsResult<T> success -> result.complete(success.credentials());
                case ErrorAcquireCredentialsResult<T> error -> result.completeExceptionally(new RuntimeException(error.description()));
            }
        }
        catch (Exception e) {
            result.completeExceptionally(e);
        }
    }
}
