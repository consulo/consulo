// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.graphql;

import com.intellij.collaboration.api.HttpApiHelper;
import com.intellij.collaboration.api.dto.GraphQLRequestDTO;
import com.intellij.collaboration.api.dto.GraphQLResponseDTO;
import com.intellij.collaboration.api.httpclient.ByteArrayProducingBodyPublisher;
import com.intellij.collaboration.api.httpclient.HttpClientUtil;
import com.intellij.collaboration.api.json.HttpJsonDeserializationException;
import com.intellij.collaboration.api.json.JsonDataSerializer;
import consulo.logging.Logger;
import kotlin.coroutines.Continuation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

final class GraphQLApiHelperImpl implements GraphQLApiHelper {
    private final @Nonnull Logger logger;
    private final @Nonnull HttpApiHelper httpHelper;
    private final @Nonnull JsonDataSerializer serializer;
    private final @Nonnull GraphQLDataDeserializer deserializer;

    GraphQLApiHelperImpl(
        @Nonnull Logger logger,
        @Nonnull HttpApiHelper httpHelper,
        @Nonnull JsonDataSerializer serializer,
        @Nonnull GraphQLDataDeserializer deserializer
    ) {
        this.logger = logger;
        this.httpHelper = httpHelper;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public @Nonnull HttpRequest query(@Nonnull URI uri, @Nonnull Supplier<String> loadQuery, @Nullable Object variablesObject) {
        ByteArrayProducingBodyPublisher publisher = new ByteArrayProducingBodyPublisher(() -> {
            logger.debug("GraphQL request " + uri);
            String query = loadQuery.get();
            GraphQLRequestDTO request = new GraphQLRequestDTO(query, variablesObject);
            byte[] jsonBytes = serializer.toJsonBytes(request);
            if (logger.isTraceEnabled()) {
                logger.trace("GraphQL request " + uri + " : Request body: " + new String(jsonBytes, StandardCharsets.UTF_8));
            }
            return jsonBytes;
        });

        return httpHelper.request(uri)
            .POST(publisher)
            .header(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON)
            .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable Object loadResponseByClass(
        @Nonnull HttpRequest request,
        @Nonnull Class<T> clazz,
        @Nonnull String[] pathFromData,
        @Nonnull Continuation<? super HttpResponse<? extends T>> continuation
    ) {
        String requestLogName = HttpApiHelper.logName(request);
        HttpResponse.BodyHandler<T> handler =
            HttpClientUtil.inflateAndReadWithErrorHandlingAndLogging(logger, request, (reader, responseInfo) -> {
                T result;
                try {
                    GraphQLResponseDTO<T, ?> responseDTO = deserializer.readAndMapGQLResponse(reader, pathFromData, clazz);
                    result = GraphQLResponseDTO.getOrThrow(responseDTO);
                }
                catch (Throwable e) {
                    logger.warn("API response deserialization failed", e);
                    throw new HttpJsonDeserializationException(requestLogName, e);
                }
                return result;
            });
        return httpHelper.sendAndAwaitCancellable(request, handler, continuation);
    }
}
