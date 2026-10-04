// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

public interface DataGridAppearance {
    void setResultViewVisibleRowCount(int v);

    void setResultViewShowRowNumbers(boolean v);

    void setTransparentColumnHeaderBackground(boolean v);

    void setTransparentRowHeaderBackground(boolean v);

    void setResultViewAdditionalRowsCount(int v);

    void setResultViewSetShowHorizontalLines(boolean v);

    void setResultViewStriped(boolean v);

    void setBooleanMode(DataGridAppearanceSettings.BooleanMode v);

    DataGridAppearanceSettings.BooleanMode getBooleanMode();

    void setResultViewAllowMultilineColumnLabels(boolean v);

    void setAnonymousColumnName(String name);

    void addSpaceForHorizontalScrollbar(boolean v);

    void expandMultilineRows(boolean v);

    default void setHoveredRowBgHighlightingEnabled(boolean v) {
    }

    /**
     * How many text lines a row shows. The lines of a value are split at its line breaks only; a value with more lines than the row
     * shows ends with an ellipsis.
     *
     * @param lines {@code 1} for a single line, where a line break shows as a mark; {@code n > 1} for rows of {@code n} lines;
     *              {@code 0} for rows tall enough for the lines of their values - the grid may give every row the height of the
     *              tallest loaded one, and caps the height
     */
    default void setRowLines(int lines) {
    }

    /**
     * @return the text lines of a row, as given to {@link #setRowLines(int)}: {@code 1} by default
     */
    default int getRowLines() {
        return 1;
    }
}
