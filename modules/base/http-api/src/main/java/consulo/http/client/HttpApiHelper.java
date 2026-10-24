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

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * A helper for building a JDK11 HTTP client based API client that handles things like
 * common headers (ex. auth), logging, content encoding and error handling
 */
public interface HttpApiHelper {
    /**
     * Creates a request builder from the given URI.
     * Pre-configures the builder according to the rules defined by the implementation.
     */
    CompletableFuture<HttpRequest.Builder> request(URI uri);

    /**
     * Sends the given request and awaits a response in a cancellable way.
     * The body handler is used to fully handle the body, no additional handling is done by this method.
     */
    <T extends @Nullable Object> CompletableFuture<HttpResponse<T>> sendAndAwait(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler);

    /**
     * Same as {@link #sendAndAwait(HttpRequest, HttpResponse.BodyHandler)}, but allows a custom {@code requestName} for logging purposes
     */
    <T extends @Nullable Object> CompletableFuture<HttpResponse<T>> sendAndAwait(HttpRequest request,
                                                                                 String requestName,
                                                                                 HttpResponse.BodyHandler<T> bodyHandler);

    /**
     * Reads the response from the body input stream with a {@code bodyReader}
     * <p>
     * Takes care of logging, error handling and {@code gzip} encoding
     * <p>
     * Fails with an {@link java.io.IOException} when a connection has failed and with {@link HttpStatusErrorException} when an
     * error status code is received (&gt;400), in this case the body is read as a string and put into the exception
     */
    <R extends @Nullable Object> CompletableFuture<R> handleStreamingResponse(String requestName,
                                                                              HttpResponse<InputStream> response,
                                                                              HttpBodyReader<R> bodyReader);

    /**
     * Creates a request builder from the given URI String.
     */
    default CompletableFuture<HttpRequest.Builder> request(String uri) {
        return request(URI.create(uri));
    }

    /**
     * Sends the given request, reads the response from the body input stream with a {@code bodyReader},
     * taking care of logging and error handling, and returns the response with the {@link HttpResponse#body()} stream mapped to a value
     * <p>
     * The request is configured for {@code gzip} encoding and the {@code InputStream} passed to {@code bodyReader} is a decoded response
     * stream if the server returns an encoded response.
     * NB: Any other pre-configured encoding is disabled.
     *
     * @param requestName is used as an identifier when logging
     */
    default <T extends @Nullable Object> CompletableFuture<HttpResponse<T>> sendAndRead(HttpRequest request,
                                                                                        String requestName,
                                                                                        HttpBodyReader<T> bodyReader) {
        HttpRequest actualRequest = HttpClientUtil.forceAcceptGzipEncoding(request);
        CompletableFuture<HttpResponse<InputStream>> sent = sendAndAwait(actualRequest, requestName, HttpResponse.BodyHandlers.ofInputStream());
        CompletableFuture<HttpResponse<T>> result = sent.thenCompose(response ->
            handleStreamingResponse(requestName, response, bodyReader).thenApply(body -> HttpClientUtil.withNewBody(response, body))
        );
        return HttpClientUtil.cancelWith(result, sent);
    }

    /**
     * Same as {@link #sendAndRead(HttpRequest, String, HttpBodyReader)} with the request name made of the request itself
     */
    default <T extends @Nullable Object> CompletableFuture<HttpResponse<T>> sendAndRead(HttpRequest request, HttpBodyReader<T> bodyReader) {
        return sendAndRead(request, logName(request), bodyReader);
    }

    /**
     * Sends the given request and reads the response from the body input stream with a {@code bodyReader},
     * taking care of logging and error handling
     * <p>
     * The request is configured for {@code gzip} encoding and the {@code InputStream} passed to {@code bodyReader} is a decoded response
     * stream if the server returns an encoded response.
     * NB: Any other pre-configured encoding is disabled.
     */
    default <T extends @Nullable Object> CompletableFuture<T> sendAndReadBody(HttpRequest request, HttpBodyReader<T> bodyReader) {
        return sendAndRead(request, logName(request), bodyReader).thenApply(HttpResponse::body);
    }

    /**
     * Sends the given request and ignores the response body (except when there's an error or trace logging is enabled)
     * <p>
     * The request is configured for {@code gzip} encoding.
     * NB: Any other pre-configured encoding is disabled.
     */
    default CompletableFuture<?> sendAndAwait(HttpRequest request) {
        HttpRequest actualRequest = HttpClientUtil.forceAcceptGzipEncoding(request);
        return sendAndReadBody(actualRequest, (stream, mimeType, charset) -> Boolean.TRUE);
    }

    static HttpApiHelper create() {
        return create(Logger.getInstance(HttpApiHelper.class));
    }

    static HttpApiHelper create(Logger logger) {
        return create(logger, new HttpClientFactoryBase());
    }

    static HttpApiHelper create(Logger logger, HttpClientFactory clientFactory) {
        return create(logger, clientFactory, defaultRequestConfigurer());
    }

    static HttpApiHelper create(Logger logger, HttpClientFactory clientFactory, HttpRequestConfigurer requestConfigurer) {
        return new HttpApiHelperImpl(logger, clientFactory, requestConfigurer);
    }

    static HttpRequestConfigurer defaultRequestConfigurer() {
        return new CompoundRequestConfigurer(new RequestTimeoutConfigurer(), new CommonHeadersConfigurer());
    }

    static String logName(HttpRequest request) {
        return HttpClientUtil.getRequestName(request);
    }
}
