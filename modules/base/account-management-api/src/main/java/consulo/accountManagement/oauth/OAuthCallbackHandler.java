// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.logging.Logger;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * The base class of the callback handler for authorization services
 */
public abstract class OAuthCallbackHandler extends HttpRequestHandler {
    private static final Logger LOG = Logger.getInstance(OAuthCallbackHandler.class);

    public static final String PREFIX = "api";

    protected OAuthService<?> getService() {
        return oauthService();
    }

    public String getServiceName() {
        return getService().getName();
    }

    protected abstract OAuthService<?> oauthService();

    @Override
    public boolean isSupported(HttpRequest request) {
        return super.isSupported(request) && checkPrefix(request.path(), PREFIX + "/" + getServiceName());
    }

    @Override
    public HttpResponse process(HttpRequest request) {
        String path = request.path();
        CompletableFuture<HttpResponse> response = CompletableFuture.supplyAsync(() -> {
            OAuthResult<?> oAuthResult = getService().handleOAuthServerCallback(path, request.getParameters());
            return oAuthResult == null ? null : handleOAuthResult(oAuthResult);
        }, AppExecutorUtil.getAppExecutorService()).handle((res, err) -> {
            if (err != null) {
                LOG.warn(err);
                return HttpResponse.create(HttpURLConnection.HTTP_INTERNAL_ERROR, null, null);
            }
            else if (res != null) {
                return switch (res) {
                    case PageAcceptCodeHandleResult page ->
                        HttpResponse.ok("text/html;charset=utf-8", page.html().getBytes(StandardCharsets.UTF_8));
                    case RedirectAcceptCodeHandleResult redirect -> sendRedirect(redirect);
                };
            }
            else {
                return HttpResponse.create(HttpURLConnection.HTTP_NO_CONTENT, null, null);
            }
        });
        return HttpResponse.deferred(response);
    }

    protected abstract AcceptCodeHandleResult handleOAuthResult(OAuthResult<?> oAuthResult);

    private static HttpResponse sendRedirect(RedirectAcceptCodeHandleResult redirect) {
        return HttpResponse.create(HttpURLConnection.HTTP_MOVED_TEMP, null, null)
            .withHeader("Location", redirect.url().toExternalForm());
    }
}
