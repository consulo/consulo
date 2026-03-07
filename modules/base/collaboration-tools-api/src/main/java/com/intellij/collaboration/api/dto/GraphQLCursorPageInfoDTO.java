// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api.dto;

import jakarta.annotation.Nullable;

@GraphQLFragment(filePath = "/graphql/fragment/pageInfo.graphql")
public final class GraphQLCursorPageInfoDTO {
    private final @Nullable String startCursor;
    private final boolean hasPreviousPage;
    private final @Nullable String endCursor;
    private final boolean hasNextPage;

    public GraphQLCursorPageInfoDTO(
        @Nullable String startCursor,
        boolean hasPreviousPage,
        @Nullable String endCursor,
        boolean hasNextPage
    ) {
        this.startCursor = startCursor;
        this.hasPreviousPage = hasPreviousPage;
        this.endCursor = endCursor;
        this.hasNextPage = hasNextPage;
    }

    public @Nullable String getStartCursor() {
        return startCursor;
    }

    public boolean getHasPreviousPage() {
        return hasPreviousPage;
    }

    public @Nullable String getEndCursor() {
        return endCursor;
    }

    public boolean getHasNextPage() {
        return hasNextPage;
    }
}
