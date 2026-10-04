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
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import org.jspecify.annotations.Nullable;

/**
 * The colors of the cells and row headers of a grid. A theme color - a {@link consulo.ui.style.StyleColorValue} such as a
 * {@link consulo.ui.style.ComponentColors} entry - follows the current theme, any other {@link ColorValue} is used as is. A
 * {@code null} color means the default one of the frontend; selection colors are the frontend's own and override these.
 * <p/>
 * The colors of the column headers are left out, like in {@link ColorLayer}.
 */
public interface GridColorModel {
    @Nullable
    ColorValue getCellBackground(ModelIndex<GridRow> row, ModelIndex<GridColumn> column);

    /**
     * @return the text color of the cell, or {@code null} for the default one
     */
    @Nullable
    ColorValue getCellForeground(ModelIndex<GridRow> row, ModelIndex<GridColumn> column);

    @Nullable
    ColorValue getRowHeaderBackground(ModelIndex<GridRow> row);
}
