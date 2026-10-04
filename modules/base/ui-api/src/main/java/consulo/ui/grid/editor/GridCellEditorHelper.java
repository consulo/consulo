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
import consulo.ui.grid.GridCellRequest;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.GridTypeKind;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.ReservedCellValue;
import consulo.ui.grid.editor.UnparsedValue.ParsingError;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * What the cell editors of a grid need to know about its columns and values, kept on the grid ({@link #get}, {@link #set}).
 * <ul>
 * <li>{@link #guessTypeKindForEditing} answers the {@link GridTypeKind} of a cell, {@link #guessTypeKindForColumn} the one of a
 * whole column;</li>
 * <li>{@link #areValuesEqual} compares plain values, numbers and {@code byte[]} / {@code char[]} arrays; it does not compare
 * large object values - the grid model has none.</li>
 * </ul>
 */
public interface GridCellEditorHelper {
    Key<GridCellEditorHelper> GRID_CELL_EDITOR_HELPER_KEY = Key.create("GRID_CELL_EDITOR_HELPER_KEY");

    UnparsedValue createUnparsedValue(String text,
                                      @Nullable ParsingError error,
                                      CoreGrid<GridRow, GridColumn> grid,
                                      ModelIndex<GridRow> row,
                                      ModelIndex<GridColumn> column);

    EnumSet<ReservedCellValue> getSpecialValues(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> column);

    boolean isNullable(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> idx);

    List<String> getEnumValues(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> column);

    BoundaryValueResolver getResolver(CoreGrid<GridRow, GridColumn> grid, @Nullable ModelIndex<GridColumn> column);

    boolean useBigDecimalWithPriorityType(CoreGrid<GridRow, GridColumn> grid);

    boolean parseBigIntAsLong(CoreGrid<GridRow, GridColumn> grid);

    boolean useLenientFormatterForTemporalObjects(CoreGrid<GridRow, GridColumn> grid);

    String getDateFormatSuffix(CoreGrid<GridRow, GridColumn> grid, ModelIndex<GridColumn> column);

    static boolean areValuesEqual(@Nullable Object v1, @Nullable Object v2) {
        return areValuesEqual(v1, v2, GridCellEditorHelper::numberEquals);
    }

    static boolean areValuesEqual(@Nullable Object v1,
                                  @Nullable Object v2,
                                  BiFunction<@Nullable Object, @Nullable Object, ThreeState> numberEquals) {
        Object value1 = v1 == ReservedCellValue.NULL ? null : v1;
        Object value2 = v2 == ReservedCellValue.NULL ? null : v2;

        ThreeState result = simpleEquals(value1, value2);
        if (result == ThreeState.UNSURE) {
            result = numberEquals.apply(value1, value2);
        }
        if (result == ThreeState.UNSURE) {
            result = arrayEquals(value1, value2);
        }
        if (result == ThreeState.UNSURE) {
            result = ThreeState.NO;
        }

        return result.toBoolean();
    }

    static ThreeState simpleEquals(@Nullable Object v1, @Nullable Object v2) {
        return Comparing.equal(v1, v2) ? ThreeState.YES : ThreeState.UNSURE;
    }

    static ThreeState arrayEquals(@Nullable Object v1, @Nullable Object v2) {
        if (v1 != null && v2 != null && v1.getClass() == v2.getClass() && v1.getClass().isArray()) {
            if (v1 instanceof byte[]) {
                return ThreeState.fromBoolean(Arrays.equals((byte[]) v1, (byte[]) v2));
            }
            else if (v1 instanceof char[]) {
                return ThreeState.fromBoolean(Arrays.equals((char[]) v1, (char[]) v2));
            }
        }
        return ThreeState.UNSURE;
    }

    static ThreeState numberEquals(@Nullable Object v1, @Nullable Object v2) {
        Number n1 = ObjectUtil.tryCast(v1, Number.class);
        Number n2 = ObjectUtil.tryCast(v2, Number.class);
        if (n1 != null && n2 != null) {
            //double is too rough for large integers
            long l1 = n1.longValue();
            long l2 = n2.longValue();
            double d1 = n1.doubleValue();
            double d2 = n2.doubleValue();
            return ThreeState.fromBoolean(l1 == l2 && (d1 == d2 || Double.isNaN(d1) && Double.isNaN(d2)));
        }
        return ThreeState.UNSURE;
    }

    @Nullable
    Set<GridEditGuard> getEditGuards();

    static GridCellEditorHelper get(CoreGrid<?, ?> grid) {
        return Objects.requireNonNull(GRID_CELL_EDITOR_HELPER_KEY.get(grid));
    }

    static void set(CoreGrid<?, ?> grid, GridCellEditorHelper helper) {
        GRID_CELL_EDITOR_HELPER_KEY.set(grid, helper);
    }

    /**
     * The kind the factories pick an editor by; {@link GridTypeKind#OTHER}
     * when the request is not valid. A data source maps a kind no factory edits to one that is edited, for example a JSON or UUID
     * column to {@link GridTypeKind#TEXT}.
     */
    GridTypeKind guessTypeKindForEditing(GridCellRequest<GridRow, GridColumn> request);

    /**
     * The kind of a column when there is no cell to ask, for example for a request made with
     * {@link GridCellRequest#requestColumn} - {@link #guessTypeKindForEditing} answers {@link GridTypeKind#OTHER} for it, because
     * its row is not valid. It is a hint only (for example whether a frontend offers to edit the column); whether a cell is
     * editable is decided per cell, by {@link GridCellEditorFactoryProvider#provideEditorFactory}. A helper which maps kinds in
     * {@link #guessTypeKindForEditing} maps them here too.
     */
    default GridTypeKind guessTypeKindForColumn(GridCellRequest<GridRow, GridColumn> request) {
        if (!request.isColumnIdxValid()) {
            return GridTypeKind.OTHER;
        }
        GridColumn column = request.getColumn();
        return column == null ? GridTypeKind.OTHER : column.getType().getKind();
    }

    boolean areValuesEqual(@Nullable Object v1, @Nullable Object v2, @Nullable CoreGrid<GridRow, GridColumn> grid);

    @Nullable
    ReservedCellValue getDefaultNullValue(CoreGrid<GridRow, GridColumn> grid, @Nullable ModelIndex<GridColumn> column);
}
