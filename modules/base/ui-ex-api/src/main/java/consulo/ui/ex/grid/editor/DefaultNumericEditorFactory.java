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

import consulo.ui.HorizontalAlignment;
import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.ui.grid.editor.GridEditInitiator;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

import static consulo.ui.ex.grid.editor.FormatterCreator.getDecimalKey;

/**
 * The format is picked by the {@link GridTypeKind} of the cell:
 * <ul>
 * <li>{@link GridTypeKind#INTEGER} - the SQL {@code INTEGER}, {@code SMALLINT} and {@code TINYINT}: the Long format. A kind has no
 * {@code BIGINT}, so the BigInt format of {@code GridCellEditorHelper.parseBigIntAsLong} is not reached;</li>
 * <li>{@link GridTypeKind#FLOAT} and {@link GridTypeKind#DECIMAL} - {@code REAL}, {@code FLOAT}, {@code DOUBLE}, {@code DECIMAL} and
 * {@code NUMERIC}: the decimal format.</li>
 * </ul>
 * The Up/Down keys do not commit and move to the row above or below.
 */
public class DefaultNumericEditorFactory extends FormatBasedGridCellEditorFactory {
    @Override
    protected Formatter getFormatInner(GridCellRequest<GridRow, GridColumn> request) {
        Formatter format = getFormat(request);
        if (format != null) {
            return format;
        }
        GridColumn c = Objects.requireNonNull(request.getColumn());
        return FormatterCreator.get(request.getGrid()).create(getDecimalKey(c, null));
    }

    @Override
    public int getSuitability(GridCellRequest<GridRow, GridColumn> request) {
        return isNumericCell(request) && getFormat(request) != null ? SUITABILITY_MIN : SUITABILITY_UNSUITABLE;
    }

    private static @Nullable Formatter getFormat(GridCellRequest<GridRow, GridColumn> request) {
        GridColumn c = Objects.requireNonNull(request.getColumn());
        CoreGrid<GridRow, GridColumn> grid = request.getGrid();
        FormatsCache formatsCache = FormatsCache.get(grid);
        FormatterCreator creator = FormatterCreator.get(grid);

        GridCellEditorHelper helper = GridCellEditorHelper.get(grid);
        GridTypeKind type = helper.guessTypeKindForEditing(request);
        if (helper.useBigDecimalWithPriorityType(grid)) {
            return formatsCache.get(FormatsCache.getBigDecimalWithPriorityTypeFormatProvider(type, null), creator);
        }
        return switch (type) {
            case INTEGER -> formatsCache.get(FormatsCache.getLongFormatProvider(null), creator);
            case FLOAT, DECIMAL -> creator.create(getDecimalKey(c, null));
            default -> null;
        };
    }

    /**
     * Whether the cell holds a number: an integer, a decimal or a float.
     */
    private static boolean isNumericCell(GridCellRequest<GridRow, GridColumn> request) {
        GridTypeKind type = GridCellEditorHelper.get(request.getGrid()).guessTypeKindForEditing(request);
        return type == GridTypeKind.INTEGER || type == GridTypeKind.DECIMAL || type == GridTypeKind.FLOAT;
    }

    @Override
    protected FormatBasedGridCellEditor createEditorImpl(GridCellRequest<GridRow, GridColumn> request,
                                                         Formatter format,
                                                         @Nullable ReservedCellValue nullValue,
                                                         GridEditInitiator initiator,
                                                         ValueParser valueParser,
                                                         ValueFormatter valueFormatter) {
        return new NumericEditor(request, format, nullValue, initiator, valueParser, valueFormatter);
    }

    private static class NumericEditor extends FormatBasedGridCellEditor {
        NumericEditor(GridCellRequest<GridRow, GridColumn> request,
                      Formatter format,
                      @Nullable ReservedCellValue nullValue,
                      GridEditInitiator initiator,
                      ValueParser valueParser,
                      ValueFormatter valueFormatter) {
            super(request, format, nullValue, initiator, valueParser, valueFormatter, false);
            // right aligned: the grid is never transposed, and always a table
            getTextField().setHorizontalAlignment(HorizontalAlignment.RIGHT);
        }
    }
}
