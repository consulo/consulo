// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.dto;

import jakarta.annotation.Nonnull;

import java.util.List;

public class GraphQLConnectionDTO<T> extends GraphQLNodesDTO<T> implements GraphQLPagedResponseDataDTO<T> {
    private final @Nonnull GraphQLCursorPageInfoDTO pageInfo;

    public GraphQLConnectionDTO(@Nonnull GraphQLCursorPageInfoDTO pageInfo, @Nonnull List<T> nodes) {
        super(nodes);
        this.pageInfo = pageInfo;
    }

    @Override
    @Nonnull
    public GraphQLCursorPageInfoDTO getPageInfo() {
        return pageInfo;
    }
}
