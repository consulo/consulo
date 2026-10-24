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
package consulo.http.json;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.http.client.EmptyHttpResponseException;
import consulo.http.client.HttpApiHelper;
import consulo.http.client.HttpBodyReader;
import consulo.http.client.HttpClientUtil;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

final class JsonHttpApiHelperImpl implements JsonHttpApiHelper, HttpApiHelper {
    private final Logger myLogger;
    private final HttpApiHelper myHttpHelper;
    private final JsonDataSerializer mySerializer;
    private final String myDefaultSendContentType;
    private final JsonDataDeserializer myDeserializer;
    private final String myDefaultAcceptMimeType;

    JsonHttpApiHelperImpl(Logger logger,
                          HttpApiHelper httpHelper,
                          JsonDataSerializer serializer,
                          String defaultSendContentType,
                          JsonDataDeserializer deserializer,
                          String defaultAcceptMimeType) {
        myLogger = logger;
        myHttpHelper = httpHelper;
        mySerializer = serializer;
        myDefaultSendContentType = defaultSendContentType;
        myDeserializer = deserializer;
        myDefaultAcceptMimeType = defaultAcceptMimeType;
    }

    @Override
    public CompletableFuture<HttpRequest.Builder> request(URI uri) {
        return myHttpHelper.request(uri);
    }

    @Override
    public <T extends @Nullable Object> CompletableFuture<HttpResponse<T>> sendAndAwait(HttpRequest request,
                                                                                        HttpResponse.BodyHandler<T> bodyHandler) {
        return myHttpHelper.sendAndAwait(request, bodyHandler);
    }

    @Override
    public <T extends @Nullable Object> CompletableFuture<HttpResponse<T>> sendAndAwait(HttpRequest request,
                                                                                        String requestName,
                                                                                        HttpResponse.BodyHandler<T> bodyHandler) {
        return myHttpHelper.sendAndAwait(request, requestName, bodyHandler);
    }

    @Override
    public <R extends @Nullable Object> CompletableFuture<R> handleStreamingResponse(String requestName,
                                                                                     HttpResponse<InputStream> response,
                                                                                     HttpBodyReader<R> bodyReader) {
        return myHttpHelper.handleStreamingResponse(requestName, response, bodyReader);
    }

    @Override
    public CompletableFuture<HttpRequest.Builder> putJson(URI uri, Object body) {
        return request(uri).thenCombine(jsonBodyPublisher("PUT", uri, body), (builder, publisher) -> withJsonContent(builder.PUT(publisher)));
    }

    @Override
    public CompletableFuture<HttpRequest.Builder> postJson(URI uri, Object body) {
        return request(uri).thenCombine(jsonBodyPublisher("POST", uri, body), (builder, publisher) -> withJsonContent(builder.POST(publisher)));
    }

    @Override
    public CompletableFuture<HttpRequest.Builder> sendJson(URI uri, String httpMethod, Object body) {
        return request(uri).thenCombine(jsonBodyPublisher(httpMethod, uri, body),
            (builder, publisher) -> withJsonContent(builder.method(httpMethod, publisher)));
    }

    @Override
    public HttpRequest.Builder withJsonContent(HttpRequest.Builder builder) {
        return builder.header(HttpClientUtil.CONTENT_TYPE_HEADER, myDefaultSendContentType);
    }

    @Override
    public CompletableFuture<HttpRequest.BodyPublisher> jsonBodyPublisher(String httpMethod, URI uri, Object body) {
        String requestName = HttpClientUtil.getRequestName(httpMethod, uri);
        return CompletableFuture.supplyAsync(() -> {
            byte[] jsonBytes;
            try {
                jsonBytes = mySerializer.toJsonBytes(body);
            }
            catch (Exception e) {
                myLogger.warn("API request serialization failed", e);
                throw new HttpJsonSerializationException(requestName, e);
            }
            if (myLogger.isTraceEnabled()) {
                myLogger.trace(requestName + " : Request body: " + new String(jsonBytes, StandardCharsets.UTF_8));
            }
            return HttpRequest.BodyPublishers.ofByteArray(jsonBytes);
        }, AppExecutorUtil.getAppExecutorService());
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> loadJsonValueByClass(HttpRequest request, Class<T> clazz) {
        return sendAndRead(request, (requestName, stream, mimeType, charset) -> {
            checkJsonMimeType(mimeType);
            T value = deserialize(requestName, stream, charset, clazz);
            if (value == null) {
                throw new CompletionException(new EmptyHttpResponseException(requestName));
            }
            return value;
        });
    }

    @Override
    public <T> CompletableFuture<HttpResponse<@Nullable T>> loadOptionalJsonValueByClass(HttpRequest request, Class<T> clazz) {
        return sendAndRead(request, (requestName, stream, mimeType, charset) -> {
            if (mimeType == null || !HttpClientUtil.isJsonMimeType(mimeType)) {
                myLogger.debug(requestName + " : no content type - inferring no content");
                return null;
            }
            return deserialize(requestName, stream, charset, clazz);
        });
    }

    @Override
    public <T> CompletableFuture<HttpResponse<List<T>>> loadJsonListByClass(HttpRequest request, Class<T> clazz) {
        return sendAndRead(request, (requestName, stream, mimeType, charset) -> {
            checkJsonMimeType(mimeType);
            List<T> value = deserializeList(requestName, stream, charset, clazz);
            if (value == null) {
                throw new CompletionException(new EmptyHttpResponseException(requestName));
            }
            return value;
        });
    }

    @Override
    public <T> CompletableFuture<HttpResponse<@Nullable List<T>>> loadOptionalJsonListByClass(HttpRequest request, Class<T> clazz) {
        return sendAndRead(request, (requestName, stream, mimeType, charset) -> {
            if (mimeType == null || !HttpClientUtil.isJsonMimeType(mimeType)) {
                myLogger.debug(requestName + " : no content type - inferring no content");
                return null;
            }
            return deserializeList(requestName, stream, charset, clazz);
        });
    }

    private <T extends @Nullable Object> CompletableFuture<HttpResponse<T>> sendAndRead(HttpRequest originalRequest, NamedBodyReader<T> bodyReader) {
        HttpRequest request = HttpClientUtil.ensureAcceptHeader(originalRequest, myDefaultAcceptMimeType);
        String requestName = HttpApiHelper.logName(request);
        return sendAndRead(request, requestName, (stream, mimeType, charset) -> bodyReader.read(requestName, stream, mimeType, charset));
    }

    private <T> @Nullable T deserialize(String requestName, InputStream stream, @Nullable Charset charset, Class<T> clazz) {
        try {
            return myDeserializer.readJson(stream, charset == null ? StandardCharsets.UTF_8 : charset, clazz);
        }
        catch (Exception e) {
            myLogger.warn("API response deserialization failed", e);
            throw new HttpJsonDeserializationException(requestName, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> @Nullable List<T> deserializeList(String requestName, InputStream stream, @Nullable Charset charset, Class<T> clazz) {
        Object value;
        try {
            value = myDeserializer.readJson(stream, charset == null ? StandardCharsets.UTF_8 : charset, List.class, clazz);
        }
        catch (Exception e) {
            myLogger.warn("API response deserialization failed", e);
            throw new HttpJsonDeserializationException(requestName, e);
        }
        return value instanceof List<?> list ? (List<T>) list : null;
    }

    private static void checkJsonMimeType(@Nullable String mimeType) {
        if (mimeType == null || !HttpClientUtil.isJsonMimeType(mimeType)) {
            throw new IllegalStateException("JSON mime type expected, got " + mimeType);
        }
    }
}
