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
package consulo.ui.ex.grid.editor;

import consulo.ui.grid.BaseObjectFormatter;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.editor.GridCellEditor;
import consulo.ui.grid.editor.GridCellEditorFactory;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

/**
 * Notes:
 * <ul>
 * <li>the factory is picked by the {@link GridTypeKind} of the cell - {@link GridTypeKind#TEXT} and {@link GridTypeKind#XML}, the
 * character, CLOB and {@code SQLXML} types of {@code java.sql.Types}. A data source backed by a document (a CSV file), which would be
 * edited as text in every column, is not supported;</li>
 * <li>the grid model has no LOB values, so a value is editable when it is {@code null}, a {@link String}, a
 * {@link ReservedCellValue} or an array shorter than {@code BaseObjectFormatter.MAX_ARRAY_SIZE};</li>
 * <li>the editor cannot load its value from a file.</li>
 * </ul>
 */
public class DefaultTextEditorFactory implements GridCellEditorFactory {
    @Override
    public int getSuitability(GridCellRequest<GridRow, GridColumn> request) {
        return getCommonSuitability(request);
    }

    @Override
    public ValueFormatter getValueFormatter(GridCellRequest<GridRow, GridColumn> request) {
        return getValueFormatter((DataGrid) request.getGrid(), request.getColumnIdx(), request.getValue());
    }

    private static ValueFormatter getValueFormatter(DataGrid grid, ModelIndex<GridColumn> columnIdx, @Nullable Object value) {
        return new DefaultValueToText(grid, columnIdx, value);
    }

    @Override
    public ValueParser getValueParser(GridCellRequest<GridRow, GridColumn> request) {
        Object initialValue = request.getValue();
        // Respect the editor opening value so editSelectedCellWithValue() can control the unchanged commit result.
        String initialText = getValueFormatter(request).format().text();
        return text -> {
            boolean valueChanged = !initialText.equals(text);
            return valueChanged ? text : ObjectUtil.notNull(initialValue, ReservedCellValue.NULL);
        };
    }

    @Override
    public IsEditableChecker getIsEditableChecker() {
        return (value, grid, column) -> {
            return isEditable(value);
        };
    }

    protected static boolean isEditable(@Nullable Object value) {
        // the longest array the formatter shows whole
        if (value == null || value instanceof String || value instanceof ReservedCellValue ||
            (value instanceof Object[] objArr && objArr.length < BaseObjectFormatter.MAX_ARRAY_SIZE)) {
            return true;
        }
        return false;
    }

    @Override
    public GridCellEditor createEditor(GridCellRequest<GridRow, GridColumn> request, GridEditInitiator initiator) {
        ValueParser parser = getValueParser(request);
        ValueFormatter formatter = getValueFormatter(request);
        return new GridTextCellEditor(request, initiator, getIsEditableChecker(), parser, formatter);
    }

    private static int getCommonSuitability(GridCellRequest<GridRow, GridColumn> request) {
        return switch (GridCellEditorHelper.get(request.getGrid()).guessTypeKindForEditing(request)) {
            case TEXT, XML -> SUITABILITY_MIN;
            default -> SUITABILITY_UNSUITABLE;
        };
    }

    private static class GridTextCellEditor extends GridTextCellEditorBase {
        private final ValueParser myValueParser;

        private GridTextCellEditor(GridCellRequest<GridRow, GridColumn> request,
                                   GridEditInitiator initiator,
                                   IsEditableChecker editableChecker,
                                   ValueParser valueParser,
                                   ValueFormatter valueFormatter) {
            super(request, initiator, editableChecker, valueFormatter);
            myValueParser = valueParser;
        }

        @Override
        public @Nullable Object getValue() {
            return isValueEditable() ? myValueParser.parse(myTextField.getText()) : myValue;
        }
    }
}
