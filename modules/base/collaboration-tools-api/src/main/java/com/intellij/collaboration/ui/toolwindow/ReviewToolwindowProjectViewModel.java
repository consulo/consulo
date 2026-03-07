// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.toolwindow;

import com.intellij.collaboration.ui.codereview.list.ReviewListViewModel;
import kotlinx.coroutines.flow.StateFlow;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Represent a view model of a review toolwindow with selected project (for GitHub it is a repository).
 * <p>
 * VM is mostly used for UI creation in {@link ReviewTabsComponentFactory}
 * to create the review list component or other tabs.
 *
 * @param <T>   tab type
 * @param <TVM> tab view model
 */
public interface ReviewToolwindowProjectViewModel<T extends ReviewTab, TVM extends ReviewTabViewModel> {
    /**
     * Presentable name for the project which context is hold here.
     * Used in toolwindow UI places like review list tab title.
     */
    @Nls
    @Nonnull
    String getProjectName();

    /**
     * ViewModel of the review list view.
     */
    @Nonnull
    ReviewListViewModel getListVm();

    /**
     * Refresh the toolwindow of the currently opened tab
     */
    @ApiStatus.Internal
    default void refresh() {
        getListVm().refresh();
    }

    /**
     * State of displayed review tabs besides the list
     */
    @Nonnull
    StateFlow<ReviewToolwindowTabs<T, TVM>> getTabs();

    /**
     * Pass a {@code tab} to select certain review tab or null to select list tab
     */
    void selectTab(@Nullable T tab);

    void closeTab(@Nonnull T tab);
}
