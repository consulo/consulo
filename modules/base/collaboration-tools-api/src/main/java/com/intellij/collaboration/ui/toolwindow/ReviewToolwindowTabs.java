// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.toolwindow;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * Represents the state of review toolwindow tabs
 *
 * @param <T>   tab type
 * @param <TVM> tab view model
 */
public final class ReviewToolwindowTabs<T extends ReviewTab, TVM extends ReviewTabViewModel> {
    private final @Nonnull Map<T, TVM> tabs;
    private final @Nullable T selectedTab;

    public ReviewToolwindowTabs(@Nonnull Map<T, TVM> tabs, @Nullable T selectedTab) {
        this.tabs = tabs;
        this.selectedTab = selectedTab;
    }

    public @Nonnull Map<T, TVM> getTabs() {
        return tabs;
    }

    public @Nullable T getSelectedTab() {
        return selectedTab;
    }

    public @Nonnull ReviewToolwindowTabs<T, TVM> copy(@Nonnull Map<T, TVM> tabs, @Nullable T selectedTab) {
        return new ReviewToolwindowTabs<>(tabs, selectedTab);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReviewToolwindowTabs<?, ?> that)) {
            return false;
        }
        return Objects.equals(tabs, that.tabs) && Objects.equals(selectedTab, that.selectedTab);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tabs, selectedTab);
    }

    @Override
    public String toString() {
        return "ReviewToolwindowTabs(tabs=" + tabs + ", selectedTab=" + selectedTab + ")";
    }
}
