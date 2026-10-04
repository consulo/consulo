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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.grid.GridUtil;
import consulo.ui.ex.grid.action.TableResultColumnHeaderPopupGroup;
import consulo.ui.ex.grid.action.TableResultPopupGroup;
import consulo.ui.ex.grid.editor.GridCellEditorFactoryImpl;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridAppearanceSettings.BooleanMode;
import consulo.ui.grid.DataGridListener;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridMutationModel;
import consulo.ui.grid.GridPagingModel;
import consulo.ui.grid.GridRequestSource;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.SelectionModel;
import consulo.ui.grid.ThrowableInfo;
import consulo.ui.grid.color.ColorLayer;
import consulo.ui.grid.color.GridColorModelImpl;
import consulo.ui.grid.editor.GridCellEditorFactoryProvider;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The data grid over {@link SampleGridData} - 2000 rows of every kind of value, paged by 100 - with switches which make
 * the data source fail or answer slowly, switches of how the grid shows its rows, and a status line of the loaded page and the
 * selection. The cells are edited in place; the changes stay pending until Submit in the toolbar of the grid, Revert drops them, and
 * Set NULL writes NULL into the selection.
 * <p/>
 * The context menus of the cells and of the column headers insert, delete and clone rows and columns; dragging a column header moves
 * the column. These changes are made at once - see {@link DemoGridMutator}. The columns stay for Reload and the other pages, the rows
 * only until the page is loaded again, which the status line tells while the page holds such rows. The delete key clears the selected
 * cells, as Clear Cells does; Delete Rows is in the context menu.
 *
 * @since 2026-10-03
 */
@ExtensionImpl(id = "grid", order = "before layouts")
public class GridUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > Grid");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        // the data source answers on a background thread, and reports the answers back on this thread
        DemoGridHookUp hookUp = new DemoGridHookUp(UIAccess.current());
        Disposer.register(uiDisposable, hookUp);

        // the grid loads its first page by itself; it shows the row and column actions in its context menus
        DataGrid grid = GridUtil.createDataGrid(
            hookUp,
            TableResultPopupGroup.ID,
            TableResultColumnHeaderPopupGroup.ID,
            (dataGrid, appearance) -> {
                appearance.setResultViewShowRowNumbers(true);
                // a grid is edited once it has editors
                GridCellEditorFactoryProvider.set(dataGrid, GridCellEditorFactoryImpl.getInstance());
                GridCellEditorHelper.set(dataGrid, new DemoGridCellEditorHelper());
                // the rows are deleted at once, which Revert cannot undo - the delete key must not do it without a question
                dataGrid.putUserData(GridUtil.DELETE_CLEARS_CELLS, true);
            }
        );
        Disposer.register(uiDisposable, grid);

        // a page load or a Submit, whichever comes first
        CheckBox failBox = CheckBox.create(LocalizeValue.localizeTODO("Fail next request"));
        failBox.addValueListener(event -> hookUp.setFailNext(Boolean.TRUE.equals(event.getValue())));

        CheckBox slowBox = CheckBox.create(LocalizeValue.localizeTODO("Slow requests (2s)"));
        slowBox.addValueListener(event -> hookUp.setSlow(Boolean.TRUE.equals(event.getValue())));

        // the editor of a boolean toggles it at once, instead of offering true, false and null
        CheckBox booleanBox = CheckBox.create(LocalizeValue.localizeTODO("Booleans as check boxes"));
        booleanBox.addValueListener(event -> {
            boolean checkBoxes = Boolean.TRUE.equals(event.getValue());
            grid.getAppearance().setBooleanMode(checkBoxes ? BooleanMode.CHECKBOX : BooleanMode.TEXT);
        });

        // every column in a text color of its own
        ColorLayer rainbowLayer = new RainbowColumnsLayer();
        CheckBox rainbowBox = CheckBox.create(LocalizeValue.localizeTODO("Rainbow columns"));
        rainbowBox.addValueListener(event -> setRainbowColumns(grid, hookUp, rainbowLayer, Boolean.TRUE.equals(event.getValue())));

        ComboBox<Integer> rowLinesBox = ComboBox.<Integer>builder()
            .add(1, LocalizeValue.of("1"))
            .add(2, LocalizeValue.of("2"))
            .add(3, LocalizeValue.of("3"))
            // as many lines as the tallest loaded value has
            .add(0, LocalizeValue.localizeTODO("Auto"))
            .build();
        rowLinesBox.setValue(grid.getAppearance().getRowLines());
        rowLinesBox.addValueListener(event -> {
            Integer lines = event.getValue();
            if (lines != null) {
                grid.getAppearance().setRowLines(lines);
            }
        });

        HorizontalLayout rowLines = HorizontalLayout.create(Space.SMALL);
        rowLines.add(Label.create(LocalizeValue.localizeTODO("Row lines:")));
        rowLines.add(rowLinesBox);

        // check boxes have no frame of their own - a bigger gap keeps them apart
        HorizontalLayout sourceOptions = HorizontalLayout.create(Space.MEDIUM);
        sourceOptions.add(failBox);
        sourceOptions.add(slowBox);

        HorizontalLayout viewOptions = HorizontalLayout.create(Space.MEDIUM);
        viewOptions.add(booleanBox);
        viewOptions.add(rainbowBox);
        viewOptions.add(rowLines);

        VerticalLayout options = VerticalLayout.create(Space.SMALL);
        options.add(sourceOptions);
        options.add(viewOptions);

        Label pageLabel = Label.create(LocalizeValue.localizeTODO("Loading…"));
        Label selectionLabel = Label.create(selectionText(grid.getSelectionModel()));

        grid.addDataGridListener(new DataGridListener() {
            @Override
            public void onSelectionChanged(DataGrid dataGrid, boolean isAdjusting) {
                selectionLabel.setText(selectionText(dataGrid.getSelectionModel()));
            }

            @Override
            public void onContentChanged(DataGrid dataGrid, GridRequestSource.@Nullable RequestPlace place) {
                // a reload maps the selection onto the new rows without reporting a selection change
                selectionLabel.setText(selectionText(dataGrid.getSelectionModel()));
            }
        }, uiDisposable);

        // the data source reports its requests on the UI thread
        hookUp.addRequestListener(new GridDataHookUp.RequestListener<>() {
            @Override
            @RequiredUIAccess
            public void error(GridRequestSource source, ThrowableInfo errorInfo) {
                LocalizeValue message = LocalizeValue.of(errorInfo.getMessage());
                pageLabel.setText(LocalizeValue.join(LocalizeValue.localizeTODO("Request failed: "), message));
            }

            @Override
            public void updateCountReceived(GridRequestSource source, int updateCount) {
            }

            @Override
            @RequiredUIAccess
            public void requestStarted(GridRequestSource source) {
                pageLabel.setText(LocalizeValue.localizeTODO("Loading…"));
            }

            @Override
            @RequiredUIAccess
            public void requestFinished(GridRequestSource source, boolean success) {
                if (success) {
                    pageLabel.setText(pageText(hookUp));
                }
                // the data source fails one request only - the box follows it
                if (!hookUp.isFailNext() && Boolean.TRUE.equals(failBox.getValue())) {
                    failBox.setValue(false, false);
                }
            }
        }, uiDisposable);

        HorizontalLayout status = HorizontalLayout.create(Space.LARGE);
        status.add(pageLabel);
        status.add(selectionLabel);

        // no scrollable layout around the grid - it scrolls by itself
        return DockLayout.create().top(options).center(grid).bottom(status);
    }

    /**
     * Adds the layer to the colors of the grid, or removes it. The color model tells nobody of its layers, so the loaded cells are
     * painted again.
     */
    @RequiredUIAccess
    private static void setRainbowColumns(DataGrid grid, DemoGridHookUp hookUp, ColorLayer layer, boolean rainbow) {
        if (!(grid.getColorModel() instanceof GridColorModelImpl colorModel)) {
            return;
        }
        colorModel.removeLayer(layer);
        if (rainbow) {
            colorModel.addLayer(layer);
        }
        GridMutationModel mutationModel = hookUp.getMutationModel();
        mutationModel.notifyCellsUpdated(mutationModel.getRowIndices(), mutationModel.getColumnIndices(), null);
    }

    /**
     * The loaded page; while it holds inserted or cloned rows, or misses deleted ones, also how many rows it shows, and that they last
     * until the page is loaded again - a load does not ask before it drops them.
     */
    private static LocalizeValue pageText(DemoGridHookUp hookUp) {
        // the numbers are taken now - the text is formatted again whenever the language changes
        GridPagingModel<GridRow, GridColumn> pageModel = hookUp.getPageModel();
        int pageStart = pageModel.getPageStart();
        int pageEnd = pageModel.getPageEnd();
        long total = pageModel.getTotalRowCount();
        LocalizeValue page = LocalizeValue.localizeTODO("Rows %d-%d of %d")
            .map(text -> String.format(Locale.ROOT, text, pageStart, pageEnd, total));
        if (!hookUp.getMutator().hasUnsavedRows()) {
            return page;
        }
        int shown = hookUp.getDataModel().getRowCount();
        LocalizeValue unsaved = LocalizeValue.localizeTODO(" (%d shown - inserted and deleted rows last until the page is loaded again)")
            .map(text -> String.format(Locale.ROOT, text, shown));
        return LocalizeValue.join(page, unsaved);
    }

    private static LocalizeValue selectionText(SelectionModel<GridRow, GridColumn> selection) {
        int rows = selection.getSelectedRowCount();
        int columns = selection.getSelectedColumnCount();
        return LocalizeValue.localizeTODO("Selected rows: %d, columns: %d").map(text -> String.format(Locale.ROOT, text, rows, columns));
    }

    /**
     * The text of every column in a color of the grid palette, which repeats after ten columns: the color belongs to the position of
     * the column in the model ({@code 1 + model index % 10}), not to the place it is shown at. This data source reorders its model when a
     * column is moved, so the moved column and the columns it passes take the colors of their new positions.
     */
    private static final class RainbowColumnsLayer implements ColorLayer {
        private static final ComponentColors[] COLORS = {
            ComponentColors.GRID_COLUMN_FOREGROUND_1,
            ComponentColors.GRID_COLUMN_FOREGROUND_2,
            ComponentColors.GRID_COLUMN_FOREGROUND_3,
            ComponentColors.GRID_COLUMN_FOREGROUND_4,
            ComponentColors.GRID_COLUMN_FOREGROUND_5,
            ComponentColors.GRID_COLUMN_FOREGROUND_6,
            ComponentColors.GRID_COLUMN_FOREGROUND_7,
            ComponentColors.GRID_COLUMN_FOREGROUND_8,
            ComponentColors.GRID_COLUMN_FOREGROUND_9,
            ComponentColors.GRID_COLUMN_FOREGROUND_10
        };

        /**
         * After the layer of the pending changes, which colors backgrounds only.
         */
        private static final int PRIORITY = 2;

        @Override
        public @Nullable ColorValue getCellBackground(ModelIndex<GridRow> row,
                                                      ModelIndex<GridColumn> column,
                                                      DataGrid grid,
                                                      @Nullable ColorValue color) {
            return color;
        }

        @Override
        public @Nullable ColorValue getCellForeground(ModelIndex<GridRow> row,
                                                      ModelIndex<GridColumn> column,
                                                      DataGrid grid,
                                                      @Nullable ColorValue color) {
            return COLORS[Math.floorMod(column.asInteger(), COLORS.length)];
        }

        @Override
        public @Nullable ColorValue getRowHeaderBackground(ModelIndex<GridRow> row, DataGrid grid, @Nullable ColorValue color) {
            return color;
        }

        @Override
        public int getPriority() {
            return PRIORITY;
        }
    }
}
