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

import consulo.logging.Logger;
import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ObjectFormatterConfig;
import consulo.ui.grid.ObjectFormatterMode;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.editor.GridCellEditor;
import consulo.ui.grid.editor.GridCellEditorFactory;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.ui.grid.editor.UnparsedValue.ParsingError;
import consulo.util.lang.ObjectUtil;
import org.jspecify.annotations.Nullable;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Notes:
 * <ul>
 * <li>an editor gets no completion;</li>
 * <li>a value the format cannot print is printed with one display config: the grid formats every cell with the same config;</li>
 * <li>{@link #adaptFormatter} lets a factory put the values of the grid model into the shape its format expects (see
 * {@link DefaultTemporalEditorFactory}).</li>
 * </ul>
 */
public abstract class FormatBasedGridCellEditorFactory implements GridCellEditorFactory {
    private static final Logger LOG = Logger.getInstance(FormatBasedGridCellEditorFactory.class);

    private final boolean myMultiline;

    protected FormatBasedGridCellEditorFactory() {
        this(false);
    }

    protected FormatBasedGridCellEditorFactory(boolean multiline) {
        myMultiline = multiline;
    }

    @Override
    public GridCellEditor createEditor(GridCellRequest<GridRow, GridColumn> request, GridEditInitiator initiator) {
        Formatter formatter = getFormat(request);
        GridCellEditorHelper helper = GridCellEditorHelper.get(request.getGrid());
        ReservedCellValue nullValue = helper.getDefaultNullValue(request.getGrid(), request.getColumnIdx());
        ValueParser valueParser = getValueParser(request);
        ValueFormatter valueFormatter = getValueFormatter(request);
        return createEditorImpl(request, formatter, nullValue, initiator, valueParser, valueFormatter);
    }

    private Formatter getFormat(GridCellRequest<GridRow, GridColumn> request) {
        Formatter baseFormatter = adaptFormatter(getFormatInner(request), request);
        return makeFormatterLenient(request.getGrid()) ? new LenientFormatter(baseFormatter) : baseFormatter;
    }

    protected abstract Formatter getFormatInner(GridCellRequest<GridRow, GridColumn> request);

    /**
     * Wraps the format of {@link #getFormatInner} before it formats a value of the cell or parses the text of the editor - for
     * example to convert between the value classes of the grid model and the classes the format knows.
     */
    protected Formatter adaptFormatter(Formatter formatter, GridCellRequest<GridRow, GridColumn> request) {
        return formatter;
    }

    @Override
    public IsEditableChecker getIsEditableChecker() {
        return (value, grid, column) -> true;
    }

    @Override
    public ValueParser getValueParser(GridCellRequest<GridRow, GridColumn> request) {
        Object initialValue = request.getValue();
        String initialText = getValueFormatter(request).format().text();
        CoreGrid<GridRow, GridColumn> grid = request.getGrid();
        ModelIndex<GridRow> rowIdx = request.getRowIdx();
        ModelIndex<GridColumn> columnIdx = request.getColumnIdx();
        Object databaseValue = GridCellRequest.getDatabaseValue(request);
        String databaseValueText = getValueFormatter(GridCellRequest.overrideValue(request, databaseValue)).format().text();
        Formatter format = getFormat(request);
        ValueParser baseParser = getValueParser(format, grid, databaseValue, databaseValueText, columnIdx,
            (text, e) -> GridCellEditorHelper.get(grid).createUnparsedValue(text, e, grid, rowIdx, columnIdx));
        return text -> initialText.equals(text) ? ObjectUtil.notNull(initialValue, ReservedCellValue.NULL) : baseParser.parse(text);
    }

    protected static ValueParser getValueParser(
        Formatter format,
        CoreGrid<GridRow, GridColumn> grid,
        @Nullable Object databaseValue,
        @Nullable String databaseValueText,
        @Nullable ModelIndex<GridColumn> columnIdx,
        BiFunction<? super String, ? super @Nullable ParsingError, UnparsedValue> unparsedValueCreator
    ) {
        ReservedCellValue nullValue = GridCellEditorHelper.get(grid).getDefaultNullValue(grid, columnIdx);
        Formatter parser = new Formatter.Wrapper(format) {
            @Override
            public Object parse(String value) throws ParseException {
                if (databaseValueText != null && databaseValueText.equals(value) && databaseValue != null) {
                    return databaseValue;
                }
                return super.parse(value);
            }

            @Override
            public @Nullable Object parse(String value, ParsePosition position) {
                if (databaseValueText != null && databaseValueText.equals(value) && databaseValue != null) {
                    position.setIndex(value.length());
                    return databaseValue;
                }
                return super.parse(value, position);
            }
        };
        return new ValueParserWrapper(parser, columnIdx != null && GridCellEditorHelper.get(grid).isNullable(grid, columnIdx), nullValue,
            unparsedValueCreator::apply);
    }

    @Override
    public ValueFormatter getValueFormatter(GridCellRequest<GridRow, GridColumn> request) {
        DataGrid grid = (DataGrid) request.getGrid();
        Object value = request.getValue();
        ModelIndex<GridColumn> columnIdx = request.getColumnIdx();
        if (value instanceof UnparsedValue) {
            return new DefaultValueToText(grid, columnIdx, value);
        }
        Formatter format = getFormat(request);
        return () -> {
            try {
                return new ValueFormatterResult(value instanceof ReservedCellValue || value == null ? "" : format.format(value));
            }
            catch (IllegalArgumentException iae) {
                if (LOG.isDebugEnabled()) {
                    String message = "Failed to format object " + value +
                        " of class " + (value == null ? "null" : value.getClass().getName()) +
                        " using format " + format + "\n";
                    LOG.debug(message, iae);
                }
                GridColumn column = request.getColumn();
                String text;
                if (column != null) {
                    // the grid formats every cell with one display config
                    ObjectFormatterConfig config = ObjectFormatterConfig.of(ObjectFormatterMode.DISPLAY);
                    text = Objects.requireNonNullElse(grid.getObjectFormatter().objectToString(value, column, config), "null");
                }
                else {
                    text = value instanceof String ? (String) value : "";
                }
                return new ValueFormatterResult(text);
            }
        };
    }

    protected boolean makeFormatterLenient(CoreGrid<GridRow, GridColumn> grid) {
        return false;
    }

    protected FormatBasedGridCellEditor createEditorImpl(GridCellRequest<GridRow, GridColumn> request,
                                                         Formatter format,
                                                         @Nullable ReservedCellValue nullValue,
                                                         GridEditInitiator initiator,
                                                         ValueParser valueParser,
                                                         ValueFormatter valueFormatter) {
        return new FormatBasedGridCellEditor(request, format, nullValue, initiator, valueParser, valueFormatter, myMultiline);
    }

    private static class LenientFormatter extends Formatter.Wrapper {
        LenientFormatter(Formatter formatter) {
            super(formatter);
        }

        @Override
        public Object parse(String value) {
            try {
                return super.parse(value);
            }
            catch (ParseException ignore) {
            }
            return value;
        }

        @Override
        public @Nullable Object parse(String value, ParsePosition position) {
            Object parsed = super.parse(value, position);
            if (parsed == null || position.getErrorIndex() != -1) {
                position.setErrorIndex(-1);
                return value;
            }
            return parsed;
        }

        @Override
        public String format(Object value) {
            try {
                return super.format(value);
            }
            catch (IllegalArgumentException ignore) {
            }
            return value.toString();
        }
    }
}
