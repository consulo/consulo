// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid.color;

import consulo.ui.color.ColorValue;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import org.jspecify.annotations.Nullable;

/**
 * Decides the background and the text color of a cell, and the background of a row header. Layers apply in the order of their
 * {@link #getPriority() priority}, each one given the color the layers before it decided.
 * <p/>
 * A color is any {@link ColorValue}: a theme color - a {@link consulo.ui.style.StyleColorValue} such as a
 * {@link consulo.ui.style.ComponentColors} entry - follows the current theme, any other color is used as is.
 */
public interface ColorLayer extends Comparable<ColorLayer> {
    /**
     * @param color the background the layers before decided, or {@code null} for the default one
     * @return the background of the cell, or {@code null} for the default one
     */
    @Nullable
    ColorValue getCellBackground(ModelIndex<GridRow> row,
                                 ModelIndex<GridColumn> column,
                                 DataGrid grid,
                                 @Nullable ColorValue color);

    /**
     * @param color the text color the layers before decided, or {@code null} for the default one
     * @return the text color of the cell, or {@code null} for the default one; the default passes {@code color} through
     */
    default @Nullable ColorValue getCellForeground(ModelIndex<GridRow> row,
                                                   ModelIndex<GridColumn> column,
                                                   DataGrid grid,
                                                   @Nullable ColorValue color) {
        return color;
    }

    /**
     * @param color the background the layers before decided, or {@code null} for the default one
     * @return the background of the row header, or {@code null} for the default one
     */
    @Nullable
    ColorValue getRowHeaderBackground(ModelIndex<GridRow> row,
                                      DataGrid grid,
                                      @Nullable ColorValue color);

    int getPriority();

    @Override
    default int compareTo(ColorLayer o) {
        return Integer.compare(getPriority(), o.getPriority());
    }
}
