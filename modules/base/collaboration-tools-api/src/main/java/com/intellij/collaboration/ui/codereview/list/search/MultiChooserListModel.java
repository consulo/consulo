// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list.search;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A special list model which allows choosing multiple items from the list.
 * Ensures items are not added to the list twice.
 */
final class MultiChooserListModel<T> extends AbstractListModel<T> {
    private final Set<T> chosenItems = new LinkedHashSet<>();
    private final Set<T> itemsSet = new LinkedHashSet<>();
    private final List<T> items = new ArrayList<>();

    @Override
    public int getSize() {
        return items.size();
    }

    @Override
    public @Nullable T getElementAt(int index) {
        if (index < 0 || index >= items.size()) {
            return null;
        }
        return items.get(index);
    }

    boolean isChosen(@Nonnull T value) {
        return chosenItems.contains(value);
    }

    @Nonnull
    List<T> getChosenItems() {
        return items.stream().filter(chosenItems::contains).collect(Collectors.toList());
    }

    void setChosen(@Nonnull Collection<T> toChoose) {
        chosenItems.clear();
        for (T item : toChoose) {
            int idx = items.indexOf(item);
            if (idx >= 0) {
                chosenItems.add(item);
                fireContentsChanged(this, idx, idx);
            }
        }
    }

    void toggleChosen(int idx) {
        if (idx >= 0) {
            T item = getElementAt(idx);
            if (item == null) {
                return;
            }
            if (chosenItems.contains(item)) {
                chosenItems.remove(item);
            }
            else {
                chosenItems.add(item);
            }
            fireContentsChanged(this, idx, idx);
        }
    }

    void add(@Nonnull List<T> newList) {
        int lastIndex = items.size() - 1;
        int count = 0;
        for (T item : newList) {
            if (itemsSet.add(item)) {
                items.add(item);
                count++;
            }
        }
        if (count != 0) {
            fireIntervalAdded(this, 0, lastIndex + count);
        }
    }

    /**
     * Updates the list of items, ensuring that chosen items stay in the list.
     */
    void retainChosenAndUpdate(@Nonnull List<T> newList) {
        int oldSize = items.size();

        items.clear();
        itemsSet.clear();

        for (T chosen : chosenItems) {
            items.add(chosen);
            itemsSet.add(chosen);
        }

        for (T item : newList) {
            if (itemsSet.add(item)) {
                items.add(item);
            }
        }

        int newSize = items.size();
        if (oldSize == 0 && newSize > 0) {
            fireIntervalAdded(this, 0, newSize - 1);
        }
        else if (newSize == 0 && oldSize > 0) {
            fireIntervalRemoved(this, 0, oldSize - 1);
        }
        else if (oldSize != newSize) {
            int minSize = Math.min(oldSize, newSize);
            if (minSize > 0) {
                fireContentsChanged(this, 0, minSize - 1);
            }
            if (newSize > oldSize) {
                fireIntervalAdded(this, oldSize, newSize - 1);
            }
            else {
                fireIntervalRemoved(this, newSize, oldSize - 1);
            }
        }
        else {
            fireContentsChanged(this, 0, newSize - 1);
        }
    }

    void removeAllExceptChosen() {
        int oldSize = items.size();
        items.clear();
        itemsSet.clear();
        if (oldSize != 0) {
            fireIntervalRemoved(this, 0, oldSize - 1);
        }

        for (T chosen : chosenItems) {
            items.add(chosen);
            itemsSet.add(chosen);
        }
        if (!items.isEmpty()) {
            fireIntervalAdded(this, 0, items.size() - 1);
        }
    }
}
