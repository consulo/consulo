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
package consulo.ui.grid.editor;

import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.DataAccessType;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridModel;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ObjectFormatterConfig;
import consulo.ui.grid.ObjectFormatterMode;
import consulo.ui.grid.ReservedCellValue;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Creates the editor of a cell, and parses and formats the values of the cells it is {@link #getSuitability suitable} for.
 * <ul>
 * <li>{@link #createEditor} gets a {@link GridEditInitiator}, which tells what started the edit, the same on every frontend;</li>
 * <li>{@link ValueParser#parse} gets the text only - no document, whose charset and byte order mark only an editor of binary
 * values would read;</li>
 * <li>{@link DefaultValueToText} formats a value with the {@link DataGrid#getObjectFormatter() formatter} of the grid; it knows
 * no large object or geometry values - the grid model has none.</li>
 * </ul>
 */
public interface GridCellEditorFactory {
    int SUITABILITY_UNSUITABLE = 0;
    int SUITABILITY_MIN = 1;
    int SUITABILITY_MAX = 10;

    int getSuitability(GridCellRequest<GridRow, GridColumn> request);

    IsEditableChecker getIsEditableChecker();

    GridCellEditorFactory.ValueParser getValueParser(GridCellRequest<GridRow, GridColumn> request);

    GridCellEditorFactory.ValueFormatter getValueFormatter(GridCellRequest<GridRow, GridColumn> request);

    GridCellEditor createEditor(GridCellRequest<GridRow, GridColumn> request, GridEditInitiator initiator);

    /**
     * True when values produced by this factory evaluate per-row, so applying the same edited value across a multi-row
     * selection is safe even on a UNIQUE-constrained column.
     */
    default boolean allowsUniqueMultiEdit() {
        return false;
    }

    interface IsEditableChecker {
        boolean isEditable(@Nullable Object value, CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> column);
    }

    interface ValueParser {
        Object parse(String text);
    }

    interface ValueFormatter {
        ValueFormatterResult format();
    }

    /**
     * The text of a value, with the charset and the byte order mark it is written with.
     */
    record ValueFormatterResult(String text, Charset charset, byte[] bom) {
        private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

        public ValueFormatterResult(String text) {
            this(text, StandardCharsets.UTF_8, UTF8_BOM.clone());
        }
    }

    class DefaultValueToText implements ValueFormatter {
        private final DataGrid myGrid;
        private final ModelIndex<GridColumn> myColumnIdx;
        private final Object myValue;

        public DefaultValueToText(DataGrid grid, ModelIndex<GridColumn> columnIdx, @Nullable Object value) {
            myGrid = grid;
            myColumnIdx = columnIdx;
            myValue = ObjectUtil.notNull(value, ReservedCellValue.NULL);
        }

        @Override
        public ValueFormatterResult format() {
            GridModel<GridRow, GridColumn> model = myGrid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS);
            GridColumn column = model.getColumn(myColumnIdx);
            // the grid formats every cell with one display config
            ObjectFormatterConfig formatterConfig = ObjectFormatterConfig.of(ObjectFormatterMode.DISPLAY);
            String text = myValue instanceof ReservedCellValue || column == null ? "" :
                StringUtil.notNullize(myGrid.getObjectFormatter().objectToString(myValue, column, formatterConfig));
            return new ValueFormatterResult(text);
        }
    }
}
