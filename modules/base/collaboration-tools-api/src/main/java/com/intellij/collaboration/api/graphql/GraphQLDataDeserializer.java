// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.graphql;

import com.intellij.collaboration.api.dto.GraphQLErrorDTO;
import com.intellij.collaboration.api.dto.GraphQLResponseDTO;
import jakarta.annotation.Nonnull;

import java.io.Reader;

@ApiStatus.Experimental
public interface GraphQLDataDeserializer {
    /**
     * The reader is not closed by this function. It should be managed by the caller.
     */
    @Nonnull
    <T> GraphQLResponseDTO<T, GraphQLErrorDTO> readAndMapGQLResponse(
        @Nonnull Reader bodyReader,
        @Nonnull String[] pathFromData,
        @Nonnull Class<T> clazz
    );
}
