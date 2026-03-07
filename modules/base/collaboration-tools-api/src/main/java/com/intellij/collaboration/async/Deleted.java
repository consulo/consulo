// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

@ApiStatus.Internal
public non-sealed class Deleted<V> implements Change<V> {
    private final @Nonnull Predicate<V> isDeleted;

    public Deleted(@Nonnull Predicate<V> isDeleted) {
        this.isDeleted = isDeleted;
    }

    public @Nonnull Predicate<V> getIsDeleted() {
        return isDeleted;
    }
}
