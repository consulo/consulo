// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api.dto;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public final class GraphQLRequestDTO {
    private final @Nonnull String query;
    private final @Nullable Object variables;

    public GraphQLRequestDTO(@Nonnull String query, @Nullable Object variables) {
        this.query = query;
        this.variables = variables;
    }

    public @Nonnull String getQuery() {
        return query;
    }

    public @Nullable Object getVariables() {
        return variables;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GraphQLRequestDTO that)) {
            return false;
        }
        return Objects.equals(query, that.query) && Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, variables);
    }

    @Override
    public @Nonnull String toString() {
        return "GraphQLRequestDTO(query=" + query + ", variables=" + variables + ")";
    }
}
