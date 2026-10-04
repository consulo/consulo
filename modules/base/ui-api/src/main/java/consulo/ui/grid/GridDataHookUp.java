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
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.EventListener;

/**
 * Data source of a grid.
 * <p/>
 * It knows neither the project nor the language of the filter and sorting text - neither is visible from
 * {@code consulo.ui.api}. The error a request reports is a {@link ThrowableInfo}; an error which offers fixes is an
 * {@code consulo.ui.ex.grid.ErrorInfo}.
 * <p/>
 * The grid sends its requests on the UI thread, and a data source reports them to its {@link RequestListener}s on the UI thread
 * too: a source which answers asynchronously goes back there before it reports, with the {@link consulo.ui.UIAccess} given to it
 * by whoever created it there.
 */
public interface GridDataHookUp<Row, Column> extends MutationSupport<Row, Column> {

    GridPagingModel<Row, Column> getPageModel();

    void updateFilterSortFully();

    boolean isFilterApplicable();

    String getFilterPrefix();

    String getFilterEmptyText();

    @Nullable
    GridFilteringModel getFilteringModel();

    String getSortingPrefix();

    String getSortingEmptyText();

    @Nullable
    GridSortingModel<Row, Column> getSortingModel();

    @Nullable
    GridMutator<Row, Column> getMutator();

    GridLoader getLoader();

    int getBusyCount();

    boolean isReadOnly();

    default boolean isForSingleSource() {
        return true;
    }

    void addRequestListener(RequestListener<Row, Column> listener, Disposable disposable);

    /**
     * Every event is fired on the UI thread.
     */
    interface RequestListener<Row, Column> extends EventListener {

        @RequiredUIAccess
        void error(GridRequestSource source, ThrowableInfo errorInfo);

        @RequiredUIAccess
        void updateCountReceived(GridRequestSource source, int updateCount);

        @RequiredUIAccess
        default void dropModelDependentCache(GridRequestSource source) {
        }

        //todo: implemented only for scripted hookup
        @RequiredUIAccess
        default void requestStarted(GridRequestSource source) {
        }

        @RequiredUIAccess
        default void requestProgress(GridRequestSource source, @Nullable String progress) {
        }

        @RequiredUIAccess
        void requestFinished(GridRequestSource source, boolean success);
    }
}
