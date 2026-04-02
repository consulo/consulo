// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.graphql;

import com.intellij.collaboration.api.dto.GraphQLErrorDTO;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import jakarta.annotation.Nonnull;

import java.util.List;

public final class GraphQLErrorException extends RuntimeException {
    private final @Nonnull List<? extends GraphQLErrorDTO> errors;

    public GraphQLErrorException(@Nonnull List<? extends GraphQLErrorDTO> errors) {
        super("GraphQL error: " + errors);
        this.errors = errors;
    }

    public @Nonnull List<? extends GraphQLErrorDTO> getErrors() {
        return errors;
    }

    @Override
    public String getLocalizedMessage() {
        return CollaborationToolsLocalize.graphqlErrors(errors.toString()).get();
    }
}
