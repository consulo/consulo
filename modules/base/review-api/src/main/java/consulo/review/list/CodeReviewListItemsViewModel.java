/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.review.list;

import consulo.disposer.Disposable;
import consulo.review.list.search.ReviewListSearchPanelViewModel;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The review list with its items, so the platform can show it.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface CodeReviewListItemsViewModel<T> extends ReviewListViewModel {
    ObservableValue<ComputedResult<List<T>>> getItems();

    ReviewListItemPresentation getPresentation(T item);

    /**
     * Opens the review - usually a details tab of the review tool window.
     */
    void open(T item);

    /**
     * Loads the next page of the list - called while the list is scrolled close to its end.
     */
    default void requestMore() {
    }

    /**
     * Whether a page of the list is being loaded, shown as a progress over the top of the list.
     */
    default ObservableValue<Boolean> isLoading() {
        return ObservableValue.of(false);
    }

    /**
     * Told when the list should take the focus.
     */
    default void addFocusRequestListener(Disposable parentDisposable, Runnable listener) {
    }

    /**
     * The search filtering the list, or {@code null} for none. An empty list filtered by it offers to clear the
     * filters.
     */
    default @Nullable ReviewListSearchPanelViewModel<?, ?> getSearchVm() {
        return null;
    }
}
