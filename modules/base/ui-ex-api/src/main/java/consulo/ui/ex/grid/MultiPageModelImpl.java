// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.ex.grid;

import consulo.ui.grid.GridModel;
import consulo.ui.grid.GridPagingModelImpl;
import consulo.ui.grid.MultiPageModel;
import consulo.util.collection.Lists;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MultiPageModelImpl<Row, Column> extends GridPagingModelImpl<Row, Column> implements MultiPageModel<Row, Column> {
    private final @Nullable DataGridSettings mySettings;
    private final List<PageModelListener> myPageModelListeners = Lists.newLockFreeCopyOnWriteList();
    private int myPageSize = UNSET_PAGE_SIZE;
    private int myPageStart = 1;
    private int myPageEnd;
    private long myTotalRowCount;
    private boolean myTotalRowCountIsPrecise;
    private boolean myTotalRowCountUpdateable;

    public MultiPageModelImpl(GridModel<Row, Column> model, @Nullable DataGridSettings settings) {
        super(model);
        mySettings = settings;
        myPageStart = getFirstRowIndex();
    }

    @Override
    public boolean isFirstPage() {
        return myPageStart == getFirstRowIndex();
    }

    @Override
    public boolean isLastPage() {
        return myPageEnd == -1 || myPageEnd + (1 - getFirstRowIndex()) >= myTotalRowCount && myTotalRowCountIsPrecise;
    }

    @Override
    public void setPageSize(int pageSize) {
        myPageSize = pageSize;
        for (PageModelListener listener : myPageModelListeners) {
            listener.pageSizeChanged();
        }
    }

    @Override
    public int getPageSize() {
        return myPageSize == UNSET_PAGE_SIZE ? DataGridSettings.getPageSize(mySettings) : myPageSize;
    }

    @Override
    public boolean pageSizeSet() {
        return myPageSize != UNSET_PAGE_SIZE;
    }

    @Override
    public long getTotalRowCount() {
        return myTotalRowCount;
    }

    @Override
    public boolean isTotalRowCountPrecise() {
        return myTotalRowCountIsPrecise;
    }

    @Override
    public boolean isTotalRowCountUpdateable() {
        return myTotalRowCountUpdateable;
    }

    @Override
    public int getPageStart() {
        return myPageStart;
    }

    @Override
    public int getPageEnd() {
        return myPageEnd;
    }

    @Override
    public void setPageStart(int pageStart) {
        myPageStart = pageStart;
        for (PageModelListener listener : myPageModelListeners) {
            listener.pageStartChanged();
        }
    }

    @Override
    public void setPageEnd(int pageEnd) {
        myPageEnd = pageEnd;
    }

    @Override
    public void setTotalRowCount(long totalRowCount, boolean precise) {
        myTotalRowCount = totalRowCount;
        myTotalRowCountIsPrecise = precise;
    }

    @Override
    public void setTotalRowCountUpdateable(boolean updateable) {
        myTotalRowCountUpdateable = updateable;
    }

    @Override
    public void addPageModelListener(PageModelListener listener) {
        myPageModelListeners.add(listener);
    }

    @Override
    public int getFirstRowIndex() {
        return mySettings == null ? 1 : mySettings.getFirstRowIndex();
    }
}
