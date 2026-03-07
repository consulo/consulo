// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.graphql;

import com.intellij.collaboration.api.HttpApiHelper;
import com.intellij.collaboration.api.json.JsonDataSerializer;
import consulo.logging.Logger;
import kotlin.coroutines.Continuation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Supplier;

@ApiStatus.Experimental
public interface GraphQLApiHelper {
    @Nonnull
    HttpRequest query(@Nonnull URI uri, @Nonnull Supplier<String> loadQuery, @Nullable Object variablesObject);

    default @Nonnull HttpRequest query(@Nonnull URI uri, @Nonnull Supplier<String> loadQuery) {
        return query(uri, loadQuery, null);
    }

    @Nullable
    <T> Object loadResponseByClass(
        @Nonnull HttpRequest request,
        @Nonnull Class<T> clazz,
        @Nonnull String[] pathFromData,
        @Nonnull Continuation<? super HttpResponse<? extends T>> continuation
    );

    @Nonnull
    static GraphQLApiHelper create(
        @Nonnull Logger logger,
        @Nonnull HttpApiHelper httpHelper,
        @Nonnull JsonDataSerializer serializer,
        @Nonnull GraphQLDataDeserializer deserializer
    ) {
        return new GraphQLApiHelperImpl(logger, httpHelper, serializer, deserializer);
    }
}
