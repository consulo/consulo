// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import jakarta.annotation.Nonnull;

import java.util.List;

@ApiStatus.Internal
public sealed interface ComputedListChange<V> permits ComputedListChange.Remove, ComputedListChange.Insert {
    record Remove(int atIndex, int length) implements ComputedListChange<Object> {
    }

    record Insert<V>(@Nonnull int atIndex, @Nonnull List<V> values) implements ComputedListChange<V> {
    }
}
