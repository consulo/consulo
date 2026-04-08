// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.json;

import com.intellij.collaboration.api.HttpApiHelper;
import com.intellij.collaboration.api.httpclient.HttpClientUtil;
import consulo.logging.Logger;
import kotlin.coroutines.Continuation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@ApiStatus.Experimental
public interface JsonHttpApiHelper {
    HttpRequest.@Nonnull BodyPublisher jsonBodyPublisher(@Nonnull URI uri, @Nonnull Object body);

    @Nullable
    <T> Object loadJsonValueByClass(
        @Nonnull HttpRequest request, @Nonnull Class<T> clazz,
        @Nonnull Continuation<? super HttpResponse<? extends T>> continuation
    );

    @Nullable
    <T> Object loadOptionalJsonValueByClass(
        @Nonnull HttpRequest request, @Nonnull Class<T> clazz,
        @Nonnull Continuation<? super HttpResponse<? extends T>> continuation
    );

    @Nullable
    <T> Object loadJsonListByClass(
        @Nonnull HttpRequest request, @Nonnull Class<T> clazz,
        @Nonnull Continuation<? super HttpResponse<? extends List<T>>> continuation
    );

    @Nullable
    <T> Object loadOptionalJsonListByClass(
        @Nonnull HttpRequest request, @Nonnull Class<T> clazz,
        @Nonnull Continuation<? super HttpResponse<? extends List<T>>> continuation
    );

    default HttpRequest.@Nonnull Builder withJsonContent(HttpRequest.@Nonnull Builder builder) {
        return builder.header(HttpClientUtil.CONTENT_TYPE_HEADER, HttpClientUtil.CONTENT_TYPE_JSON);
    }

    @Nonnull
    static JsonHttpApiHelper create(
        @Nonnull Logger logger,
        @Nonnull HttpApiHelper httpHelper,
        @Nonnull JsonDataSerializer serializer,
        @Nonnull JsonDataDeserializer deserializer
    ) {
        return new JsonHttpApiHelperImpl(logger, httpHelper, serializer, deserializer);
    }
}
