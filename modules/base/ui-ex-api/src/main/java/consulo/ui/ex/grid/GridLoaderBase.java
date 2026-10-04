// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridListModelBase;
import consulo.ui.grid.GridLoader;
import consulo.ui.grid.GridModelUpdater;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridUtilCore;
import consulo.ui.grid.MultiPageModel;

import java.util.List;

/**
 * The paging of a {@link MultiPageModel}: every page request is a {@link #load} from an offset. The grid asks on the UI thread; a
 * loader which reads the page asynchronously fills the model and reports the answer back on the UI thread - see
 * {@link GridDataHookUpBase}.
 */
public abstract class GridLoaderBase implements GridLoader {
    protected final GridDataHookUpBase<GridRow, GridColumn> myHookUp;
    protected final MultiPageModel<GridRow, GridColumn> myPageModel;
    protected final GridModelUpdater myModelUpdater;

    protected GridLoaderBase(GridDataHookUpBase<GridRow, GridColumn> hookUp,
                             MultiPageModel<GridRow, GridColumn> pageModel,
                             GridModelUpdater modelUpdater) {
        myHookUp = hookUp;
        myPageModel = pageModel;
        myModelUpdater = modelUpdater;
    }

    protected GridDataHookUpBase<GridRow, GridColumn> getHookUp() {
        return myHookUp;
    }

    protected MultiPageModel<GridRow, GridColumn> getPageModel() {
        return myPageModel;
    }

    protected GridModelUpdater getModelUpdater() {
        return myModelUpdater;
    }

    @Override
    @RequiredUIAccess
    public void reloadCurrentPage(GridRequestSource source) {
        load(source, Math.max(0, myPageModel.getPageStart() - 1));
    }

    @Override
    @RequiredUIAccess
    public void loadNextPage(GridRequestSource source) {
        load(source, myPageModel.getPageEnd());
    }

    @Override
    @RequiredUIAccess
    public void loadPreviousPage(GridRequestSource source) {
        load(source, Math.max(0, myPageModel.getPageStart() - myPageModel.getPageSize() - 1));
    }

    @Override
    @RequiredUIAccess
    public void loadFirstPage(GridRequestSource source) {
        load(source, 0);
    }

    @Override
    @RequiredUIAccess
    public void updateTotalRowCount(GridRequestSource source) {
        myHookUp.notifyRequestFinished(source, false);
    }

    @Override
    @RequiredUIAccess
    public void applyFilterAndSorting(GridRequestSource source) {
        myHookUp.notifyRequestFinished(source, false);
    }

    @Override
    public void updateIsTotalRowCountUpdateable() {
    }

    @Override
    @RequiredUIAccess
    public void loadLastPage(GridRequestSource source) {
        int pageSize = myPageModel.getPageSize();
        load(source, -(pageSize > 0 ? pageSize : 100));
    }

    protected long getPositiveOffset(int offset) {
        return offset < 0 ? myPageModel.getTotalRowCount() + offset : offset;
    }

    protected long getCount(long positiveOffset) {
        return Math.min(myPageModel.getTotalRowCount(),
            GridUtilCore.isPageSizeUnlimited(myPageModel.getPageSize())
                ? myPageModel.getTotalRowCount() - positiveOffset
                : myPageModel.getPageSize());
    }

    protected void afterLastRowAdded(int rowsLoaded, GridRequestSource source) {
        int rowCount = myHookUp.getDataModel().getRowCount();
        if (rowsLoaded >= 0 && rowsLoaded < rowCount) {
            int rowsToRemove = rowCount - rowsLoaded;
            myModelUpdater.removeRows(rowCount - rowsToRemove, rowsToRemove);
        }
        myModelUpdater.afterLastRowAdded();
        source.requestComplete(true);
    }

    protected int addRows(List<GridRow> rows, int rowsLoaded) {
        return addRows(rows, rowsLoaded, new GridRequestSource(null));
    }

    /**
     * {@link #addRows(List, int)} with the source of the load request, so the model drops the pending changes of the reloaded rows
     * when the request asks for it ({@link GridRequestSource#isMutatedDataLocally()}, set once the user agreed to lose them). The
     * overload without a source passes a new one, which keeps them.
     */
    protected int addRows(List<GridRow> rows, int rowsLoaded, GridRequestSource source) {
        if (rows.isEmpty()) {
            return 0;
        }
        if (rowsLoaded == 0) {
            myPageModel.setPageStart(rows.get(0).getRowNum());
        }
        myPageModel.setPageEnd(rows.get(rows.size() - 1).getRowNum());
        myModelUpdater.setRows(rowsLoaded, rows, source);
        return rows.size();
    }

    protected void loadingStarted(int offset) {
        if (myHookUp.getDataModel() instanceof GridListModelBase<?, ?> listModel) {
            listModel.setUpdatingNow(true);
        }
        myPageModel.setPageStart(offset + myPageModel.getFirstRowIndex());
        myPageModel.setPageEnd(offset);
    }
}
