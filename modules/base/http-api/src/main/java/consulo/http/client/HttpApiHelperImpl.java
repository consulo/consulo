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

import consulo.application.Application;
import consulo.http.HttpMethod;
import consulo.http.HttpRequestBuilder;
import consulo.http.HttpRequestBuilderFactory;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

final class HttpApiHelperImpl implements HttpApiHelper {
    private final Logger myLogger;
    private final HttpRequestConfigurer myRequestConfigurer;

    HttpApiHelperImpl(Logger logger, HttpRequestConfigurer requestConfigurer) {
        myLogger = logger;
        myRequestConfigurer = requestConfigurer;
    }

    @Override
    public CompletableFuture<HttpRequestBuilder> request(String url, HttpMethod method) {
        HttpRequestBuilderFactory factory = Application.get().getInstance(HttpRequestBuilderFactory.class);
        return myRequestConfigurer.configureAsync(factory.newBuilder(url, method));
    }

    @Override
    public <T extends @Nullable Object> CompletableFuture<HttpApiResponse<T>> sendAndRead(HttpRequestBuilder request,
                                                                                          String requestName,
                                                                                          HttpBodyReader<T> bodyReader) {
        myLogger.debug(requestName);
        CompletableFuture<HttpApiResponse<T>> sent = request.allowErrorCodes(true)
            .gzip(true)
            .connectAsync(response -> HttpClientUtil.readResponse(response, myLogger, requestName, bodyReader));
        sent.whenComplete((response, error) -> {
            if (error instanceof IOException) {
                myLogger.debug(requestName + " : Connection error", error);
            }
        });
        return sent;
    }
}
