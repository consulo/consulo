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

import consulo.ui.grid.CoreGrid;
import consulo.ui.grid.DataAccessType;
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.editor.BoundaryValueResolver;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.ui.grid.editor.GridEditGuard;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.ui.grid.editor.UnparsedValue.ParsingError;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import static consulo.ui.ex.grid.editor.FormatsCache.getDateFormatProvider;
import static consulo.ui.ex.grid.editor.FormatsCache.getTimeFormatProvider;
import static consulo.ui.ex.grid.editor.FormatsCache.getTimestampFormatProvider;

/**
 * The default editor helper: {@link #guessTypeKindForEditing} answers the {@link GridTypeKind} of the column.
 */
public class GridCellEditorHelperImpl implements GridCellEditorHelper {
    @Override
    public UnparsedValue createUnparsedValue(String text,
                                             @Nullable ParsingError error,
                                             CoreGrid<GridRow, GridColumn> grid,
                                             ModelIndex<GridRow> row,
                                             ModelIndex<GridColumn> column) {
        return new UnparsedValue(text, error);
    }

    @Override
    public EnumSet<ReservedCellValue> getSpecialValues(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> column) {
        return EnumSet.of(ReservedCellValue.NULL);
    }

    @Override
    public boolean isNullable(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> idx) {
        return true;
    }

    @Override
    public List<String> getEnumValues(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> column) {
        return List.of();
    }

    @Override
    public BoundaryValueResolver getResolver(CoreGrid<GridRow, GridColumn> grid, @Nullable ModelIndex<GridColumn> column) {
        return BoundaryValueResolver.ALWAYS_NULL;
    }

    @Override
    public boolean useBigDecimalWithPriorityType(CoreGrid<GridRow, GridColumn> grid) {
        return false;
    }

    @Override
    public boolean parseBigIntAsLong(CoreGrid<GridRow, GridColumn> grid) {
        return false;
    }

    @Override
    public boolean useLenientFormatterForTemporalObjects(CoreGrid<GridRow, GridColumn> grid) {
        return false;
    }

    @Override
    public String getDateFormatSuffix(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> column) {
        Formatter dateFormat = getDateFormat(grid, column);
        return dateFormat != null ? " (" + dateFormat + ") " : "";
    }

    protected @Nullable Formatter getDateFormat(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> columnIdx) {
        // the request of the column (row -1) is not valid, so the kind is OTHER
        GridTypeKind type = guessTypeKindForEditing(GridCellRequest.requestColumn(grid, columnIdx));
        FormatterCreator creator = FormatterCreator.get(grid);
        FormatsCache cache = FormatsCache.get(grid);
        GridColumn column = grid.getDataModel(DataAccessType.DATA_WITH_MUTATIONS).getColumn(columnIdx);
        return type == GridTypeKind.DATE ? cache.get(getDateFormatProvider(column, null), creator) :
            type == GridTypeKind.TIME ? cache.get(getTimeFormatProvider(column, null), creator) :
                type == GridTypeKind.TIMESTAMP ? cache.get(getTimestampFormatProvider(column, null), creator) : null;
    }

    @Override
    public GridTypeKind guessTypeKindForEditing(GridCellRequest<GridRow, GridColumn> request) {
        if (!request.isValid()) {
            return GridTypeKind.OTHER;
        }
        GridColumn c = request.getColumn();
        return c == null ? GridTypeKind.OTHER : c.getType().getKind();
    }

    @Override
    public @Nullable ReservedCellValue getDefaultNullValue(CoreGrid<GridRow, GridColumn> grid, @Nullable ModelIndex<GridColumn> column) {
        return ReservedCellValue.NULL;
    }

    @Override
    public boolean areValuesEqual(@Nullable Object v1, @Nullable Object v2, @Nullable CoreGrid<GridRow, GridColumn> grid) {
        return GridCellEditorHelper.areValuesEqual(v1, v2, numberEquals(null, grid));
    }

    protected BiFunction<@Nullable Object, @Nullable Object, ThreeState> numberEquals(@Nullable GridDataHookUp<GridRow, GridColumn> hookUp,
                                                                                      @Nullable CoreGrid<GridRow, GridColumn> grid) {
        return GridCellEditorHelper::numberEquals;
    }

    @Override
    public @Nullable Set<GridEditGuard> getEditGuards() {
        return null;
    }
}
