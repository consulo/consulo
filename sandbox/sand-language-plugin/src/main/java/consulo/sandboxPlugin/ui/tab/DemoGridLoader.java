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
package consulo.sandboxPlugin.ui.tab;

import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.grid.ErrorInfo;
import consulo.ui.ex.grid.GridLoaderBase;
import consulo.ui.ex.grid.SimpleErrorInfo;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridModelUpdater;
import consulo.ui.grid.GridMutationModel;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridUtilCore;
import consulo.ui.grid.MultiPageModel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Loads a page of {@link SampleGridData} on a background thread after a delay, the way a database would answer, and
 * fills the model on the UI thread, with the values submitted so far. The data source is told about the request on the UI
 * thread only: the grid asks there, and the answer goes back there through the {@link UIAccess} the loader is given.
 *
 * @since 2026-10-03
 */
final class DemoGridLoader extends GridLoaderBase {
    private static final String FAILURE_MESSAGE = "Simulated failure: the sample source refused the request";

    private final DemoGridHookUp myDemoHookUp;
    private final UIAccess myUIAccess;

    DemoGridLoader(DemoGridHookUp hookUp,
                   MultiPageModel<GridRow, GridColumn> pageModel,
                   GridModelUpdater modelUpdater,
                   UIAccess uiAccess) {
        super(hookUp, pageModel, modelUpdater);
        myDemoHookUp = hookUp;
        myUIAccess = uiAccess;
    }

    @Override
    @RequiredUIAccess
    public void load(GridRequestSource source, int offset) {
        myDemoHookUp.notifyRequestStarted(source);

        int start = getStart(offset);
        int count = getRowCount(start);
        long delay = myDemoHookUp.getRequestDelayMillis();
        CompletableFuture.runAsync(() -> {
            try {
                List<GridRow> rows = SampleGridData.fetch(start, count);
                myUIAccess.give(() -> onLoaded(source, start, rows));
            }
            catch (RuntimeException e) {
                myUIAccess.give(() -> onFailed(source, SimpleErrorInfo.create(e)));
            }
        }, CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS));
    }

    /**
     * @return the index of the first row to load, starting from 0
     */
    private int getStart(int offset) {
        long positiveOffset;
        if (getPageModel().getTotalRowCount() > 0) {
            positiveOffset = getPositiveOffset(offset);
        }
        else {
            // nothing is loaded yet, so the page model does not know the total
            positiveOffset = offset < 0 ? SampleGridData.TOTAL + (long) offset : offset;
        }
        return (int) Math.max(0, Math.min(SampleGridData.TOTAL - 1, positiveOffset));
    }

    private int getRowCount(int start) {
        long count;
        if (getPageModel().getTotalRowCount() > 0) {
            count = getCount(start);
        }
        else {
            int pageSize = getPageModel().getPageSize();
            count = GridUtilCore.isPageSizeUnlimited(pageSize) ? SampleGridData.TOTAL : pageSize;
        }
        return (int) Math.max(0, Math.min(SampleGridData.TOTAL - start, count));
    }

    @RequiredUIAccess
    private void onLoaded(GridRequestSource source, int start, List<GridRow> rows) {
        if (myDemoHookUp.isDisposed()) {
            return;
        }

        // the model is not touched before the error - it must not be left updating
        if (myDemoHookUp.consumeFailNext()) {
            onFailed(source, SimpleErrorInfo.create(FAILURE_MESSAGE, null));
            return;
        }

        if (myDemoHookUp.getDataModel().getColumnCount() == 0) {
            getModelUpdater().setColumns(SampleGridData.COLUMNS);
        }

        MultiPageModel<GridRow, GridColumn> pageModel = getPageModel();
        pageModel.setTotalRowCount(SampleGridData.TOTAL, true);
        pageModel.setTotalRowCountUpdateable(false);

        // the sample rows are computed again - the submitted values go back into them, as a database answers with them
        myDemoHookUp.applyCommittedValues(rows);

        loadingStarted(start);
        // with the source of the request: the model keeps the pending changes of a reloaded row which did not change, unless the
        // request drops them (GridRequestSource.isMutatedDataLocally, set once the user agreed to lose them)
        afterLastRowAdded(addRows(rows, 0, source), source);
        if (source.isMutatedDataLocally()) {
            // the model drops the pending changes of a row which did not change without reporting the row - repaint every row
            GridMutationModel mutationModel = myDemoHookUp.getMutationModel();
            mutationModel.notifyCellsUpdated(mutationModel.getRowIndices(), mutationModel.getColumnIndices(), source.place);
        }
        myDemoHookUp.notifyRequestFinished(source, true);
    }

    @RequiredUIAccess
    private void onFailed(GridRequestSource source, ErrorInfo errorInfo) {
        if (myDemoHookUp.isDisposed()) {
            return;
        }
        myDemoHookUp.notifyRequestError(source, errorInfo);
        myDemoHookUp.notifyRequestFinished(source, false);
    }
}
