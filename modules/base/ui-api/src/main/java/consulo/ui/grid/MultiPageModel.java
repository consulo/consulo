// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import java.util.EventListener;

public interface MultiPageModel<Row, Column> extends GridPagingModel<Row, Column> {
    void setPageStart(int pageStart);

    void setPageEnd(int pageEnd);

    void setTotalRowCount(long totalRowCount, boolean precise);

    void setTotalRowCountUpdateable(boolean updateable);

    void addPageModelListener(PageModelListener listener);

    default int getFirstRowIndex() {
        return 1;
    }

    interface PageModelListener extends EventListener {
        void pageSizeChanged();

        void pageStartChanged();
    }
}
