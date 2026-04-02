// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.data;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class GraphQLRequestPagination {
    private static final int DEFAULT_PAGE_SIZE = 100;

    public static final @Nonnull GraphQLRequestPagination DEFAULT = new GraphQLRequestPagination((String) null);

    private final @Nullable String afterCursor;
    private final @Nullable Date since;
    private final int pageSize;

    private GraphQLRequestPagination(@Nullable String afterCursor, @Nullable Date since, int pageSize) {
        this.afterCursor = afterCursor;
        this.since = since;
        this.pageSize = pageSize;
    }

    public GraphQLRequestPagination(@Nullable String afterCursor) {
        this(afterCursor, null, DEFAULT_PAGE_SIZE);
    }

    public GraphQLRequestPagination(@Nullable String afterCursor, int pageSize) {
        this(afterCursor, null, pageSize);
    }

    public GraphQLRequestPagination(@Nullable Date since) {
        this(null, since, DEFAULT_PAGE_SIZE);
    }

    public GraphQLRequestPagination(@Nullable Date since, int pageSize) {
        this(null, since, pageSize);
    }

    public @Nullable String getAfterCursor() {
        return afterCursor;
    }

    public @Nullable Date getSince() {
        return since;
    }

    public int getPageSize() {
        return pageSize;
    }

    @Override
    public @Nonnull String toString() {
        return "afterCursor=" + afterCursor + "&since=" + since + "&pageSize=" + pageSize;
    }

    public static @Nonnull Map<String, @Nullable Object> asParameters(@Nonnull GraphQLRequestPagination pagination) {
        Map<String, Object> map = new HashMap<>();
        map.put("pageSize", pagination.pageSize);
        map.put("cursor", pagination.afterCursor);
        return map;
    }

    public static @Nonnull GraphQLRequestPagination orDefault(@Nullable GraphQLRequestPagination pagination) {
        return pagination != null ? pagination : DEFAULT;
    }
}
