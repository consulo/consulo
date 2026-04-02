// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api.dto;

import jakarta.annotation.Nonnull;

/**
 * <a href="https://spec.graphql.org/June2018/#sec-Errors">specification</a>
 */
public class GraphQLErrorDTO {
    private final @Nonnull String message;

    public GraphQLErrorDTO(@Nonnull String message) {
        this.message = message;
    }

    public @Nonnull String getMessage() {
        return message;
    }

    @Override
    public @Nonnull String toString() {
        return message;
    }
}
