// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.ui.annotation.RequiredUIAccess;

/**
 * The paging of a {@link GridDataHookUp}. The grid asks on the UI thread; the answer is reported on the UI thread too, through
 * the request listeners of the data source.
 */
public interface GridLoader {

    @RequiredUIAccess
    void reloadCurrentPage(GridRequestSource source);

    @RequiredUIAccess
    void loadNextPage(GridRequestSource source);

    @RequiredUIAccess
    void loadPreviousPage(GridRequestSource source);

    @RequiredUIAccess
    void loadLastPage(GridRequestSource source);

    @RequiredUIAccess
    void loadFirstPage(GridRequestSource source);

    @RequiredUIAccess
    void load(GridRequestSource source, int offset);

    @RequiredUIAccess
    void updateTotalRowCount(GridRequestSource source);

    @RequiredUIAccess
    void applyFilterAndSorting(GridRequestSource source);

    void updateIsTotalRowCountUpdateable();

    /**
     * Consulo: whether {@link #reloadCurrentPage} brings anything the grid does not show already - a query run again, for one. A
     * loader of data the grid always shows as it is, such as a parsed document which is parsed again on every change, has nothing
     * to reload, and the grid offers no Reload for it.
     */
    default boolean isReloadable() {
        return true;
    }
}
