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

import consulo.http.HttpMethod;
import consulo.http.HttpRequestBuilder;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * A helper for building an API client on {@link HttpRequestBuilder} that handles things like common headers (ex. auth),
 * logging, content encoding and error handling
 */
public interface HttpApiHelper {
    /**
     * Creates a request builder for the given URL and method.
     * Pre-configures the builder according to the rules defined by the implementation.
     */
    CompletableFuture<HttpRequestBuilder> request(String url, HttpMethod method);

    /**
     * Sends the request and reads the response from the body input stream with a {@code bodyReader}, taking care of
     * logging, error handling and {@code gzip} encoding
     * <p>
     * Fails with an {@link java.io.IOException} when a connection has failed and with {@link HttpStatusErrorException} when an
     * error status code is received (&gt;=400), in this case the body is read as a string and put into the exception
     *
     * @param requestName is used as an identifier when logging
     */
    <T extends @Nullable Object> CompletableFuture<HttpApiResponse<T>> sendAndRead(HttpRequestBuilder request,
                                                                                   String requestName,
                                                                                   HttpBodyReader<T> bodyReader);

    default CompletableFuture<HttpRequestBuilder> request(String url) {
        return request(url, HttpMethod.GET);
    }

    default CompletableFuture<HttpRequestBuilder> request(URI uri) {
        return request(uri.toString(), HttpMethod.GET);
    }

    default CompletableFuture<HttpRequestBuilder> request(URI uri, HttpMethod method) {
        return request(uri.toString(), method);
    }

    /**
     * Same as {@link #sendAndRead(HttpRequestBuilder, String, HttpBodyReader)} with the request name made of the request itself
     */
    default <T extends @Nullable Object> CompletableFuture<HttpApiResponse<T>> sendAndRead(HttpRequestBuilder request,
                                                                                           HttpBodyReader<T> bodyReader) {
        return sendAndRead(request, logName(request), bodyReader);
    }

    /**
     * Sends the request and reads the response body with a {@code bodyReader}, taking care of logging and error handling
     */
    default <T extends @Nullable Object> CompletableFuture<T> sendAndReadBody(HttpRequestBuilder request, HttpBodyReader<T> bodyReader) {
        return sendAndRead(request, bodyReader).thenApply(HttpApiResponse::body);
    }

    /**
     * Sends the request and ignores the response body (except when there's an error or trace logging is enabled)
     */
    default CompletableFuture<?> sendAndAwait(HttpRequestBuilder request) {
        return sendAndReadBody(request, (stream, mimeType, charset) -> Boolean.TRUE);
    }

    static HttpApiHelper create() {
        return create(Logger.getInstance(HttpApiHelper.class));
    }

    static HttpApiHelper create(Logger logger) {
        return create(logger, defaultRequestConfigurer());
    }

    static HttpApiHelper create(Logger logger, HttpRequestConfigurer requestConfigurer) {
        return new HttpApiHelperImpl(logger, requestConfigurer);
    }

    static HttpRequestConfigurer defaultRequestConfigurer() {
        return new CompoundRequestConfigurer(new RequestTimeoutConfigurer(), new CommonHeadersConfigurer());
    }

    static String logName(HttpRequestBuilder request) {
        return HttpClientUtil.getRequestName(request.getMethod().name(), request.getUrl());
    }
}
