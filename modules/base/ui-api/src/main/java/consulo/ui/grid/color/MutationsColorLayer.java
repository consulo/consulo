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
package consulo.ui.grid.color;

import consulo.ui.color.ColorValue;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridMutator;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.MutationData;
import consulo.ui.grid.MutationType;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

/**
 * The backgrounds of pending changes: a modified, inserted or deleted cell, and the row header of a row with such a change, take
 * the background of the change. A value which failed to parse, and every change while the last submit failed, take the error
 * background. The backgrounds are theme colors ({@link ComponentColors} entries), so they follow the current theme.
 */
public class MutationsColorLayer implements ColorLayer {
    private final GridMutator.@Nullable DatabaseMutator<GridRow, GridColumn> myMutator;

    public MutationsColorLayer(GridMutator.@Nullable DatabaseMutator<GridRow, GridColumn> mutator) {
        myMutator = mutator;
    }

    @Override
    public @Nullable ColorValue getCellBackground(ModelIndex<GridRow> row,
                                                  ModelIndex<GridColumn> column,
                                                  DataGrid grid,
                                                  @Nullable ColorValue color) {
        if (myMutator == null) {
            return getColor(null, color);
        }
        MutationData mutation = myMutator.getMutation(row, column);
        if (mutation != null && mutation.getValue() instanceof UnparsedValue) {
            return getFailedToInsertColor();
        }
        MutationType type = myMutator.getMutationType(row, column);
        return getColor(type, color);
    }

    @Override
    public @Nullable ColorValue getRowHeaderBackground(ModelIndex<GridRow> row, DataGrid grid, @Nullable ColorValue color) {
        if (myMutator != null && myMutator.hasUnparsedValues(row)) {
            return getFailedToInsertColor();
        }
        MutationType type = myMutator == null ? null : myMutator.getMutationType(row);
        return getColor(type, color);
    }

    @Override
    public int getPriority() {
        return 1;
    }

    private @Nullable ColorValue getColor(@Nullable MutationType type, @Nullable ColorValue oldColor) {
        ColorValue bg = type != null ? getMutationBackground(type) : null;
        return maybeFailed(bg, oldColor);
    }

    private @Nullable ColorValue maybeFailed(@Nullable ColorValue newColor, @Nullable ColorValue oldColor) {
        return newColor == null ? oldColor :
            myMutator != null && myMutator.isFailed() ? getFailedToInsertColor() :
                newColor;
    }

    private static ComponentColors getMutationBackground(MutationType type) {
        return switch (type) {
            case MODIFY -> ComponentColors.GRID_CELL_MODIFIED_BACKGROUND;
            case INSERT -> ComponentColors.GRID_CELL_INSERTED_BACKGROUND;
            case DELETE -> ComponentColors.GRID_CELL_DELETED_BACKGROUND;
        };
    }

    private static ComponentColors getFailedToInsertColor() {
        return ComponentColors.GRID_CELL_ERROR_BACKGROUND;
    }
}
