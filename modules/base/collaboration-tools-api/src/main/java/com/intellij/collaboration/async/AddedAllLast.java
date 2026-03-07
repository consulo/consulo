// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Objects;

@ApiStatus.Internal
public final class AddedAllLast<V> implements Change<V> {
    private final @Nonnull List<V> values;

    public AddedAllLast(@Nonnull List<V> values) {
        this.values = values;
    }

    public @Nonnull List<V> getValues() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AddedAllLast<?> that)) {
            return false;
        }
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public String toString() {
        return "AddedAllLast(values=" + values + ")";
    }
}
