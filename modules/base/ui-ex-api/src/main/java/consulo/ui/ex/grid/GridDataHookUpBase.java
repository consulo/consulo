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
package consulo.ui.ex.grid;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridFilteringModel;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridSortingModel;
import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * A data source of the grid which is not bound to a project, and has neither a filter nor a sorting language - see
 * {@link GridDataHookUp}.
 * <p/>
 * The {@code notify} methods tell the request listeners at once, on the calling thread, which is the UI thread. A data source
 * which answers a request asynchronously goes back to the UI thread before it reports the answer, with the
 * {@link consulo.ui.UIAccess} given to it by whoever created it there.
 */
public abstract class GridDataHookUpBase<Row, Column> implements GridDataHookUp<Row, Column> {
    private final List<RequestListener<Row, Column>> myRequestListeners = Lists.newLockFreeCopyOnWriteList();

    protected GridDataHookUpBase() {
    }

    @Override
    public void addRequestListener(RequestListener<Row, Column> listener, Disposable disposable) {
        myRequestListeners.add(listener);
        Disposer.register(disposable, () -> myRequestListeners.remove(listener));
    }

    @Override
    public void updateFilterSortFully() {
    }

    @Override
    public boolean isFilterApplicable() {
        return false;
    }

    @Override
    public String getFilterPrefix() {
        return "";
    }

    @Override
    public String getFilterEmptyText() {
        return "";
    }

    @Override
    public @Nullable GridFilteringModel getFilteringModel() {
        return null;
    }

    @Override
    public String getSortingPrefix() {
        return "";
    }

    @Override
    public String getSortingEmptyText() {
        return "";
    }

    @Override
    public @Nullable GridSortingModel<Row, Column> getSortingModel() {
        return null;
    }

    @Override
    public @Nullable GridMutator<Row, Column> getMutator() {
        return null;
    }

    @Override
    public int getBusyCount() {
        return 0;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @RequiredUIAccess
    public void notifyRequestStarted(GridRequestSource source) {
        fire(listener -> listener.requestStarted(source));
    }

    @RequiredUIAccess
    public void notifyRequestFinished(GridRequestSource source, boolean success) {
        source.requestComplete(success);
        fire(listener -> listener.requestFinished(source, success));
    }

    @RequiredUIAccess
    public void notifyRequestProgress(GridRequestSource source, @Nullable String progress) {
        fire(listener -> listener.requestProgress(source, progress));
    }

    @RequiredUIAccess
    public void notifyRequestError(GridRequestSource source, ErrorInfo errorInfo) {
        source.setErrorOccurred(errorInfo.getMessage());
        fire(listener -> listener.error(source, errorInfo));
    }

    @RequiredUIAccess
    public void notifyUpdateCountReceived(GridRequestSource source, int updateCount) {
        fire(listener -> listener.updateCountReceived(source, updateCount));
    }

    @RequiredUIAccess
    public void notifyDropModelDependentCache(GridRequestSource source) {
        fire(listener -> listener.dropModelDependentCache(source));
    }

    @RequiredUIAccess
    private void fire(Consumer<RequestListener<Row, Column>> event) {
        for (RequestListener<Row, Column> listener : myRequestListeners) {
            event.accept(listener);
        }
    }
}
