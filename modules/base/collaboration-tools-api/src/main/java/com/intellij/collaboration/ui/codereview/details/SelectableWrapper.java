// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import java.util.Objects;

public final class SelectableWrapper<T> {
    private final T value;
    private boolean isSelected;

    public SelectableWrapper(T value, boolean isSelected) {
        this.value = value;
        this.isSelected = isSelected;
    }

    public SelectableWrapper(T value) {
        this(value, false);
    }

    public T getValue() {
        return value;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SelectableWrapper<?> that = (SelectableWrapper<?>) o;
        return isSelected == that.isSelected && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, isSelected);
    }

    @Override
    public String toString() {
        return "SelectableWrapper(value=" + value + ", isSelected=" + isSelected + ')';
    }
}
