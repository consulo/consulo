// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.dto;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Objects;

public class GraphQLNodesDTO<T> {
    private final @Nonnull List<T> nodes;

    public GraphQLNodesDTO() {
        this(List.of());
    }

    public GraphQLNodesDTO(@Nonnull List<T> nodes) {
        this.nodes = nodes;
    }

    public @Nonnull List<T> getNodes() {
        return nodes;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GraphQLNodesDTO<?> that)) {
            return false;
        }
        return Objects.equals(nodes, that.nodes);
    }

    @Override
    public int hashCode() {
        return nodes.hashCode();
    }
}
