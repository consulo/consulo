// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.json;

import com.intellij.collaboration.api.HttpApiHelper;
import com.intellij.collaboration.api.httpclient.ByteArrayProducingBodyPublisher;
import com.intellij.collaboration.api.httpclient.HttpClientUtil;
import consulo.logging.Logger;
import kotlin.coroutines.Continuation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Reader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

final class JsonHttpApiHelperImpl implements JsonHttpApiHelper {
    private final @Nonnull Logger logger;
    private final @Nonnull HttpApiHelper httpHelper;
    private final @Nonnull JsonDataSerializer serializer;
    private final @Nonnull JsonDataDeserializer deserializer;

    JsonHttpApiHelperImpl(
        @Nonnull Logger logger,
        @Nonnull HttpApiHelper httpHelper,
        @Nonnull JsonDataSerializer serializer,
        @Nonnull JsonDataDeserializer deserializer
    ) {
        this.logger = logger;
        this.httpHelper = httpHelper;
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    @Override
    public HttpRequest.@Nonnull BodyPublisher jsonBodyPublisher(@Nonnull URI uri, @Nonnull Object body) {
        return new ByteArrayProducingBodyPublisher(() -> {
            byte[] jsonBytes = serializer.toJsonBytes(body);
            if (logger.isTraceEnabled()) {
                logger.trace("Request POST " + uri + " : Request body: " + new String(jsonBytes, StandardCharsets.UTF_8));
            }
            return jsonBytes;
        });
    }

    @SuppressWarnings("unchecked")
    private <T, R> @Nullable Object loadWithMapperAndLogErrors(
        @Nonnull HttpRequest request,
        @Nonnull Function<T, R> map,
        @Nonnull Function<Reader, T> load,
        @Nonnull Continuation<? super HttpResponse<? extends R>> continuation
    ) {
        String requestLogName = HttpApiHelper.logName(request);
        HttpResponse.BodyHandler<R> bodyHandler = HttpClientUtil.inflateAndReadWithErrorHandlingAndLogging(
            logger,
            request,
            (reader, responseInfo) -> {
                T result;
                try {
                    result = load.apply(reader);
                }
                catch (Throwable e) {
                    logger.warn("API response deserialization failed", e);
                    throw new HttpJsonDeserializationException(requestLogName, e);
                }
                return map.apply(result);
            }
        );
        return httpHelper.sendAndAwaitCancellable(request, bodyHandler, continuation);
    }

    @Override
    public <T> @Nullable Object loadJsonValueByClass(
        @Nonnull HttpRequest request,
        @Nonnull Class<T> clazz,
        @Nonnull Continuation<? super HttpResponse<? extends T>> continuation
    ) {
        return loadWithMapperAndLogErrors(
            request,
            (T it) -> {
                if (it == null) {
                    throw new IllegalStateException("Empty response");
                }
                return it;
            },
            reader -> deserializer.fromJson(reader, clazz),
            continuation
        );
    }

    @Override
    public <T> @Nullable Object loadOptionalJsonValueByClass(
        @Nonnull HttpRequest request, @Nonnull Class<T> clazz,
        @Nonnull Continuation<? super HttpResponse<? extends T>> continuation
    ) {
        return loadWithMapperAndLogErrors(
            request,
            (T it) -> it,
            reader -> deserializer.fromJson(reader, clazz),
            continuation
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable Object loadJsonListByClass(
        @Nonnull HttpRequest request, @Nonnull Class<T> clazz,
        @Nonnull Continuation<? super HttpResponse<? extends List<T>>> continuation
    ) {
        return loadWithMapperAndLogErrors(
            request,
            (List<T> it) -> {
                if (it == null) {
                    throw new IllegalStateException("Empty response");
                }
                return it;
            },
            reader -> (List<T>) deserializer.fromJson(reader, List.class, clazz),
            continuation
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable Object loadOptionalJsonListByClass(
        @Nonnull HttpRequest request, @Nonnull Class<T> clazz,
        @Nonnull Continuation<? super HttpResponse<? extends List<T>>> continuation
    ) {
        return loadWithMapperAndLogErrors(
            request,
            (List<T> it) -> it,
            reader -> (List<T>) deserializer.fromJson(reader, List.class, clazz),
            continuation
        );
    }
}
