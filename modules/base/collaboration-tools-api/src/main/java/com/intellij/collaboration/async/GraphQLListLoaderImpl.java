// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import com.intellij.collaboration.api.dto.GraphQLConnectionDTO;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class GraphQLListLoaderImpl<K, V>
    extends PaginatedPotentiallyInfiniteListLoader<GraphQLListLoaderImpl.GraphQLPageInfo, K, V> {

    private final @Nonnull GraphQLListLoader.GraphQLRequestPerformer<V> performRequest;

    GraphQLListLoaderImpl(
        @Nonnull Function<V, K> extractKey,
        boolean shouldTryToLoadAll,
        @Nonnull GraphQLListLoader.GraphQLRequestPerformer<V> performRequest
    ) {
        super(new GraphQLPageInfo(null, null), extractKey, shouldTryToLoadAll);
        this.performRequest = performRequest;
    }

    @Override
    protected @Nullable Page<GraphQLPageInfo, V> performRequestAndProcess(
        @Nonnull GraphQLPageInfo pageInfo,
        @Nonnull PageCreator<GraphQLPageInfo, V> createPage
    ) throws Exception {
        GraphQLConnectionDTO<V> results = performRequest.perform(pageInfo.cursor);
        String nextCursor = results != null
            ? results.getPageInfo().getEndCursor()
            : null;

        GraphQLPageInfo updatedInfo = new GraphQLPageInfo(pageInfo.cursor, nextCursor);
        List<V> nodes = results != null ? results.getNodes() : null;
        return createPage.create(updatedInfo, nodes);
    }

    static final class GraphQLPageInfo implements PaginatedPotentiallyInfiniteListLoader.PageInfo<GraphQLPageInfo> {
        final @Nullable String cursor;
        final @Nullable String nextCursor;

        GraphQLPageInfo(@Nullable String cursor, @Nullable String nextCursor) {
            this.cursor = cursor;
            this.nextCursor = nextCursor;
        }

        @Override
        public @Nullable GraphQLPageInfo createNextPageInfo() {
            return nextCursor != null ? new GraphQLPageInfo(nextCursor, null) : null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GraphQLPageInfo that)) {
                return false;
            }
            return Objects.equals(cursor, that.cursor) && Objects.equals(nextCursor, that.nextCursor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cursor, nextCursor);
        }

        @Override
        public String toString() {
            return "GraphQLPageInfo(cursor=" + cursor + ", nextCursor=" + nextCursor + ")";
        }
    }
}
