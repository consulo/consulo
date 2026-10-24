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
package consulo.http.client;

import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

final class HttpApiHelperImpl implements HttpApiHelper {
    private final Logger myLogger;
    private final HttpRequestConfigurer myRequestConfigurer;
    private final HttpClient myClient;

    HttpApiHelperImpl(Logger logger, HttpClientFactory clientFactory, HttpRequestConfigurer requestConfigurer) {
        myLogger = logger;
        myRequestConfigurer = requestConfigurer;
        myClient = clientFactory.createClient();
    }

    @Override
    public CompletableFuture<HttpRequest.Builder> request(URI uri) {
        return myRequestConfigurer.configureAsync(HttpRequest.newBuilder(uri));
    }

    @Override
    public <T extends @Nullable Object> CompletableFuture<HttpResponse<T>> sendAndAwait(HttpRequest request,
                                                                                        HttpResponse.BodyHandler<T> bodyHandler) {
        return sendAndAwait(request, HttpApiHelper.logName(request), bodyHandler);
    }

    @Override
    public <T extends @Nullable Object> CompletableFuture<HttpResponse<T>> sendAndAwait(HttpRequest request,
                                                                                        String requestName,
                                                                                        HttpResponse.BodyHandler<T> bodyHandler) {
        myLogger.debug(requestName);
        CompletableFuture<HttpResponse<T>> sent = myClient.sendAsync(request, bodyHandler);
        sent.whenComplete((response, error) -> {
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            if (cause instanceof IOException) {
                myLogger.debug(requestName + " : Connection error", cause);
            }
        });
        return sent;
    }

    @Override
    public <R extends @Nullable Object> CompletableFuture<R> handleStreamingResponse(String requestName,
                                                                                     HttpResponse<InputStream> response,
                                                                                     HttpBodyReader<R> bodyReader) {
        return HttpClientUtil.checkStatusCodeWithLogging(response, myLogger, requestName)
            .thenCompose(unused -> HttpClientUtil.readBodyWithLogging(response, myLogger, requestName, bodyReader));
    }
}
