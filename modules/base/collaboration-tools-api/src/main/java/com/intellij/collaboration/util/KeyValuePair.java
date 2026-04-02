// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.util;

import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

@ApiStatus.Experimental
public final class KeyValuePair<T> {
    private final @Nonnull Key<T> key;
    private final @Nullable T value;

    public KeyValuePair(@Nonnull Key<T> key, @Nullable T value) {
        this.key = key;
        this.value = value;
    }

    public @Nonnull Key<T> getKey() {
        return key;
    }

    public @Nullable T getValue() {
        return value;
    }

    public static <T> void putData(@Nonnull UserDataHolder holder, @Nonnull KeyValuePair<T> keyValue) {
        holder.putUserData(keyValue.getKey(), keyValue.getValue());
    }

    public static void clearData(@Nonnull UserDataHolder holder, @Nonnull KeyValuePair<?> keyValue) {
        holder.putUserData(keyValue.getKey(), null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyValuePair<?> that)) {
            return false;
        }
        return key.equals(that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "KeyValuePair(key=" + key + ", value=" + value + ")";
    }
}
