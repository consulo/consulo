// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.toolwindow;

import consulo.localize.LocalizeValue;
import consulo.review.list.ReviewListViewModel;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * Represent a view model of a review toolwindow with selected project (for GitHub it is a repository).
 * <p>
 * VM is mostly used for UI creation to create the review list component or other tabs.
 *
 * @param <T>   tab type
 * @param <TVM> tab view model
 */
public interface ReviewToolwindowProjectViewModel<T extends ReviewTab, TVM extends ReviewTabViewModel> {
    /**
     * Presentable name for the project which context is hold here.
     * Used in toolwindow UI places like review list tab title.
     */
    LocalizeValue getProjectName();

    /**
     * ViewModel of the review list view.
     */
    ReviewListViewModel getListVm();

    /**
     * Refresh the toolwindow of the currently opened tab
     */
    default void refresh() {
        getListVm().refresh();
    }

    /**
     * State of displayed review tabs besides the list
     */
    ObservableValue<ReviewToolwindowTabs<T, TVM>> getTabs();

    /**
     * Pass a {@code tab} to select certain review tab or null to select list tab
     */
    void selectTab(@Nullable T tab);

    void closeTab(T tab);
}
