// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

public final class NamedCollection<T> {
    private final @Nls String namePlural;
    private final List<T> items;

    private NamedCollection(@Nonnull @Nls String namePlural, @Nonnull List<T> items) {
        this.namePlural = namePlural;
        this.items = items;
    }

    public @Nonnull @Nls String getNamePlural() {
        return namePlural;
    }

    public @Nonnull List<T> getItems() {
        return items;
    }

    public static <T> @Nullable NamedCollection<T> create(@Nonnull @Nls String namePlural, @Nonnull List<T> items) {
        if (items.isEmpty()) {
            return null;
        }
        else {
            return new NamedCollection<>(namePlural, items);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NamedCollection<?> that = (NamedCollection<?>) o;
        return Objects.equals(namePlural, that.namePlural) && Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namePlural, items);
    }

    @Override
    public String toString() {
        return "NamedCollection(namePlural=" + namePlural + ", items=" + items + ")";
    }
}
