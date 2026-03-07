// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import com.intellij.collaboration.api.dto.GraphQLConnectionDTO;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Internal
public final class GraphQLListLoader {
    private GraphQLListLoader() {
    }

    @Nonnull
    public static <K, V> ReloadablePotentiallyInfiniteListLoader<V> startIn(
        @Nonnull CoroutineScope cs,
        @Nonnull Function<V, K> extractKey,
        @Nullable Flow<kotlin.Unit> requestReloadFlow,
        @Nullable Flow<kotlin.Unit> requestRefreshFlow,
        @Nullable Flow<Change<V>> requestChangeFlow,
        boolean shouldTryToLoadAll,
        @Nonnull GraphQLRequestPerformer<V> performRequest
    ) {
        GraphQLListLoaderImpl<K, V> loader = new GraphQLListLoaderImpl<>(extractKey, shouldTryToLoadAll, performRequest);

        if (requestReloadFlow != null) {
            CoroutineUtil.launchNow(cs, (scope, continuation) -> {
                return requestReloadFlow.collect((unit, cont) -> {
                    return loader.reload(cont);
                }, continuation);
            });
        }
        if (requestRefreshFlow != null) {
            kotlinx.coroutines.BuildersKt.launch(
                cs,
                kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
                kotlinx.coroutines.CoroutineStart.DEFAULT,
                (scope, continuation) -> requestRefreshFlow.collect((unit, cont) -> loader.refresh(cont), continuation)
            );
        }
        if (requestChangeFlow != null) {
            kotlinx.coroutines.BuildersKt.launch(
                cs,
                kotlin.coroutines.EmptyCoroutineContext.INSTANCE,
                kotlinx.coroutines.CoroutineStart.DEFAULT,
                (scope, continuation) -> requestChangeFlow.collect(
                    (change, cont) -> {
                        loader.update(change);
                        return kotlin.Unit.INSTANCE;
                    },
                    continuation
                )
            );
        }

        return loader;
    }

    @FunctionalInterface
    public interface GraphQLRequestPerformer<V> {
        @Nullable
        GraphQLConnectionDTO<V> perform(@Nullable String cursor) throws Exception;
    }
}
