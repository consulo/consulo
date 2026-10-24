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
package consulo.externalProcessAuthHelper;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.builtinWebServer.http.HttpRequest;
import consulo.builtinWebServer.http.HttpRequestHandler;
import consulo.builtinWebServer.http.HttpResponse;
import consulo.component.ProcessCanceledException;
import consulo.http.HttpMethod;
import consulo.logging.Logger;
import externalApp.ExternalAppHandler;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public abstract class ExternalProcessRest<T extends ExternalAppHandler> extends HttpRequestHandler {
    private static final Logger LOG = Logger.getInstance(ExternalProcessRest.class);

    private static final String PREFIX = "api/";

    private final String myEntryPointName;

    protected ExternalProcessRest(String entryPointName) {
        myEntryPointName = entryPointName;
    }

    protected abstract ExternalProcessHandlerService<T> getExternalProcessHandler();

    @Override
    public boolean isSupported(HttpRequest request) {
        return request.method() == HttpMethod.POST && checkPrefix(request.path(), PREFIX + myEntryPointName);
    }

    @Override
    public HttpResponse process(HttpRequest request) throws IOException {
        UUID uuid = getHandlerUUID(request);
        if (uuid == null || !getExternalProcessHandler().validateHandler(uuid)) {
            return HttpResponse.create(HttpURLConnection.HTTP_BAD_REQUEST, "text/plain", "Handler is not specified".getBytes(StandardCharsets.UTF_8));
        }

        String bodyContent = request.getContentAsString(StandardCharsets.UTF_8);

        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        CompletableFuture<HttpResponse> response = CompletableFuture
            .supplyAsync(() -> runHandler(indicator, uuid, bodyContent), AppExecutorUtil.getAppExecutorService())
            .handle((result, error) -> {
                if (error != null) {
                    Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                    if (cause instanceof ProcessCanceledException) {
                        throw new CancellationException();
                    }
                    LOG.warn(cause);
                    throw new CompletionException(cause);
                }
                if (result == null) {
                    return HttpResponse.create(HttpURLConnection.HTTP_NO_CONTENT, null, null);
                }
                return HttpResponse.ok("text/plain; charset=UTF-8", result.getBytes(StandardCharsets.UTF_8));
            });
        response.whenComplete((result, error) -> {
            if (error instanceof CancellationException) {
                indicator.cancel();
            }
        });
        return HttpResponse.deferred(response);
    }

    private static @Nullable UUID getHandlerUUID(HttpRequest request) {
        String handlerId = request.getParameterValue(ExternalAppHandler.HANDLER_ID_PARAMETER);
        if (handlerId == null) {
            return null;
        }
        try {
            return UUID.fromString(handlerId);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    private @Nullable String runHandler(EmptyProgressIndicator indicator, UUID uuid, String bodyContent) {
        return ProgressManager.getInstance().runProcess(() -> getExternalProcessHandler().invokeHandler(uuid, bodyContent), indicator);
    }
}
