// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import jakarta.annotation.Nonnull;

import java.util.Objects;

@ApiStatus.Internal
public final class AddedFirst<V> implements Change<V> {
    private final @Nonnull V value;

    public AddedFirst(@Nonnull V value) {
        this.value = value;
    }

    public @Nonnull V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AddedFirst<?> that)) {
            return false;
        }
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "AddedFirst(value=" + value + ")";
    }
}
