// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.page;

import com.intellij.collaboration.api.data.GraphQLRequestPagination;
import com.intellij.collaboration.api.dto.GraphQLPagedResponseDataDTO;
import com.intellij.collaboration.api.util.LinkHttpHeaderValue;
import com.intellij.collaboration.util.URIUtil;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowCollector;
import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.function.Function;

public final class ApiPageUtil {
    private ApiPageUtil() {
    }

    @Nonnull
    public static <T> Flow<GraphQLPagedResponseDataDTO<T>> createGQLPagesFlow(
        boolean reversed,
        @Nonnull Function<GraphQLRequestPagination, Object> loader
    ) {
        return new Flow<>() {
            @Override
            public @Nullable Object collect(
                @Nonnull FlowCollector<? super GraphQLPagedResponseDataDTO<T>> collector,
                @Nonnull Continuation<? super kotlin.Unit> continuation
            ) {
                // This is a suspend flow - requires Kotlin coroutine infrastructure to collect.
                throw new UnsupportedOperationException("Flow must be collected from Kotlin coroutine context");
            }
        };
    }

    @Nonnull
    public static <T> Flow<GraphQLPagedResponseDataDTO<T>> createGQLPagesFlow(
        @Nonnull Function<GraphQLRequestPagination, Object> loader
    ) {
        return createGQLPagesFlow(false, loader);
    }

    @Nonnull
    public static <T> Flow<HttpResponse<T>> createPagesFlowByLinkHeader(
        @Nonnull URI initialURI,
        @Nonnull Function<URI, Object> request
    ) {
        return new Flow<>() {
            @Override
            public @Nullable Object collect(
                @Nonnull FlowCollector<? super HttpResponse<T>> collector,
                @Nonnull Continuation<? super kotlin.Unit> continuation
            ) {
                // This is a suspend flow - requires Kotlin coroutine infrastructure to collect.
                throw new UnsupportedOperationException("Flow must be collected from Kotlin coroutine context");
            }
        };
    }

    /**
     * Prefer not to use this method!
     * This is a last resort if Link headers are missing and you need paginated info.
     * It's recommended to use the Link header in all other cases though.
     * <p>
     * Careful: starts pagination at page=1 and increments from there.
     */
    @ApiStatus.Internal
    @Nonnull
    public static <T> Flow<HttpResponse<? extends List<T>>> createPagesFlowByPagination(
        @Nonnull Function<Integer, Object> loader
    ) {
        return new Flow<>() {
            @Override
            public @Nullable Object collect(
                @Nonnull FlowCollector<? super HttpResponse<? extends List<T>>> collector,
                @Nonnull Continuation<? super kotlin.Unit> continuation
            ) {
                // This is a suspend flow - requires Kotlin coroutine infrastructure to collect.
                throw new UnsupportedOperationException("Flow must be collected from Kotlin coroutine context");
            }
        };
    }
}
