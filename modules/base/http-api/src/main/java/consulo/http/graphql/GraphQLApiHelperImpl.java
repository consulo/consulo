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
package consulo.http.graphql;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.http.HttpMethod;
import consulo.http.HttpRequestBuilder;
import consulo.http.client.HttpApiHelper;
import consulo.http.client.HttpApiResponse;
import consulo.http.client.HttpBodyReader;
import consulo.http.client.HttpClientUtil;
import consulo.http.graphql.dto.GraphQLErrorDTO;
import consulo.http.graphql.dto.GraphQLRequestDTO;
import consulo.http.graphql.dto.GraphQLResponseDTO;
import consulo.http.json.HttpJsonDeserializationException;
import consulo.http.json.HttpJsonSerializationException;
import consulo.http.json.JsonDataSerializer;
import consulo.logging.Logger;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

final class GraphQLApiHelperImpl implements GraphQLApiHelper, HttpApiHelper {
    private final Logger myLogger;
    private final HttpApiHelper myHttpHelper;
    private final JsonDataSerializer mySerializer;
    private final GraphQLDataDeserializer myDeserializer;

    GraphQLApiHelperImpl(Logger logger, HttpApiHelper httpHelper, JsonDataSerializer serializer, GraphQLDataDeserializer deserializer) {
        myLogger = logger;
        myHttpHelper = httpHelper;
        mySerializer = serializer;
        myDeserializer = deserializer;
    }

    @Override
    public CompletableFuture<HttpRequestBuilder> request(String url, HttpMethod method) {
        return myHttpHelper.request(url, method);
    }

    @Override
    public <T extends @Nullable Object> CompletableFuture<HttpApiResponse<T>> sendAndRead(HttpRequestBuilder request,
                                                                                          String requestName,
                                                                                          HttpBodyReader<T> bodyReader) {
        return myHttpHelper.sendAndRead(request, requestName, bodyReader);
    }

    @Override
    public CompletableFuture<HttpRequestBuilder> query(URI uri, Supplier<String> loadQuery, @Nullable Object variablesObject) {
        String requestName = gqlRequestName(uri.toString());
        CompletableFuture<byte[]> body = CompletableFuture.supplyAsync(() -> {
            GraphQLRequestDTO request = new GraphQLRequestDTO(loadQuery.get(), variablesObject);
            byte[] jsonBytes;
            try {
                jsonBytes = mySerializer.toJsonBytes(request);
            }
            catch (Exception e) {
                myLogger.warn("GraphQL API request serialization failed", e);
                throw new HttpJsonSerializationException(requestName, e);
            }
            if (myLogger.isTraceEnabled()) {
                myLogger.trace(requestName + " : Request body: " + new String(jsonBytes, StandardCharsets.UTF_8));
            }
            return jsonBytes;
        }, AppExecutorUtil.getAppExecutorService());
        return body.thenCompose(jsonBytes -> myHttpHelper.request(uri, HttpMethod.POST).thenApply(builder -> builder
            .body(jsonBytes)
            .header(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.MIME_TYPE_JSON)));
    }

    @Override
    public <T> CompletableFuture<HttpApiResponse<@Nullable T>> loadResponseByClass(HttpRequestBuilder request,
                                                                                    Class<T> clazz,
                                                                                    String... pathFromData) {
        HttpRequestBuilder jsonRequest = HttpClientUtil.ensureAcceptHeader(request, HttpClientUtil.MIME_TYPE_JSON);
        String requestName = gqlRequestName(request.getUrl());
        return sendAndRead(jsonRequest, requestName, (stream, mimeType, charset) -> {
            GraphQLResponseDTO<@Nullable T, GraphQLErrorDTO> result;
            try {
                result = myDeserializer.readAndMapGQLResponse(stream, charset == null ? StandardCharsets.UTF_8 : charset, pathFromData, clazz);
            }
            catch (Throwable e) {
                myLogger.warn("API response deserialization failed", e);
                throw new HttpJsonDeserializationException(requestName, e);
            }
            return result.getOrThrow();
        });
    }

    private static String gqlRequestName(String url) {
        return "GraphQL request POST " + url;
    }
}
