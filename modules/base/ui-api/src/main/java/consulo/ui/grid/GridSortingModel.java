// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.grid;

import consulo.disposer.Disposable;

import java.util.EventListener;
import java.util.List;

/**
 * The sorting of a data source: the current and the applied ordering of the columns, and the history of the sorting text. The
 * model holds no editor document - documents are not visible from {@code consulo.ui.api}; a data source which edits its sorting
 * as text adds the document on its own model.
 */
public interface GridSortingModel<Row, Column> {

    boolean isSortingEnabled();

    default boolean replacesClientSort() {
        return false;
    }

    void setSortingEnabled(boolean enabled);

    List<RowSortOrder<ModelIndex<Column>>> getOrdering();

    void setOrdering(List<RowSortOrder<ModelIndex<Column>>> ordering);

    List<RowSortOrder<ModelIndex<Column>>> getAppliedOrdering();

    String getAppliedSortingText();

    void apply();

    List<String> getHistory();

    void setHistory(List<String> history);

    void addListener(Listener l, Disposable disposable);

    boolean supportsAdditiveSorting();

    interface Listener extends EventListener {
        default void orderingChanged() {
        }

        default void onPsiUpdated() {
        }

        default void onPrefixUpdated() {
        }
    }
}
