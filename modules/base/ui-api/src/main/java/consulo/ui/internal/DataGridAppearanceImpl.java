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
package consulo.ui.internal;

import consulo.ui.grid.DataGridAppearance;
import consulo.ui.grid.DataGridAppearanceSettings;

/**
 * The appearance a {@code DataGrid} configurator fills in, read by the frontend when it builds its view.
 *
 * @since 2026-10-03
 */
public final class DataGridAppearanceImpl implements DataGridAppearance {
    private int myVisibleRowCount;
    private boolean myShowRowNumbers = true;
    private boolean myTransparentColumnHeaderBackground;
    private boolean myTransparentRowHeaderBackground;
    private int myAdditionalRowsCount;
    private boolean myShowHorizontalLines = true;
    private boolean myStriped;
    private DataGridAppearanceSettings.BooleanMode myBooleanMode = DataGridAppearanceSettings.BooleanMode.TEXT;
    private boolean myAllowMultilineColumnLabels;
    private String myAnonymousColumnName = "<anonymous>";
    private boolean mySpaceForHorizontalScrollbar;
    private boolean myExpandMultilineRows;
    private boolean myHoveredRowBgHighlightingEnabled = true;
    private int myRowLines = 1;
    private Runnable myRowLinesListener = () -> {
    };

    @Override
    public void setResultViewVisibleRowCount(int v) {
        myVisibleRowCount = v;
    }

    public int getVisibleRowCount() {
        return myVisibleRowCount;
    }

    @Override
    public void setResultViewShowRowNumbers(boolean v) {
        myShowRowNumbers = v;
    }

    public boolean isShowRowNumbers() {
        return myShowRowNumbers;
    }

    @Override
    public void setTransparentColumnHeaderBackground(boolean v) {
        myTransparentColumnHeaderBackground = v;
    }

    public boolean isTransparentColumnHeaderBackground() {
        return myTransparentColumnHeaderBackground;
    }

    @Override
    public void setTransparentRowHeaderBackground(boolean v) {
        myTransparentRowHeaderBackground = v;
    }

    public boolean isTransparentRowHeaderBackground() {
        return myTransparentRowHeaderBackground;
    }

    @Override
    public void setResultViewAdditionalRowsCount(int v) {
        myAdditionalRowsCount = v;
    }

    public int getAdditionalRowsCount() {
        return myAdditionalRowsCount;
    }

    @Override
    public void setResultViewSetShowHorizontalLines(boolean v) {
        myShowHorizontalLines = v;
    }

    public boolean isShowHorizontalLines() {
        return myShowHorizontalLines;
    }

    @Override
    public void setResultViewStriped(boolean v) {
        myStriped = v;
    }

    public boolean isStriped() {
        return myStriped;
    }

    @Override
    public void setBooleanMode(DataGridAppearanceSettings.BooleanMode v) {
        myBooleanMode = v;
    }

    @Override
    public DataGridAppearanceSettings.BooleanMode getBooleanMode() {
        return myBooleanMode;
    }

    @Override
    public void setResultViewAllowMultilineColumnLabels(boolean v) {
        myAllowMultilineColumnLabels = v;
    }

    public boolean isAllowMultilineColumnLabels() {
        return myAllowMultilineColumnLabels;
    }

    @Override
    public void setAnonymousColumnName(String name) {
        myAnonymousColumnName = name;
    }

    public String getAnonymousColumnName() {
        return myAnonymousColumnName;
    }

    @Override
    public void addSpaceForHorizontalScrollbar(boolean v) {
        mySpaceForHorizontalScrollbar = v;
    }

    public boolean isSpaceForHorizontalScrollbar() {
        return mySpaceForHorizontalScrollbar;
    }

    @Override
    public void expandMultilineRows(boolean v) {
        myExpandMultilineRows = v;
    }

    public boolean isExpandMultilineRows() {
        return myExpandMultilineRows;
    }

    @Override
    public void setHoveredRowBgHighlightingEnabled(boolean v) {
        myHoveredRowBgHighlightingEnabled = v;
    }

    public boolean isHoveredRowBgHighlightingEnabled() {
        return myHoveredRowBgHighlightingEnabled;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * A negative value is taken as {@code 0}. The grid applies a change at once, also after its view was built.
     */
    @Override
    public void setRowLines(int lines) {
        int value = Math.max(0, lines);
        if (value == myRowLines) {
            return;
        }
        myRowLines = value;
        myRowLinesListener.run();
    }

    @Override
    public int getRowLines() {
        return myRowLines;
    }

    /**
     * Told when the row lines change; the grid controller is the only listener.
     */
    void setRowLinesListener(Runnable listener) {
        myRowLinesListener = listener;
    }
}
