// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import org.jetbrains.annotations.ApiStatus;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.UnaryOperator;

@ApiStatus.Internal
public final class Updated<V> implements Change<V> {
    private final @Nonnull UnaryOperator<V> updater;

    public Updated(@Nonnull UnaryOperator<V> updater) {
        this.updater = updater;
    }

    public @Nonnull UnaryOperator<V> getUpdater() {
        return updater;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Updated<?> that)) {
            return false;
        }
        return updater.equals(that.updater);
    }

    @Override
    public int hashCode() {
        return Objects.hash(updater);
    }

    @Override
    public String toString() {
        return "Updated(updater=" + updater + ")";
    }
}
