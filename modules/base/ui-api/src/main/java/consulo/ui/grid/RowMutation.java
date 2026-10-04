// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class RowMutation implements DatabaseMutation {
    private final GridRow myRow;
    private final List<ColumnQueryData> myData;

    public RowMutation(GridRow row, List<ColumnQueryData> data) {
        myRow = row;
        myData = ContainerUtil.sorted(data);
    }

    public RowMutation merge(@Nullable RowMutation mutation) {
        return mutation == null ? this : new RowMutation(myRow, ContainerUtil.concat(myData, mutation.myData));
    }

    public List<ColumnQueryData> getData() {
        return myData;
    }

    public GridRow getRow() {
        return myRow;
    }

    @Override
    public int compareTo(DatabaseMutation o) {
        return o instanceof RowMutation ? Integer.compare(myRow.getRowNum(), ((RowMutation) o).myRow.getRowNum()) : -1;
    }
}
