// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.util;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CollectionDelta<T> {
    private final @Nonnull Collection<? extends T> newCollection;
    private final @Nonnull Collection<? extends T> newItems;
    private final @Nonnull Collection<? extends T> removedItems;
    private final boolean empty;

    public CollectionDelta(@Nonnull Collection<? extends T> oldCollection, @Nonnull Collection<? extends T> newCollection) {
        this.newCollection = newCollection;

        Set<T> oldSet = oldCollection instanceof Set ? (Set<T>) oldCollection : new LinkedHashSet<>(oldCollection);
        Set<T> newSet = newCollection instanceof Set ? (Set<T>) newCollection : new LinkedHashSet<>(newCollection);

        LinkedHashSet<T> added = new LinkedHashSet<>(newCollection);
        added.removeAll(oldSet);
        this.newItems = added;

        LinkedHashSet<T> removed = new LinkedHashSet<>(oldCollection);
        removed.removeAll(newSet);
        this.removedItems = removed;

        this.empty = newItems.isEmpty() && removedItems.isEmpty();
    }

    public @Nonnull Collection<? extends T> getNewCollection() {
        return newCollection;
    }

    public @Nonnull Collection<? extends T> getNewItems() {
        return newItems;
    }

    public @Nonnull Collection<? extends T> getRemovedItems() {
        return removedItems;
    }

    public boolean isEmpty() {
        return empty;
    }
}
