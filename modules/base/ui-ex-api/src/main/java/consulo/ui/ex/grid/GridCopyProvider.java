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

import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.grid.csv.CsvFormats;
import consulo.ui.ex.grid.csv.CsvFormatter;
import consulo.ui.grid.DataAccessType;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridModel;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.SelectionModel;
import consulo.ui.grid.ViewIndex;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Copies the selected cells of a grid as text.
 * <p>
 * A single selected cell is copied as its raw value. A larger selection is copied as tab separated values written by a
 * {@link CsvFormatter} with {@link CsvFormats#TSV_FORMAT}: one record per selected row, the raw values of the selected columns, both
 * in the order the grid shows them. Values are not passed through the object formatter of the grid.
 */
public class GridCopyProvider implements CopyProvider {
    private final DataGrid myGrid;

    public GridCopyProvider(DataGrid grid) {
        myGrid = grid;
    }

    @Override
    @RequiredUIAccess
    public void performCopy(DataContext dataContext) {
        String data = extractSelectedValuesForCopy();
        CopyPasteManager.getInstance().setText(data);
    }

    @Override
    public boolean isCopyEnabled(DataContext dataContext) {
        return !myGrid.isEditing() && !myGrid.isEmpty();
    }

    @Override
    public boolean isCopyVisible(DataContext dataContext) {
        return true;
    }

    private String extractSelectedValuesForCopy() {
        SelectionModel<GridRow, GridColumn> selectionModel = myGrid.getSelectionModel();
        int[] rows = selectionModel.getSelectedRows().toView(myGrid).asArray();
        int[] columns = selectionModel.getSelectedColumns().toView(myGrid).asArray();
        if (rows.length == 0 || columns.length == 0) {
            return "";
        }
        Arrays.sort(rows);
        Arrays.sort(columns);

        GridModel<GridRow, GridColumn> dataModel = myGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
        if (selectionModel.getSelectedRowCount() == 1 &&
            selectionModel.getSelectedColumnCount() == 1) {
            @Nullable Object value = getValue(dataModel, rows[0], columns[0]);
            return value == null ? "" : String.valueOf(value);
        }

        CsvFormatter formatter = new CsvFormatter(CsvFormats.TSV_FORMAT.get());
        StringBuilder out = new StringBuilder();
        for (int row : rows) {
            List<@Nullable Object> values = new ArrayList<>(columns.length);
            for (int column : columns) {
                values.add(getValue(dataModel, row, column));
            }
            out.append(formatter.formatRecord(values));
            out.append(formatter.recordSeparator());
        }
        return out.toString();
    }

    private @Nullable Object getValue(GridModel<GridRow, GridColumn> dataModel, int viewRow, int viewColumn) {
        ModelIndex<GridRow> row = ViewIndex.forRow(myGrid, viewRow).toModel(myGrid);
        ModelIndex<GridColumn> column = ViewIndex.forColumn(myGrid, viewColumn).toModel(myGrid);
        @Nullable Object value = dataModel.getValueAt(row, column);
        return value instanceof ReservedCellValue ? null : value;
    }
}
