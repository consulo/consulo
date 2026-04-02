// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.api.json;

import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Reader;

@ApiStatus.Experimental
public interface JsonDataDeserializer {
    /**
     * Parses a value of the given type from the given reader.
     * <p>
     * The reader is not closed by this function. It should be managed by the caller.
     */
    @Nullable
    <T> T fromJson(@Nonnull Reader bodyReader, @Nonnull Class<T> clazz);

    /**
     * Parses a value of type T = L&lt;A, B&gt; from the given reader.
     * Type T is given by {@code clazz}, whereas A and B are given through {@code classArgs}.
     * <p>
     * The reader is not closed by this function. It should be managed by the caller.
     */
    @Nullable
    <T> T fromJson(@Nonnull Reader bodyReader, @Nonnull Class<T> clazz, @Nonnull Class<?>... classArgs);
}
