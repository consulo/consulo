// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api.dto;

import com.intellij.collaboration.api.graphql.GraphQLErrorException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * <a href="https://spec.graphql.org/June2018/#sec-Response">specification</a>
 */
public final class GraphQLResponseDTO<D, E extends GraphQLErrorDTO> {
    private final @Nullable D data;
    private final @Nullable List<E> errors;

    public GraphQLResponseDTO(@Nullable D data, @Nullable List<E> errors) {
        this.data = data;
        this.errors = errors;
    }

    public @Nullable D getData() {
        return data;
    }

    public @Nullable List<E> getErrors() {
        return errors;
    }

    /**
     * Returns the data if present, throws {@link GraphQLErrorException} if errors are present, or returns null.
     */
    @Nullable
    public static <D> D getOrThrow(@Nonnull GraphQLResponseDTO<D, ?> response) {
        if (response.getData() != null) {
            return response.getData();
        }
        if (response.getErrors() != null) {
            throw new GraphQLErrorException(response.getErrors());
        }
        return null;
    }
}
