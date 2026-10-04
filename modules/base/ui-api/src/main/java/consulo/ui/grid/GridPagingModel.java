// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public interface GridPagingModel<Row, Column> {
    int UNLIMITED_PAGE_SIZE = -1;
    int UNSET_PAGE_SIZE = -2;

    boolean isFirstPage();

    boolean isLastPage();

    void setPageSize(int pageSize);

    int getPageSize();

    long getTotalRowCount();

    boolean isTotalRowCountPrecise();

    boolean isTotalRowCountUpdateable();

    int getPageStart();

    int getPageEnd();

    ModelIndex<Row> findRow(int rowDataIdx);

    boolean pageSizeSet();
}
