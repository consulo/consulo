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

import consulo.localize.LocalizeValue;
import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridAppearanceSettings;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.editor.GridCellEditor;
import consulo.ui.grid.editor.GridCellEditorFactory;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The editors of a boolean cell:
 * <ul>
 * <li>the text mode editor is a {@link GridCellEditorPresentation.Kind#LIST} of the values a boolean cell takes; the frontend
 * applies the value object of a choice with {@link GridCellEditor#selectOption};</li>
 * <li>a value decided when the editor is created - typed with a key, or toggled in check box mode - makes a
 * {@link GridCellEditorPresentation.Kind#NONE} editor, which the grid commits at once;</li>
 * <li>the typed key is the typed character of the {@link GridEditInitiator} ({@link GridEditInitiator#getTypedKeyChar()});</li>
 * <li>a boolean column is a {@link GridTypeKind#BOOLEAN} one.</li>
 * </ul>
 */
public class DefaultBooleanEditorFactory implements GridCellEditorFactory {

    protected static final Formatter myParser = new FormatterImpl() {
        @Override
        protected LocalizeValue getErrorMessage() {
            return LocalizeValue.localizeTODO("Expected boolean value");
        }

        @Override
        public @Nullable Object parse(String value, ParsePosition position) {
            String text = value.trim();
            if (StringUtil.equalsIgnoreCase(text, "true") || StringUtil.equalsIgnoreCase(text, "1")) {
                position.setIndex(value.length());
                return Boolean.TRUE;
            }
            if (StringUtil.equalsIgnoreCase(text, "false") || StringUtil.equalsIgnoreCase(text, "0")) {
                position.setIndex(value.length());
                return Boolean.FALSE;
            }
            position.setErrorIndex(0);
            return null;
        }

        @Override
        public String format(Object value) {
            throw new UnsupportedOperationException();
        }
    };


    @Override
    public int getSuitability(GridCellRequest<GridRow, GridColumn> request) {
        GridTypeKind type = GridCellEditorHelper.get(request.getGrid()).guessTypeKindForEditing(request);
        GridColumn c = Objects.requireNonNull(request.getColumn());
        return isBooleanColumn(c, type) ?
            SUITABILITY_MIN + 1 :
            SUITABILITY_UNSUITABLE;
    }

    /**
     * Whether the column holds booleans: its kind is {@link GridTypeKind#BOOLEAN}.
     */
    private static boolean isBooleanColumn(GridColumn column, GridTypeKind type) {
        return type == GridTypeKind.BOOLEAN;
    }

    @Override
    public IsEditableChecker getIsEditableChecker() {
        return (value, grid, column) -> true;
    }

    @Override
    public GridCellEditor createEditor(GridCellRequest<GridRow, GridColumn> request, GridEditInitiator initiator) {
        EnumSet<ReservedCellValue> opts =
            GridCellEditorHelper.get(request.getGrid()).getSpecialValues(request.getGrid(), request.getColumnIdx());
        Object typedValue = null;
        if (initiator.isKey()) {
            char code = initiator.getTypedKeyChar();
            typedValue = 'T' == code || '1' == code ? Boolean.TRUE :
                'F' == code || '0' == code ? Boolean.FALSE :
                    'N' == code && opts.contains(ReservedCellValue.NULL) ? ReservedCellValue.NULL :
                        'D' == code && opts.contains(ReservedCellValue.DEFAULT) ? ReservedCellValue.DEFAULT :
                            'C' == code && opts.contains(ReservedCellValue.COMPUTED) ? ReservedCellValue.COMPUTED :
                                'G' == code && opts.contains(ReservedCellValue.GENERATED) ? ReservedCellValue.GENERATED :
                                    ' ' == code ? (Object) !Boolean.TRUE.equals(request.getValue()) :
                                        null;
        }
        GridCellRequest<GridRow, GridColumn> request2 =
            request.getValue() == null ? GridCellRequest.overrideValue(request, ReservedCellValue.NULL) : request;
        return ((DataGrid) request.getGrid()).getAppearance().getBooleanMode() == DataGridAppearanceSettings.BooleanMode.TEXT
            ? new TextBooleanCellEditor(request2, opts, typedValue)
            : new CheckboxBooleanCellEditor(request2, typedValue);
    }

    @Override
    public ValueParser getValueParser(GridCellRequest<GridRow, GridColumn> request) {
        CoreGrid<GridRow, GridColumn> grid = request.getGrid();
        ModelIndex<GridRow> rowIdx = request.getRowIdx();
        ModelIndex<GridColumn> columnIdx = request.getColumnIdx();
        GridCellEditorHelper editorHelper = GridCellEditorHelper.get(grid);
        return new ValueParserWrapper(myParser, editorHelper.isNullable(grid, columnIdx),
            editorHelper.getDefaultNullValue(grid, columnIdx),
            (text, e) -> editorHelper.createUnparsedValue(text, e, grid, rowIdx, columnIdx));
    }

    @Override
    public GridCellEditorFactory.ValueFormatter getValueFormatter(GridCellRequest<GridRow, GridColumn> request) {
        return new DefaultValueToText((DataGrid) request.getGrid(), request.getColumnIdx(), request.getValue());
    }

    private abstract static class AbstractBooleanCellEditor extends GridCellEditor.Adapter {
        final DataGrid myGrid;
        final Object myInitialValue;
        private final boolean myShouldMoveFocus;
        @Nullable Object myValue;

        AbstractBooleanCellEditor(GridCellRequest<GridRow, GridColumn> request, @Nullable Object typedValue) {
            myGrid = (DataGrid) request.getGrid();
            // never null: the factory passes NULL for a null value
            myInitialValue = ObjectUtil.notNull(request.getValue(), ReservedCellValue.NULL);
            myShouldMoveFocus = typedValue == null;
            if (typedValue != null) {
                // the typed value is decided at once; the grid commits the editor without showing it
                myValue = typedValue;
            }
        }

        @Override
        public String getText() {
            return myInitialValue.toString();
        }

        /**
         * The editor has no text field: a choice is applied with {@link #selectOption}.
         */
        @Override
        public void setText(String text) {
        }

        @Override
        public void setEditingListener(Consumer<@Nullable Object> listener) {
            super.setEditingListener(listener);
            if (!myShouldMoveFocus && myValue != null) {
                // the typed value is fired once the grid installed its listener
                fireEditing(myValue);
            }
        }

        @Override
        public GridCellEditorPresentation getPresentation() {
            return GridCellEditorPresentation.none(getText());
        }

        void setValue(Object value) {
            myValue = value;
            fireEditing(value);
        }

        @Override
        public @Nullable Object getValue() {
            return myValue != null ? myValue : myInitialValue;
        }

        @Override
        public boolean isColumnSpanAllowed() {
            return false;
        }

        @Override
        public boolean shouldMoveFocus() {
            return myShouldMoveFocus;
        }
    }

    private static final class TextBooleanCellEditor extends AbstractBooleanCellEditor {
        final EnumSet<ReservedCellValue> myAllowedValues;
        private final Object[] myOptions;

        private TextBooleanCellEditor(GridCellRequest<GridRow, GridColumn> request,
                                      EnumSet<ReservedCellValue> allowedValues,
                                      @Nullable Object typedValue) {
            super(request, typedValue);
            myAllowedValues = allowedValues;
            myOptions = ArrayUtil.mergeArrays(new Object[]{Boolean.TRUE, Boolean.FALSE}, myAllowedValues.toArray());
        }

        /**
         * A list with speed search under the cell when no value was typed; its first item is selected.
         */
        @Override
        public GridCellEditorPresentation getPresentation() {
            if (!shouldMoveFocus()) {
                return super.getPresentation();
            }
            List<String> labels = new ArrayList<>(myOptions.length);
            for (Object option : myOptions) {
                labels.add(getOptionText(option));
            }
            return GridCellEditorPresentation.list(getText(), labels, 0);
        }

        /**
         * Applies the value of the chosen option.
         */
        @Override
        public void selectOption(int index) {
            setValue(myOptions[index]);
        }

        /**
         * The label of an option: the value in lower case, or "Clear Field" for {@link ReservedCellValue#UNSET}.
         */
        private static String getOptionText(Object value) {
            return value == ReservedCellValue.UNSET
                ? LocalizeValue.localizeTODO("Clear Field").get()
                : StringUtil.notNullize(StringUtil.toLowerCase(value.toString())); //NON-NLS
        }
    }

    private static class CheckboxBooleanCellEditor extends AbstractBooleanCellEditor {
        CheckboxBooleanCellEditor(GridCellRequest<GridRow, GridColumn> request, @Nullable Object typedValue) {
            super(request, typedValue != null
                ? typedValue
                : Boolean.TRUE.equals(request.getValue())
                ? Boolean.FALSE
                : Boolean.TRUE);
        }
    }
}
