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

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.grid.*;
import consulo.ui.grid.color.GridColorModel;
import consulo.ui.grid.color.GridColorModelImpl;
import consulo.ui.grid.color.MutationsColorLayer;
import consulo.ui.grid.editor.GridCellEditor;
import consulo.ui.grid.editor.GridCellEditorFactory;
import consulo.ui.grid.editor.GridCellEditorFactoryProvider;
import consulo.ui.grid.editor.GridCellEditorHelper;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.collection.Lists;
import consulo.util.concurrent.ActionCallback;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

/**
 * What every frontend's {@link DataGrid} shares - everything which is not the native table: listening to the data source,
 * tracking the requests the grid issued, paging, sorting, selection, the mapping between view and model indices, the
 * presentation of a cell, and editing cells.
 * <p/>
 * A frontend builds its native table on top of it: it reads cells through the view indices, reports the native
 * selection back with {@link #onNativeSelectionChanged}, embeds {@link #getToolbar()}, and is told about changes through
 * {@link View}. Every method is called on the UI thread.
 * <p/>
 * Editing: calls of the {@link DataGrid} API go controller → {@link View} → native editor → controller, the user's gestures go
 * native editor → controller. The controller never touches a widget: the frontend opens its editor for the
 * {@link DataGridEditSession} returned by {@link #startEditing}, and closes it with {@link #commitEditing()} or
 * {@link #discardEditing()}.
 * <p/>
 * Pointer gestures on the parts of the table which are not cells come here too: a context menu
 * ({@link #onContextMenuRequested}), a click on a column header ({@link #onColumnHeaderClicked(int, boolean, boolean)},
 * {@link #onColumnHeaderSortClicked}), a column dragged to another place ({@link #onColumnMoved}) and a column resized by the user
 * ({@link #onColumnResized}). Column widths are counted in characters of the grid font ({@link #getViewColumnWidth}), and a row
 * shows {@link #getRowLines()} lines of text ({@link #getCellLines}), so they mean the same on every frontend.
 * <p/>
 * The widths, the selection, the hidden columns and the sort orders survive a change of the columns of the data source: a
 * column is found again by its name when no other column has it and the data source did not make the name up from its position,
 * otherwise by its index.
 *
 * @since 2026-10-03
 */
public final class DataGridController implements Disposable {
    /**
     * Callbacks a frontend's native table answers.
     */
    public interface View {
        /**
         * The visible columns changed - rebuild the columns, with their widths ({@link #getViewColumnWidth}; a width of {@code 0}
         * leaves the column to the frontend's own sizing), and the row height ({@link #getRowLines()}).
         */
        @RequiredUIAccess
        void structureChanged();

        /**
         * The rows or their order changed - reload every row.
         */
        @RequiredUIAccess
        void rowsChanged();

        /**
         * Cells of the rows in {@code [firstViewRow, lastViewRow]} changed in place.
         */
        @RequiredUIAccess
        void cellsChanged(int firstViewRow, int lastViewRow);

        /**
         * Column headers changed their text, for example a sort marker.
         */
        @RequiredUIAccess
        void headersChanged();

        /**
         * The selection was set by the grid - apply {@link #getSelectedViewRows()} and {@link #getSelectedViewColumns()}.
         */
        @RequiredUIAccess
        void selectionChanged();

        @RequiredUIAccess
        void scrollToCell(int viewRow, int viewColumn);

        /**
         * Opens the native editor of the cell and focuses it. The editor asks {@link #startEditing} for its session and does
         * not open when it gets none.
         * <p/>
         * The default does nothing: a frontend without cell editors never edits.
         */
        @RequiredUIAccess
        default void editCellAt(int viewRow, int viewColumn, GridEditInitiator initiator) {
        }

        /**
         * Commits the open native editor through {@link #commitEditing()} and closes it when the commit is accepted.
         *
         * @return {@code false} when the commit is refused and the editor stays open; {@code true} when it is closed, or when no
         * native editor is open
         */
        @RequiredUIAccess
        default boolean stopCellEditor() {
            return true;
        }

        /**
         * Closes the open native editor through {@link #discardEditing()}, if there is one.
         */
        @RequiredUIAccess
        default void cancelCellEditor() {
        }

        /**
         * The widths of the columns were set through the grid ({@link #getViewColumnWidth}) - apply them. A width of {@code 0}
         * leaves the column to the frontend's own sizing. Not called for a width the frontend reported itself
         * ({@link #onColumnResized}).
         */
        @RequiredUIAccess
        default void columnWidthsChanged() {
        }

        /**
         * The text lines of a row changed ({@link #getRowLines()}) - set the height of the rows.
         */
        @RequiredUIAccess
        default void rowHeightsChanged() {
        }

        /**
         * The paging bar was shown or hidden ({@link #isToolbarVisible()}) - show or hide what holds it.
         */
        @RequiredUIAccess
        default void toolbarVisibilityChanged() {
        }
    }

    public static final int MAX_DISPLAY_CHARS = 1000;

    /**
     * The most text lines a row gets when its height follows its values ({@link DataGridAppearance#setRowLines} with {@code 0}).
     */
    public static final int MAX_AUTO_ROW_LINES = 10;

    /**
     * The narrowest and the widest every frontend makes a column it sizes by its header and its first rows, in characters of the
     * grid font. The frontend reports such a width once rows are loaded ({@link #onColumnResized}), so the column keeps it.
     */
    public static final int FIT_MIN_CHARS = 4;
    public static final int FIT_MAX_CHARS = 50;

    private static final TextAttribute NULL_ATTRIBUTE = new TextAttribute(TextAttribute.STYLE_ITALIC, ComponentColors.DISABLED_TEXT);
    private static final int[] PAGE_SIZES = {10, 100, 500, 1000};
    /**
     * Room a fitted column keeps for the sort marker of its header: a space and the arrow.
     */
    private static final int SORT_MARKER_CHARS = 2;

    private final DataGrid myGrid;
    private final GridDataHookUp<GridRow, GridColumn> myHookUp;
    private final UIAccess myUIAccess;
    private final DataGridAppearanceImpl myAppearance = new DataGridAppearanceImpl();
    private final List<DataGridListener> myListeners = Lists.newLockFreeCopyOnWriteList();
    private final Set<GridRequestSource> myPendingRequests = new HashSet<>();
    private final ObjectFormatterConfig myDisplayConfig = ObjectFormatterConfig.of(ObjectFormatterMode.DISPLAY);

    private final SelectionModelImpl mySelectionModel = new SelectionModelImpl();
    private final RawIndexConverter myIndexConverter = new IndexConverterImpl();
    private final GridDataSupport myDataSupport = new DataSupportImpl();
    private final CoreResultView myResultView = new CoreResultView() {
    };

    private @Nullable View myView;
    private Function<DataGrid, ObjectFormatter> myFormatterProvider = grid -> new BaseObjectFormatter();
    private @Nullable ObjectFormatter myFormatter;

    /**
     * Sort order per model column number: negative is ascending, positive descending, and the absolute value is the priority,
     * starting from 1.
     */
    private final Map<Integer, Integer> mySortOrders = new LinkedHashMap<>();
    private final Set<Integer> myHiddenColumns = new HashSet<>();

    private int[] myViewToModelRows = new int[0];
    private int[] myModelToViewRows = new int[0];
    private int[] myViewToModelColumns = new int[0];

    private int[] mySelectedViewRows = new int[0];
    private int[] mySelectedViewColumns = new int[0];
    private int myLeadViewRow = -1;
    private int myLeadViewColumn = -1;

    private @Nullable DataGridEditSession myEditSession;
    private @Nullable PendingEdit myPendingEdit;

    private final GridColorModelImpl myColorModel;
    private MutationsColorLayer myMutationsColorLayer;
    private GridMutator.@Nullable DatabaseMutator<GridRow, GridColumn> myMutationsColorLayerMutator;

    /**
     * How each model column is found again after the columns change, as of the last {@link #rebuildColumns()}.
     */
    private List<ColumnKey> myColumnKeys = List.of();
    /**
     * Model column per name, for the names no other column has.
     */
    private Map<String, Integer> myUniqueColumnNames = Map.of();
    private Set<String> myColumnNames = Set.of();
    /**
     * The widths in characters: by the name of the column, which holds when the columns move, and by its index, for the columns
     * whose name is not unique, or changed.
     */
    private final Map<String, Integer> myWidthsByName = new HashMap<>();
    private final Map<Integer, WidthEntry> myWidthsByIndex = new HashMap<>();
    /**
     * The selection and the column attributes as they were before the model started to change, kept until the current UI task
     * ends: a data source replaces its columns and rows with several events, between which the grid may have no columns or no
     * rows at all.
     */
    private @Nullable StructureSnapshot myStructureSnapshot;

    private GridHitArea myContextArea = GridHitArea.EMPTY;
    private int myContextModelColumn = -1;
    private int[] myContextViewRows = new int[0];
    private int[] myContextViewColumns = new int[0];

    private int myRowLines = 1;
    /**
     * Column moves sent but not started yet: an open edit was committed first, and its value reaches the mutator before the move.
     */
    private int myPendingColumnMoves;
    /**
     * Long requests the data source runs now ({@link LongActionRequestPlace#getLoadingUI()}).
     */
    private int myLongActionCount;

    private @Nullable String myErrorMessage;
    private boolean myStarted;
    private boolean myDisposed;

    private @Nullable Component myToolbar;
    private @Nullable Button myFirstButton;
    private @Nullable Button myPreviousButton;
    private @Nullable Button myNextButton;
    private @Nullable Button myLastButton;
    private @Nullable Button myReloadButton;
    private @Nullable Button mySubmitButton;
    private @Nullable Button myRevertButton;
    private @Nullable Button mySetNullButton;
    private @Nullable Label myPageLabel;
    private @Nullable Label myTotalLabel;
    private @Nullable Label myStatusLabel;
    private @Nullable ComboBox<Integer> myPageSizeBox;
    private List<Component> myToolbarItems = List.of();
    private boolean myToolbarVisible = true;
    private boolean myUpdatingToolbar;

    /**
     * Created before the configurator of the grid runs, so the configurator may add color layers ({@link #getColorModel()}) and set
     * the row lines.
     */
    @RequiredUIAccess
    public DataGridController(DataGrid grid, GridDataHookUp<GridRow, GridColumn> hookUp) {
        myGrid = grid;
        myHookUp = hookUp;
        myUIAccess = UIAccess.current();

        GridMutator.@Nullable DatabaseMutator<GridRow, GridColumn> mutator = getDatabaseMutator();
        myMutationsColorLayer = new MutationsColorLayer(mutator);
        myMutationsColorLayerMutator = mutator;
        myColorModel = new GridColorModelImpl(grid, myMutationsColorLayer);

        myAppearance.setRowLinesListener(() -> onUIThread(this::refreshRowLines));

        rebuildColumns();
        rebuildRows();
        myRowLines = computeRowLines();
    }

    public DataGridAppearanceImpl getAppearance() {
        return myAppearance;
    }

    public GridDataHookUp<GridRow, GridColumn> getHookUp() {
        return myHookUp;
    }

    public void setView(View view) {
        myView = view;
    }

    /**
     * Starts listening to the data source and loads its first page, unless it already holds data. Called by the frontend
     * once its view is set.
     */
    @RequiredUIAccess
    public void start() {
        if (myStarted) {
            return;
        }
        myStarted = true;

        myHookUp.getMutationModel().addListener(new GridModel.Listener<>() {
            @Override
            public void columnsAdded(ModelIndexSet<GridColumn> columns) {
                onUIThread(DataGridController.this::onColumnsChanged);
            }

            @Override
            public void columnsRemoved(ModelIndexSet<GridColumn> columns) {
                onUIThread(DataGridController.this::onColumnsChanged);
            }

            @Override
            public void rowsAdded(ModelIndexSet<GridRow> rows) {
                onUIThread(() -> onRowsChanged(null));
            }

            @Override
            public void rowsRemoved(ModelIndexSet<GridRow> rows) {
                onUIThread(() -> onRowsChanged(null));
            }

            @Override
            public void cellsUpdated(ModelIndexSet<GridRow> rows,
                                     ModelIndexSet<GridColumn> columns,
                                     GridRequestSource.@Nullable RequestPlace place) {
                onUIThread(() -> onCellsUpdated(rows, place));
            }

            @Override
            public void afterLastRowAdded() {
                onUIThread(DataGridController.this::updateToolbar);
            }
        }, this);

        // a hook-up reports its requests on the UI thread
        myHookUp.addRequestListener(new GridDataHookUp.RequestListener<>() {
            @Override
            @RequiredUIAccess
            public void error(GridRequestSource source, ThrowableInfo errorInfo) {
                if (myDisposed || !isOwnRequest(source)) {
                    return;
                }
                myErrorMessage = errorInfo.getMessage();
                updateToolbar();
            }

            @Override
            public void updateCountReceived(GridRequestSource source, int updateCount) {
            }

            @Override
            @RequiredUIAccess
            public void requestStarted(GridRequestSource source) {
                updateToolbar();
            }

            @Override
            @RequiredUIAccess
            public void requestFinished(GridRequestSource source, boolean success) {
                if (myDisposed) {
                    return;
                }
                myPendingRequests.remove(source);
                if (!source.errorOccurred() && isOwnRequest(source)) {
                    myErrorMessage = null;
                }
                updateToolbar();
            }
        }, this);

        if (myHookUp.getMutationModel().getColumnCount() == 0 && myHookUp.getMutationModel().getRowCount() == 0) {
            myUIAccess.give(this::loadFirstPage);
        }
    }

    // region paging

    @RequiredUIAccess
    public void loadFirstPage() {
        issue(source -> myHookUp.getLoader().loadFirstPage(source));
    }

    @RequiredUIAccess
    public void loadPreviousPage() {
        issue(source -> myHookUp.getLoader().loadPreviousPage(source));
    }

    @RequiredUIAccess
    public void loadNextPage() {
        issue(source -> myHookUp.getLoader().loadNextPage(source));
    }

    @RequiredUIAccess
    public void loadLastPage() {
        issue(source -> myHookUp.getLoader().loadLastPage(source));
    }

    @RequiredUIAccess
    public void reloadCurrentPage() {
        issue(source -> myHookUp.getLoader().reloadCurrentPage(source));
    }

    /**
     * Sets the page size and loads again: from the first row when the page grows past the rows shown, otherwise the current
     * page.
     */
    @RequiredUIAccess
    public void setPageSizeAndReload(int pageSize) {
        GridPagingModel<GridRow, GridColumn> pageModel = myHookUp.getPageModel();
        pageModel.setPageSize(pageSize);
        if (pageSize == GridPagingModel.UNLIMITED_PAGE_SIZE || pageSize > getViewRowCount()) {
            issue(source -> myHookUp.getLoader().load(source, 0));
        }
        else {
            issue(source -> myHookUp.getLoader().reloadCurrentPage(source));
        }
    }

    public boolean isBusy() {
        return !myPendingRequests.isEmpty() || myHookUp.getBusyCount() > 0 || myLongActionCount > 0;
    }

    public @Nullable String getErrorMessage() {
        return myErrorMessage;
    }

    /**
     * The page label: the row count of a single page, otherwise the range of rows the page shows.
     */
    public String getPageText() {
        GridPagingModel<GridRow, GridColumn> pageModel = myHookUp.getPageModel();
        if (isSinglePage()) {
            int rowCount = getViewRowCount();
            return rowCount == 1 ? "1 row" : format(rowCount) + " rows";
        }
        if (pageModel.getPageEnd() == 0) {
            return "0 rows";
        }
        return format(pageModel.getPageStart()) + "-" + format(pageModel.getPageEnd());
    }

    /**
     * The total row count next to the page label, empty when the rows fit on a single page.
     */
    public String getTotalText() {
        if (isSinglePage()) {
            return "";
        }
        GridPagingModel<GridRow, GridColumn> pageModel = myHookUp.getPageModel();
        return "of " + format(pageModel.getTotalRowCount()) + (pageModel.isTotalRowCountPrecise() ? "" : "+");
    }

    public boolean isSinglePage() {
        GridPagingModel<GridRow, GridColumn> pageModel = myHookUp.getPageModel();
        return pageModel.isFirstPage() && pageModel.isLastPage();
    }

    /**
     * The paging buttons are hidden when there is nothing to page through.
     */
    public boolean isNavigationVisible() {
        GridPagingModel<GridRow, GridColumn> pageModel = myHookUp.getPageModel();
        int pageSize = pageModel.getPageSize();
        return pageSize != GridPagingModel.UNLIMITED_PAGE_SIZE
            && (!pageModel.isTotalRowCountPrecise() || pageSize < pageModel.getTotalRowCount());
    }

    public boolean canGoBack() {
        return isReadyForRequest() && !myHookUp.getPageModel().isFirstPage();
    }

    public boolean canGoForward() {
        return isReadyForRequest() && !myHookUp.getPageModel().isLastPage();
    }

    public boolean isReadyForRequest() {
        return myGrid.isReady() && !isBusy();
    }

    @RequiredUIAccess
    private void issue(Consumer<GridRequestSource> request) {
        issue(new DataGridRequestPlace(myGrid), request);
    }

    /**
     * Sends a request of this grid: the grid is busy until its source is processed, and an error reported for it shows in the
     * status line until a later request of the grid finishes without one.
     *
     * @return the source of the request, or {@code null} when the grid is disposed and sends nothing
     */
    @RequiredUIAccess
    private @Nullable GridRequestSource issue(GridRequestSource.RequestPlace place, Consumer<GridRequestSource> request) {
        if (myDisposed) {
            return null;
        }
        GridRequestSource source = new GridRequestSource(place);
        myPendingRequests.add(source);
        source.getActionCallback().doWhenProcessed(() -> onUIThread(() -> {
            myPendingRequests.remove(source);
            updateToolbar();
        }));
        updateToolbar();
        request.accept(source);
        return source;
    }

    /**
     * The paging and Reload buttons: the open edit is committed, or cancelled when it cannot be, and pending changes make the
     * grid ask whether to lose them - the request then drops them from the rows it loads
     * ({@link GridRequestSource#setMutatedDataLocally}).
     */
    @RequiredUIAccess
    private void runPageAction(Consumer<GridRequestSource> request) {
        boolean wasEditing = isEditing();
        if (!stopEditing()) {
            cancelEditing();
        }
        if (wasEditing) {
            // a committed value reaches the mutator later (setValueAt) - look for pending changes after it
            myUIAccess.give(() -> runPageRequest(request));
        }
        else {
            runPageRequest(request);
        }
    }

    @RequiredUIAccess
    private void runPageRequest(Consumer<GridRequestSource> request) {
        if (myDisposed) {
            return;
        }
        GridMutator<GridRow, GridColumn> mutator = myHookUp.getMutator();
        if (mutator == null || !mutator.hasPendingChanges()) {
            issue(request);
            return;
        }
        showIgnoreUnsubmittedChangesYesNoDialog(yes -> {
            if (yes && !myDisposed) {
                issue(source -> {
                    source.setMutatedDataLocally(true);
                    request.accept(source);
                });
            }
        });
    }

    private boolean isOwnRequest(GridRequestSource source) {
        return source.place instanceof GridRequestSource.GridRequestPlace<?, ?> place && place.getGrid() == myGrid;
    }

    private static String format(long value) {
        return String.format("%,d", value);
    }

    // endregion

    // region toolbar

    /**
     * The paging bar of the grid, built from unified components, so it looks and behaves the same on every frontend.
     */
    @RequiredUIAccess
    public Component getToolbar() {
        if (myToolbar != null) {
            return myToolbar;
        }

        myFirstButton = Button.create(LocalizeValue.of("«"), event -> runPageAction(source -> myHookUp.getLoader().loadFirstPage(source)));
        myFirstButton.setToolTipText(LocalizeValue.localizeTODO("First Page"));
        myPreviousButton =
            Button.create(LocalizeValue.of("‹"), event -> runPageAction(source -> myHookUp.getLoader().loadPreviousPage(source)));
        myPreviousButton.setToolTipText(LocalizeValue.localizeTODO("Previous Page"));
        myNextButton = Button.create(LocalizeValue.of("›"), event -> runPageAction(source -> myHookUp.getLoader().loadNextPage(source)));
        myNextButton.setToolTipText(LocalizeValue.localizeTODO("Next Page"));
        myLastButton = Button.create(LocalizeValue.of("»"), event -> runPageAction(source -> myHookUp.getLoader().loadLastPage(source)));
        myLastButton.setToolTipText(LocalizeValue.localizeTODO("Last Page"));
        myReloadButton = Button.create(LocalizeValue.localizeTODO("Reload"),
            event -> runPageAction(source -> myHookUp.getLoader().reloadCurrentPage(source)));

        mySubmitButton = Button.create(LocalizeValue.localizeTODO("Submit"), event -> submit());
        mySubmitButton.setToolTipText(LocalizeValue.localizeTODO("Submit Changes"));
        myRevertButton = Button.create(LocalizeValue.localizeTODO("Revert"), event -> revertSelection());
        myRevertButton.setToolTipText(LocalizeValue.localizeTODO("Reverts selected changes"));
        mySetNullButton = Button.create(LocalizeValue.localizeTODO("Set NULL"), event -> setSelectionToNull());
        mySetNullButton.setToolTipText(LocalizeValue.localizeTODO("Sets the selected cells to NULL"));

        myPageLabel = Label.create(LocalizeValue.empty());
        myTotalLabel = Label.create(LocalizeValue.empty());
        myStatusLabel = Label.create(LocalizeValue.empty());

        ComboBox.Builder<Integer> pageSizes = ComboBox.builder();
        Set<Integer> sizes = new TreeSet<>();
        for (int size : PAGE_SIZES) {
            sizes.add(size);
        }
        int current = myHookUp.getPageModel().getPageSize();
        if (current > 0) {
            sizes.add(current);
        }
        for (Integer size : sizes) {
            pageSizes.add(size, LocalizeValue.of(format(size)));
        }
        pageSizes.add(GridPagingModel.UNLIMITED_PAGE_SIZE, LocalizeValue.localizeTODO("All"));
        ComboBox<Integer> pageSizeBox = pageSizes.build();
        pageSizeBox.setValue(current > 0 ? current : GridPagingModel.UNLIMITED_PAGE_SIZE, false);
        pageSizeBox.setToolTipText(LocalizeValue.localizeTODO("Page Size"));
        pageSizeBox.addValueListener(event -> {
            Integer value = event.getValue();
            if (!myUpdatingToolbar && value != null && value != myHookUp.getPageModel().getPageSize()) {
                setPageSizeAndReload(value);
            }
        });
        myPageSizeBox = pageSizeBox;

        HorizontalLayout toolbar = HorizontalLayout.create(Space.X_SMALL);
        toolbar.add(myFirstButton);
        toolbar.add(myPreviousButton);
        toolbar.add(myPageLabel);
        toolbar.add(myTotalLabel);
        toolbar.add(myNextButton);
        toolbar.add(myLastButton);
        toolbar.add(pageSizeBox);
        toolbar.add(myReloadButton);
        toolbar.add(mySubmitButton);
        toolbar.add(myRevertButton);
        toolbar.add(mySetNullButton);
        toolbar.add(myStatusLabel);
        // the first control does not touch the edge of the grid
        toolbar.paddingBuilder().leftSet(Space.SMALL).apply();
        myToolbarItems = List.of(myFirstButton, myPreviousButton, myPageLabel, myTotalLabel, myNextButton, myLastButton, pageSizeBox,
            myReloadButton, mySubmitButton, myRevertButton, mySetNullButton, myStatusLabel);
        myToolbar = toolbar;

        updateToolbar();
        return toolbar;
    }

    /**
     * Whether the paging bar shows anything; it hides itself when none of its controls is visible.
     */
    public boolean isToolbarVisible() {
        return myToolbarVisible;
    }

    @RequiredUIAccess
    private void updateToolbar() {
        if (myToolbar == null || myDisposed) {
            return;
        }

        myUpdatingToolbar = true;
        try {
            boolean navigation = isNavigationVisible();
            boolean back = canGoBack();
            boolean forward = canGoForward();
            for (Button button : List.of(ObjectUtil.notNull(myFirstButton), ObjectUtil.notNull(myPreviousButton))) {
                button.setVisible(navigation);
                button.setEnabled(back);
            }
            for (Button button : List.of(ObjectUtil.notNull(myNextButton), ObjectUtil.notNull(myLastButton))) {
                button.setVisible(navigation);
                button.setEnabled(forward);
            }
            // a data source which always holds all of its rows has no pages, and a loader which has nothing to reload says so
            boolean singlePageSource = myHookUp.getPageModel() instanceof GridPagingModelImpl.SinglePage;
            Button reloadButton = ObjectUtil.notNull(myReloadButton);
            reloadButton.setVisible(myHookUp.getLoader().isReloadable());
            reloadButton.setEnabled(!isBusy());

            ObjectUtil.notNull(myPageLabel).setText(LocalizeValue.of(getPageText()));
            String total = getTotalText();
            Label totalLabel = ObjectUtil.notNull(myTotalLabel);
            totalLabel.setText(LocalizeValue.of(total));
            totalLabel.setVisible(!total.isEmpty());

            ComboBox<Integer> pageSizeBox = ObjectUtil.notNull(myPageSizeBox);
            pageSizeBox.setVisible(!singlePageSource);
            pageSizeBox.setEnabled(isReadyForRequest());
            int pageSize = myHookUp.getPageModel().getPageSize();
            Integer selected = pageSizeBox.getValue();
            if (selected == null || selected != pageSize) {
                pageSizeBox.setValue(pageSize > 0 ? pageSize : GridPagingModel.UNLIMITED_PAGE_SIZE, false);
            }

            Label statusLabel = ObjectUtil.notNull(myStatusLabel);
            String error = myErrorMessage;
            if (isBusy()) {
                statusLabel.setText(LocalizeValue.localizeTODO("Loading…"));
                statusLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);
                statusLabel.setVisible(true);
            }
            else if (error != null) {
                statusLabel.setText(LocalizeValue.of(error));
                statusLabel.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
                statusLabel.setVisible(true);
            }
            else {
                statusLabel.setText(LocalizeValue.empty());
                statusLabel.setForegroundColor(null);
                statusLabel.setVisible(false);
            }

            // last: it also decides whether the bar shows at all
            updateEditButtons();
        }
        finally {
            myUpdatingToolbar = false;
        }
    }

    /**
     * Hides the paging bar while none of its controls is visible, and shows it again once one is.
     */
    @RequiredUIAccess
    private void updateToolbarVisibility() {
        Component toolbar = myToolbar;
        if (toolbar == null || myDisposed) {
            return;
        }
        boolean visible = false;
        for (Component item : myToolbarItems) {
            if (item.isVisible()) {
                visible = true;
                break;
            }
        }
        if (visible == myToolbarVisible) {
            return;
        }
        myToolbarVisible = visible;
        toolbar.setVisible(visible);
        if (myView != null) {
            myView.toolbarVisibilityChanged();
        }
    }

    /**
     * The Submit / Revert / Set NULL segment: shown for a {@link GridMutator.DatabaseMutator} of a data source which is not
     * read-only; Submit is hidden while the mutator submits every change at once.
     */
    @RequiredUIAccess
    private void updateEditButtons() {
        Button submitButton = mySubmitButton;
        Button revertButton = myRevertButton;
        Button setNullButton = mySetNullButton;
        if (submitButton == null || revertButton == null || setNullButton == null || myDisposed) {
            return;
        }

        boolean visible = getDatabaseMutator() != null && !myHookUp.isReadOnly();
        submitButton.setVisible(visible && !myDataSupport.isSubmitImmediately());
        submitButton.setEnabled(canSubmit());
        revertButton.setVisible(isRevertVisible());
        revertButton.setEnabled(canRevertSelection());
        setNullButton.setVisible(visible);
        setNullButton.setEnabled(canSetSelectionToNull());
        updateToolbarVisibility();
    }

    // endregion

    // region columns and rows

    public int getViewColumnCount() {
        return myViewToModelColumns.length;
    }

    public int getViewRowCount() {
        return myViewToModelRows.length;
    }

    public ModelIndex<GridColumn> toModelColumn(int viewColumn) {
        int model = viewColumn >= 0 && viewColumn < myViewToModelColumns.length ? myViewToModelColumns[viewColumn] : -1;
        return ModelIndex.forColumn(getMutationModel(), model);
    }

    public ModelIndex<GridRow> toModelRow(int viewRow) {
        int model = viewRow >= 0 && viewRow < myViewToModelRows.length ? myViewToModelRows[viewRow] : -1;
        return ModelIndex.forRow(getMutationModel(), model);
    }

    public int toViewColumn(ModelIndex<GridColumn> column) {
        for (int i = 0; i < myViewToModelColumns.length; i++) {
            if (myViewToModelColumns[i] == column.asInteger()) {
                return i;
            }
        }
        return -1;
    }

    public int toViewRow(ModelIndex<GridRow> row) {
        int model = row.asInteger();
        return model >= 0 && model < myModelToViewRows.length ? myModelToViewRows[model] : -1;
    }

    public @Nullable GridColumn getColumn(int viewColumn) {
        return getMutationModel().getColumn(toModelColumn(viewColumn));
    }

    /**
     * The column name with a sort marker next to it: an arrow, and the priority when several columns are sorted.
     */
    public String getColumnHeaderText(int viewColumn) {
        GridColumn column = getColumn(viewColumn);
        String name = column == null ? "" : getName(column);
        int order = column == null ? 0 : mySortOrders.getOrDefault(toModelColumn(viewColumn).asInteger(), 0);
        if (order == 0) {
            return name;
        }
        String marker = order < 0 ? "▲" : "▼";
        return name + " " + marker + (mySortOrders.size() > 1 ? Integer.toString(Math.abs(order)) : "");
    }

    /**
     * The column name and the type the data source gives it.
     */
    public String getColumnTooltip(int viewColumn) {
        GridColumn column = getColumn(viewColumn);
        if (column == null) {
            return "";
        }
        GridDataType type = column.getType();
        String typeName = type.getName().isEmpty() ? type.getKind().name().toLowerCase() : type.getName();
        return getName(column) + ": " + typeName;
    }

    public HorizontalAlignment getColumnAlignment(int viewColumn) {
        GridColumn column = getColumn(viewColumn);
        if (column == null) {
            return HorizontalAlignment.LEFT;
        }
        return switch (column.getType().getKind()) {
            case INTEGER, DECIMAL, FLOAT -> HorizontalAlignment.RIGHT;
            default -> HorizontalAlignment.LEFT;
        };
    }

    public boolean isColumnSortable(int viewColumn) {
        GridColumn column = getColumn(viewColumn);
        return column != null && !GridUtilCore.isRowId(column);
    }

    /**
     * A click on the sort marker of a column header - or whatever part of the header the frontend reserves for sorting - toggles
     * the sorting by the column; {@code additive} keeps the other sorted columns.
     */
    @RequiredUIAccess
    public void onColumnHeaderSortClicked(int viewColumn, boolean additive) {
        clearContextMenuTarget();
        // the rows move: the open edit is committed first, and the sorting stays when the commit is refused
        if (!isColumnSortable(viewColumn) || !stopEditing()) {
            return;
        }
        toggleSortColumns(List.of(toModelColumn(viewColumn)), additive);
    }

    /**
     * A click on a column header selects the whole column. The open edit is committed first; the selection stays when the commit
     * is refused.
     *
     * @param extend the interval modifier (Shift): the columns from the lead column to this one are selected
     * @param toggle the exclusive modifier (Ctrl, or Meta on macOS): the column is added to the selection, or removed from it when it
     *               is selected already; with {@code extend}, the interval is added to the selected columns
     */
    @RequiredUIAccess
    public void onColumnHeaderClicked(int viewColumn, boolean extend, boolean toggle) {
        clearContextMenuTarget();
        if (viewColumn < 0 || viewColumn >= getViewColumnCount() || !stopEditing()) {
            return;
        }

        int[] rows = allViewRows();
        int leadRow = getLeadViewRow();
        if (leadRow < 0 && rows.length > 0) {
            leadRow = 0;
        }

        int[] columns;
        int leadColumn;
        if (extend) {
            // the lead stays where it was, so the next click extends from the same column
            int anchor = getLeadViewColumn();
            if (anchor < 0) {
                anchor = viewColumn;
            }
            int[] interval = range(Math.min(anchor, viewColumn), Math.max(anchor, viewColumn));
            columns = toggle ? union(mySelectedViewColumns, interval) : interval;
            leadColumn = anchor;
        }
        else if (toggle) {
            boolean wholeColumnSelected = contains(mySelectedViewColumns, viewColumn) && mySelectedViewRows.length == rows.length;
            if (wholeColumnSelected) {
                columns = Arrays.stream(mySelectedViewColumns).filter(column -> column != viewColumn).toArray();
                leadColumn = columns.length == 0 ? -1 : columns[0];
            }
            else {
                columns = union(mySelectedViewColumns, new int[]{viewColumn});
                leadColumn = viewColumn;
            }
        }
        else {
            columns = new int[]{viewColumn};
            leadColumn = viewColumn;
        }

        if (columns.length == 0) {
            setSelectionInner(new int[0], new int[0]);
        }
        else {
            setSelectionInner(rows, columns, leadRow, leadColumn);
        }
    }

    public boolean isShowRowNumbers() {
        return myAppearance.isShowRowNumbers();
    }

    /**
     * The number of the row in the data source, or the name of a {@link NamedRow}. It is read from the row of the data source, not
     * from the row with the pending changes: a {@link MutationRow} numbers itself by its model index, which is not the number on any
     * page but the first.
     */
    public String getRowNumberText(int viewRow) {
        ModelIndex<GridRow> rowIdx = toModelRow(viewRow);
        GridRow row = myHookUp.getDataModel().getRow(rowIdx);
        if (row == null) {
            row = getMutationModel().getRow(rowIdx);
        }
        if (row instanceof NamedRow namedRow) {
            return namedRow.name;
        }
        return row == null ? "" : Integer.toString(row.getRowNum());
    }

    /**
     * The value of a cell, with the pending changes. While a multi-cell edit is open, every cell it writes shows the value typed
     * in the editor.
     */
    public @Nullable Object getValueAt(int viewRow, int viewColumn) {
        ModelIndex<GridRow> rowIdx = toModelRow(viewRow);
        ModelIndex<GridColumn> columnIdx = toModelColumn(viewColumn);
        DataGridEditSession session = myEditSession;
        if (session != null && session.isTarget(rowIdx, columnIdx) && isMultiEditingAllowed()) {
            return session.getCommonValue();
        }
        return getMutationModel().getValueAt(rowIdx, columnIdx);
    }

    public boolean isNullValue(int viewRow, int viewColumn) {
        Object value = getValueAt(viewRow, viewColumn);
        return value == null || value == ReservedCellValue.NULL || value == ReservedCellValue.UNSET;
    }

    /**
     * The text of a cell on one line: a line break shows as {@code ⏎}, and a long value is cut.
     */
    public String getCellText(int viewRow, int viewColumn) {
        Object value = getValueAt(viewRow, viewColumn);
        if (value == null) {
            return ReservedCellValue.NULL.getDisplayName();
        }
        GridColumn column = getColumn(viewColumn);
        if (column == null) {
            return "";
        }
        String text = getObjectFormatter().objectToString(value, column, myDisplayConfig);
        return toDisplayString(text == null ? "null" : text);
    }

    /**
     * Renders a cell: a {@code null} in the null style, everything else formatted by the grid's {@link ObjectFormatter}, in the text
     * color of the {@link #getColorModel() color model}, on the background of its pending change, if it has one. When a row shows
     * more than one line ({@link #getRowLines()}), the text holds the lines of {@link #getCellLines} separated by {@code \n}.
     */
    @RequiredUIAccess
    public void renderCell(TextItemPresentation presentation, int viewRow, int viewColumn) {
        renderCell(presentation, viewRow, viewColumn, true);
    }

    /**
     * @param paintBackground {@code false} keeps the background of the presentation - a frontend passes it for a selected cell,
     *                        whose selection background stays
     */
    @RequiredUIAccess
    public void renderCell(TextItemPresentation presentation, int viewRow, int viewColumn, boolean paintBackground) {
        renderCell(presentation, viewRow, viewColumn, paintBackground, true);
    }

    /**
     * @param paintBackground {@code false} keeps the background of the presentation - a frontend passes it for a selected cell,
     *                        whose selection background stays
     * @param paintForeground {@code false} leaves out the text color of the color model - a frontend passes it for a selected cell,
     *                        whose text keeps the selection color. The styles of {@code null}, reserved and failed values stay.
     */
    @RequiredUIAccess
    public void renderCell(TextItemPresentation presentation,
                           int viewRow,
                           int viewColumn,
                           boolean paintBackground,
                           boolean paintForeground) {
        if (paintBackground) {
            ColorValue background = getCellBackground(viewRow, viewColumn);
            if (background != null) {
                presentation.withBackgroundColor(background);
            }
        }

        CellContent content = getCellContent(viewRow, viewColumn);
        TextAttribute attribute = switch (content.kind()) {
            case NULL -> NULL_ATTRIBUTE;
            case RESERVED -> TextAttribute.GRAYED;
            case FAILED -> TextAttribute.ERROR;
            case VALUE -> {
                @Nullable ColorValue foreground = paintForeground ? getCellForeground(viewRow, viewColumn) : null;
                yield foreground == null ? TextAttribute.REGULAR : new TextAttribute(TextAttribute.STYLE_PLAIN, foreground);
            }
        };
        int rowLines = getRowLines();
        String text = rowLines > 1 ? String.join("\n", toDisplayLines(content.text(), rowLines)) : toSingleLine(content);
        presentation.append(text, attribute);
    }

    /**
     * The text lines a cell shows, for a frontend which paints the lines of a row itself: one line, where a line break shows as
     * {@code ⏎}, while a row shows one line; otherwise at most {@link #getRowLines()} lines, split at the line breaks of the value,
     * each one cut at {@link #MAX_DISPLAY_CHARS}, and the last one ending with {@code …} when the value has more lines.
     */
    public List<String> getCellLines(int viewRow, int viewColumn) {
        CellContent content = getCellContent(viewRow, viewColumn);
        int rowLines = getRowLines();
        return rowLines > 1 ? toDisplayLines(content.text(), rowLines) : List.of(toSingleLine(content));
    }

    private enum CellKind {
        NULL,
        RESERVED,
        FAILED,
        VALUE
    }

    /**
     * What a cell shows before it is cut to lines: the text is the whole formatted value, line breaks included.
     */
    private record CellContent(CellKind kind, String text) {
    }

    private CellContent getCellContent(int viewRow, int viewColumn) {
        Object value = getValueAt(viewRow, viewColumn);
        if (value == null || value == ReservedCellValue.NULL || value == ReservedCellValue.UNSET) {
            return new CellContent(CellKind.NULL, ReservedCellValue.NULL.getDisplayName());
        }
        if (value instanceof ReservedCellValue reserved) {
            return new CellContent(CellKind.RESERVED, reserved.getDisplayName());
        }
        if (GridUtilCore.isFailedToLoad(value)) {
            return new CellContent(CellKind.FAILED, (String) value);
        }
        GridColumn column = getColumn(viewColumn);
        if (column == null) {
            return new CellContent(CellKind.VALUE, "");
        }
        String text = getObjectFormatter().objectToString(value, column, myDisplayConfig);
        return new CellContent(CellKind.VALUE, text == null ? "null" : text);
    }

    private static String toSingleLine(CellContent content) {
        return switch (content.kind()) {
            case NULL, RESERVED -> content.text();
            case FAILED, VALUE -> toDisplayString(content.text());
        };
    }

    // region colors

    /**
     * The colors of the cells and row headers. Its first layer shows the pending changes; a data source adds its own layers, usually
     * from the configurator of the grid.
     */
    public GridColorModel getColorModel() {
        return colorModel();
    }

    /**
     * The background of a cell from the {@link #getColorModel() color model} - for example the one of a pending change - or
     * {@code null} when it keeps its background. A theme color ({@link consulo.ui.style.StyleColorValue}, such as a
     * {@link ComponentColors} entry) follows the current theme, any other color is painted as is.
     */
    public @Nullable ColorValue getCellBackground(int viewRow, int viewColumn) {
        if (!isValidCell(viewRow, viewColumn)) {
            return null;
        }
        return colorModel().getCellBackground(toModelRow(viewRow), toModelColumn(viewColumn));
    }

    /**
     * The text color of a cell from the {@link #getColorModel() color model}, or {@code null} for the default one.
     * {@link #renderCell} applies it to a value which is neither {@code null}, reserved nor failed; a selected cell keeps the
     * selection color.
     */
    public @Nullable ColorValue getCellForeground(int viewRow, int viewColumn) {
        if (!isValidCell(viewRow, viewColumn)) {
            return null;
        }
        return colorModel().getCellForeground(toModelRow(viewRow), toModelColumn(viewColumn));
    }

    /**
     * The background of the row header from the {@link #getColorModel() color model} - for example the one of a row with pending
     * changes - or {@code null} when it keeps its background.
     */
    public @Nullable ColorValue getRowHeaderBackground(int viewRow) {
        if (viewRow < 0 || viewRow >= getViewRowCount()) {
            return null;
        }
        return colorModel().getRowHeaderBackground(toModelRow(viewRow));
    }

    /**
     * The color model, whose pending changes layer follows the mutator of the data source: the layer is replaced when the data
     * source changes its mutator.
     */
    private GridColorModelImpl colorModel() {
        GridMutator.@Nullable DatabaseMutator<GridRow, GridColumn> mutator = getDatabaseMutator();
        if (mutator != myMutationsColorLayerMutator) {
            MutationsColorLayer layer = new MutationsColorLayer(mutator);
            myColorModel.removeLayer(myMutationsColorLayer);
            myColorModel.addLayer(layer);
            myMutationsColorLayer = layer;
            myMutationsColorLayerMutator = mutator;
        }
        return myColorModel;
    }

    private boolean isValidCell(int viewRow, int viewColumn) {
        return viewRow >= 0 && viewRow < getViewRowCount() && viewColumn >= 0 && viewColumn < getViewColumnCount();
    }

    // endregion

    // region row lines

    /**
     * The text lines every row shows: the lines set through {@link DataGridAppearance#setRowLines}, or, when the rows follow their
     * values ({@code 0}), the most lines of a value among the loaded rows, at most {@link #MAX_AUTO_ROW_LINES}. A frontend makes its
     * rows that many lines tall, and is told about a change with {@link View#rowHeightsChanged()}.
     */
    public int getRowLines() {
        return myRowLines;
    }

    @RequiredUIAccess
    private void refreshRowLines() {
        if (myDisposed) {
            return;
        }
        int lines = computeRowLines();
        if (lines == myRowLines) {
            return;
        }
        myRowLines = lines;
        if (myView != null) {
            myView.rowHeightsChanged();
        }
    }

    private int computeRowLines() {
        int lines = myAppearance.getRowLines();
        return lines > 0 ? lines : computeAutoRowLines();
    }

    /**
     * The most lines of a text value of the visible cells. Only text values are counted, so the formatter does not run for every
     * cell; a value of another type counts as one line.
     */
    private int computeAutoRowLines() {
        GridModel<GridRow, GridColumn> model = getMutationModel();
        int result = 1;
        for (int modelRow : myViewToModelRows) {
            ModelIndex<GridRow> rowIdx = ModelIndex.forRow(model, modelRow);
            for (int modelColumn : myViewToModelColumns) {
                if (model.getValueAt(rowIdx, ModelIndex.forColumn(model, modelColumn)) instanceof String text) {
                    result = Math.max(result, countLines(text, MAX_AUTO_ROW_LINES));
                    if (result >= MAX_AUTO_ROW_LINES) {
                        return MAX_AUTO_ROW_LINES;
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return the lines of the text, split at {@code \r\n}, {@code \r} and {@code \n}, but at most {@code max}
     */
    private static int countLines(String text, int max) {
        int lines = 1;
        int length = text.length();
        for (int i = 0; i < length && lines < max; i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                if (i + 1 < length && text.charAt(i + 1) == '\n') {
                    i++;
                }
                lines++;
            }
            else if (c == '\n') {
                lines++;
            }
        }
        return lines;
    }

    /**
     * Splits a text into the lines a cell shows: at its line breaks, at most {@code maxLines} lines, each cut at
     * {@link #MAX_DISPLAY_CHARS} and without trailing white space. The last line ends with {@code …} when the text has more lines,
     * or more text than the lines can show.
     */
    public static List<String> toDisplayLines(String text, int maxLines) {
        int max = Math.max(1, maxLines);
        // a huge value is not scanned to its end: the lines shown never need more than this
        int limit = (MAX_DISPLAY_CHARS + 2) * max;
        boolean truncated = text.length() > limit;
        String source = truncated ? text.substring(0, limit) : text;

        List<String> lines = new ArrayList<>(Math.min(max, 4));
        boolean more = false;
        int length = source.length();
        int start = 0;
        while (true) {
            int end = start;
            while (end < length && source.charAt(end) != '\n' && source.charAt(end) != '\r') {
                end++;
            }
            lines.add(toDisplayLine(source.substring(start, end)));
            if (end >= length) {
                break;
            }
            int next = end + 1;
            if (source.charAt(end) == '\r' && next < length && source.charAt(next) == '\n') {
                next++;
            }
            if (lines.size() == max) {
                more = true;
                break;
            }
            start = next;
        }

        if (more || truncated) {
            int last = lines.size() - 1;
            String line = lines.get(last);
            if (!line.endsWith("…")) {
                lines.set(last, line + "…");
            }
        }
        return lines;
    }

    private static String toDisplayLine(String line) {
        boolean cut = line.length() > MAX_DISPLAY_CHARS;
        String text = cut ? line.substring(0, MAX_DISPLAY_CHARS) : line;
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        text = text.substring(0, end);
        return cut ? text + "…" : text;
    }

    // endregion

    public static String toDisplayString(String text) {
        String cut = text.length() > MAX_DISPLAY_CHARS ? text.substring(0, MAX_DISPLAY_CHARS) : text;
        StringBuilder builder = new StringBuilder(cut.length() + 1);
        for (int i = 0; i < cut.length(); i++) {
            char c = cut.charAt(i);
            if (c == '\r') {
                if (i + 1 < cut.length() && cut.charAt(i + 1) == '\n') {
                    i++;
                }
                builder.append('⏎');
            }
            else if (c == '\n') {
                builder.append('⏎');
            }
            else {
                builder.append(c);
            }
        }
        int end = builder.length();
        while (end > 0 && Character.isWhitespace(builder.charAt(end - 1))) {
            end--;
        }
        builder.setLength(end);
        if (cut.length() < text.length()) {
            builder.append('…');
        }
        return builder.toString();
    }

    private GridModel<GridRow, GridColumn> getMutationModel() {
        return myHookUp.getMutationModel();
    }

    private void rebuildColumns() {
        int count = getMutationModel().getColumnCount();
        myHiddenColumns.removeIf(column -> column >= count);
        int[] columns = new int[count - myHiddenColumns.size()];
        int index = 0;
        for (int i = 0; i < count; i++) {
            if (!myHiddenColumns.contains(i)) {
                columns[index++] = i;
            }
        }
        myViewToModelColumns = columns;
        mySortOrders.keySet().removeIf(column -> column >= count);
        rebuildColumnKeys();
    }

    /**
     * Remembers how each column is found again after the columns change: by its name when no other column has it, otherwise by
     * its index. A name the data source made up from the position of the column ({@link ColumnDescriptor.Attribute#GENERATED_NAME})
     * names whatever column takes the position, so such a column is found by its index too - and a column move carries it along.
     */
    private void rebuildColumnKeys() {
        List<GridColumn> columns = getMutationModel().getColumns();
        Map<String, Integer> counts = new HashMap<>();
        for (GridColumn column : columns) {
            counts.merge(column.getName(), 1, Integer::sum);
        }
        List<ColumnKey> keys = new ArrayList<>(columns.size());
        Map<String, Integer> unique = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            GridColumn column = columns.get(i);
            String name = column.getName();
            boolean isUnique = !name.isEmpty()
                && counts.getOrDefault(name, 0) == 1
                && !column.getAttributes().contains(ColumnDescriptor.Attribute.GENERATED_NAME);
            keys.add(new ColumnKey(name, isUnique, i));
            if (isUnique) {
                unique.put(name, i);
            }
        }
        myColumnKeys = keys;
        myUniqueColumnNames = unique;
        myColumnNames = counts.keySet();
    }

    /**
     * A column as the selection, the hidden columns, the sort orders and the widths remember it across a change of the columns.
     *
     * @param unique whether the name tells the column apart - no other column has it, and the data source did not make it up from
     *               the position: the column is then found by its name, otherwise by its index
     */
    private record ColumnKey(String name, boolean unique, int index) {
    }

    /**
     * A width kept by the index of a column, with the name the column had then: it is not given to another column which merely
     * took the index while the named column is still there.
     *
     * @param name the name of the column, or {@code null} when its name was made up from its position and so names no column
     */
    private record WidthEntry(@Nullable String name, int chars) {
    }

    /**
     * What survives a change of the model, keyed so it is found again in the new model: the rows by their model index, the
     * columns by their {@link ColumnKey}.
     */
    private record StructureSnapshot(int[] selectedModelRows,
                                     int leadModelRow,
                                     List<ColumnKey> selectedColumns,
                                     @Nullable ColumnKey leadColumn,
                                     List<ColumnKey> hiddenColumns,
                                     List<Pair<ColumnKey, Integer>> sortOrders) {
    }

    /**
     * The snapshot of the current change of the model, taken by its first event, before the controller rebuilt anything; it is
     * dropped when the current UI task ends, and when the selection, the hidden columns or the sorting are changed meanwhile.
     */
    private StructureSnapshot structureSnapshot() {
        StructureSnapshot snapshot = myStructureSnapshot;
        if (snapshot != null) {
            return snapshot;
        }

        List<ColumnKey> selectedColumns = new ArrayList<>(mySelectedViewColumns.length);
        for (int viewColumn : mySelectedViewColumns) {
            addKey(selectedColumns, viewToModelColumn(viewColumn));
        }
        ColumnKey leadColumn = keyOf(viewToModelColumn(myLeadViewColumn));
        int leadModelRow = myLeadViewRow >= 0 && myLeadViewRow < myViewToModelRows.length ? myViewToModelRows[myLeadViewRow] : -1;

        List<ColumnKey> hiddenColumns = new ArrayList<>(myHiddenColumns.size());
        for (int modelColumn : myHiddenColumns) {
            addKey(hiddenColumns, modelColumn);
        }
        List<Pair<ColumnKey, Integer>> sortOrders = new ArrayList<>(mySortOrders.size());
        for (Map.Entry<Integer, Integer> entry : sortedByPriority()) {
            ColumnKey key = keyOf(entry.getKey());
            if (key != null) {
                sortOrders.add(Pair.create(key, entry.getValue()));
            }
        }

        StructureSnapshot newSnapshot =
            new StructureSnapshot(toModelRows(mySelectedViewRows), leadModelRow, selectedColumns, leadColumn, hiddenColumns, sortOrders);
        myStructureSnapshot = newSnapshot;
        myUIAccess.give(() -> {
            if (myStructureSnapshot == newSnapshot) {
                myStructureSnapshot = null;
            }
        });
        return newSnapshot;
    }

    private int viewToModelColumn(int viewColumn) {
        return viewColumn >= 0 && viewColumn < myViewToModelColumns.length ? myViewToModelColumns[viewColumn] : -1;
    }

    private boolean isNameGenerated(int modelColumn) {
        GridModel<GridRow, GridColumn> model = getMutationModel();
        GridColumn column = model.getColumn(ModelIndex.forColumn(model, modelColumn));
        return column != null && column.getAttributes().contains(ColumnDescriptor.Attribute.GENERATED_NAME);
    }

    private @Nullable ColumnKey keyOf(int modelColumn) {
        return modelColumn >= 0 && modelColumn < myColumnKeys.size() ? myColumnKeys.get(modelColumn) : null;
    }

    private void addKey(List<ColumnKey> keys, int modelColumn) {
        ColumnKey key = keyOf(modelColumn);
        if (key != null) {
            keys.add(key);
        }
    }

    /**
     * Finds a remembered column in the current columns.
     *
     * @param fallbackToIndex whether a column known by a name which is gone is taken at its index - for the selection, so a renamed
     *                        column stays selected; a hidden or sorted column whose name is gone is dropped instead
     * @param move            the new index of an old one, after a column move; {@code null} when no column moved
     * @return the model column, or {@code -1}
     */
    private int resolveColumn(ColumnKey key, boolean fallbackToIndex, @Nullable IntUnaryOperator move) {
        if (key.unique()) {
            Integer index = myUniqueColumnNames.get(key.name());
            if (index != null) {
                return index;
            }
            if (!fallbackToIndex) {
                return -1;
            }
        }
        int index = move == null ? key.index() : move.applyAsInt(key.index());
        return index >= 0 && index < myColumnKeys.size() ? index : -1;
    }

    /**
     * Puts the hidden columns and the sort orders of the snapshot back onto the current columns; the view columns are rebuilt
     * after it.
     */
    private void restoreColumnAttributes(StructureSnapshot snapshot, @Nullable IntUnaryOperator move) {
        myHiddenColumns.clear();
        for (ColumnKey key : snapshot.hiddenColumns()) {
            int modelColumn = resolveColumn(key, false, move);
            if (modelColumn >= 0) {
                myHiddenColumns.add(modelColumn);
            }
        }
        mySortOrders.clear();
        for (Pair<ColumnKey, Integer> sortOrder : snapshot.sortOrders()) {
            int modelColumn = resolveColumn(sortOrder.getFirst(), false, move);
            if (modelColumn >= 0 && !mySortOrders.containsKey(modelColumn)) {
                mySortOrders.put(modelColumn, sortOrder.getSecond());
            }
        }
        renumberSortPriorities();
    }

    /**
     * Puts the selection of the snapshot back onto the current rows and view columns.
     */
    private void restoreSelection(StructureSnapshot snapshot, @Nullable IntUnaryOperator move) {
        mySelectedViewRows = toViewRows(snapshot.selectedModelRows());
        int leadModelRow = snapshot.leadModelRow();
        myLeadViewRow = leadModelRow >= 0 && leadModelRow < myModelToViewRows.length ? myModelToViewRows[leadModelRow] : -1;

        mySelectedViewColumns = snapshot.selectedColumns().stream()
            .mapToInt(key -> modelToViewColumn(resolveColumn(key, true, move)))
            .filter(view -> view >= 0)
            .distinct()
            .sorted()
            .toArray();
        ColumnKey leadColumn = snapshot.leadColumn();
        myLeadViewColumn = leadColumn == null ? -1 : modelToViewColumn(resolveColumn(leadColumn, true, move));
    }

    private int modelToViewColumn(int modelColumn) {
        return modelColumn < 0 ? -1 : toViewColumn(ModelIndex.forColumn(getMutationModel(), modelColumn));
    }

    private void rebuildRows() {
        GridModel<GridRow, GridColumn> model = getMutationModel();
        int count = model.getRowCount();
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }

        if (!mySortOrders.isEmpty() && !isSortingViaModel()) {
            Comparator<Integer> comparator = null;
            for (Map.Entry<Integer, Integer> entry : sortedByPriority()) {
                GridColumn column = model.getColumn(ModelIndex.forColumn(model, entry.getKey()));
                if (column == null) {
                    continue;
                }
                GridRowComparator rowComparator = GridRowComparator.create(column);
                if (rowComparator == null) {
                    continue;
                }
                ModelIndex<GridColumn> columnIndex = ModelIndex.forColumn(model, entry.getKey());
                Comparator<Integer> byColumn = (a, b) -> rowComparator.compareObjects(
                    model.getValueAt(ModelIndex.forRow(model, a), columnIndex),
                    model.getValueAt(ModelIndex.forRow(model, b), columnIndex));
                if (entry.getValue() > 0) {
                    byColumn = byColumn.reversed();
                }
                comparator = comparator == null ? byColumn : comparator.thenComparing(byColumn);
            }
            if (comparator != null) {
                Arrays.sort(order, comparator);
            }
        }

        int[] viewToModel = new int[count];
        int[] modelToView = new int[count];
        for (int view = 0; view < count; view++) {
            viewToModel[view] = order[view];
            modelToView[order[view]] = view;
        }
        myViewToModelRows = viewToModel;
        myModelToViewRows = modelToView;
    }

    /**
     * The columns of the data source changed. The selection, the hidden columns and the sort orders are put back onto the columns
     * which remain, found by their name, or else by their index; the widths are looked up the same way
     * ({@link #getViewColumnWidth}).
     */
    @RequiredUIAccess
    private void onColumnsChanged() {
        cancelEditIfAny();
        clearContextMenuTarget();
        StructureSnapshot snapshot = structureSnapshot();
        rebuildColumns();
        restoreColumnAttributes(snapshot, null);
        rebuildColumns();
        rebuildRows();
        restoreSelection(snapshot, null);
        if (myView != null) {
            myView.structureChanged();
            myView.rowsChanged();
            myView.selectionChanged();
        }
        refreshRowLines();
        updateToolbar();
    }

    /**
     * The rows of the data source, or their order, changed: the selection follows its rows by their model index. A data source which
     * removes every row and adds new ones within one UI task gets its selection back once the rows are there again.
     */
    @RequiredUIAccess
    private void onRowsChanged(GridRequestSource.@Nullable RequestPlace place) {
        cancelEditIfAny();
        StructureSnapshot snapshot = structureSnapshot();
        rebuildRows();
        restoreSelection(snapshot, null);
        if (myView != null) {
            myView.rowsChanged();
            myView.selectionChanged();
        }
        refreshRowLines();
        updateToolbar();
        fireContentChanged(place);
    }

    /**
     * An open edit is cancelled before the view reloads its rows or columns: a native table drops, or loses, its editor when its
     * rows or columns are replaced.
     */
    @RequiredUIAccess
    private void cancelEditIfAny() {
        myPendingEdit = null;
        if (myEditSession != null) {
            cancelEditing();
        }
    }

    @RequiredUIAccess
    private void onCellsUpdated(ModelIndexSet<GridRow> rows, GridRequestSource.@Nullable RequestPlace place) {
        if (!mySortOrders.isEmpty() && !isSortingViaModel()) {
            onRowsChanged(place);
            return;
        }
        updateToolbar();
        if (myView != null && rows.size() > 0) {
            int first = Integer.MAX_VALUE;
            int last = -1;
            for (int model : rows.asArray()) {
                int view = model >= 0 && model < myModelToViewRows.length ? myModelToViewRows[model] : -1;
                if (view >= 0) {
                    first = Math.min(first, view);
                    last = Math.max(last, view);
                }
            }
            if (last >= 0) {
                myView.cellsChanged(first, last);
            }
        }
        refreshRowLines();
        fireContentChanged(place);
    }

    private int[] toModelRows(int[] viewRows) {
        int[] result = new int[viewRows.length];
        for (int i = 0; i < viewRows.length; i++) {
            int view = viewRows[i];
            result[i] = view >= 0 && view < myViewToModelRows.length ? myViewToModelRows[view] : -1;
        }
        return result;
    }

    private int[] toViewRows(int[] modelRows) {
        List<Integer> result = new ArrayList<>(modelRows.length);
        for (int model : modelRows) {
            int view = model >= 0 && model < myModelToViewRows.length ? myModelToViewRows[model] : -1;
            if (view >= 0) {
                result.add(view);
            }
        }
        return result.stream().mapToInt(Integer::intValue).sorted().toArray();
    }

    /**
     * Runs a callback which may arrive on any thread - a model event, a completed request callback, the answer of a dialog - on
     * the UI thread of the grid: at once when it is there already, otherwise through the {@link UIAccess} the grid was created
     * with.
     */
    private void onUIThread(@RequiredUIAccess Runnable runnable) {
        if (myDisposed) {
            return;
        }
        if (UIAccess.isUIThread()) {
            runnable.run();
        }
        else {
            myUIAccess.give(() -> {
                if (!myDisposed) {
                    runnable.run();
                }
            });
        }
    }

    // endregion

    // region sorting

    public RowSortOrder.Type getSortOrder(ModelIndex<GridColumn> column) {
        int order = mySortOrders.getOrDefault(column.asInteger(), 0);
        return order < 0 ? RowSortOrder.Type.ASC : order > 0 ? RowSortOrder.Type.DESC : RowSortOrder.Type.UNSORTED;
    }

    public int getThenBySortOrder(ModelIndex<GridColumn> column) {
        return Math.abs(mySortOrders.getOrDefault(column.asInteger(), 0));
    }

    public int countSortedColumns() {
        return mySortOrders.size();
    }

    public TreeMap<Integer, GridColumn> getSortOrderMap() {
        TreeMap<Integer, GridColumn> result = new TreeMap<>();
        GridModel<GridRow, GridColumn> model = getMutationModel();
        for (Map.Entry<Integer, Integer> entry : mySortOrders.entrySet()) {
            GridColumn column = model.getColumn(ModelIndex.forColumn(model, entry.getKey()));
            if (column != null) {
                result.put(entry.getValue(), column);
            }
        }
        return result;
    }

    /**
     * Cycles the sorting of the columns: ascending, then descending, then unsorted. A click which does not add to the
     * sorting, on a column which is not the only sorted one, starts again from ascending.
     */
    @RequiredUIAccess
    public void toggleSortColumns(List<ModelIndex<GridColumn>> columns, boolean additive) {
        if (columns.isEmpty()) {
            return;
        }
        ModelIndex<GridColumn> first = columns.get(0);
        boolean onlySorted = mySortOrders.size() == 1 && mySortOrders.containsKey(first.asInteger());
        RowSortOrder.Type current = getSortOrder(first);
        RowSortOrder.Type next;
        if (!additive && !onlySorted) {
            next = RowSortOrder.Type.ASC;
        }
        else {
            next = switch (current) {
                case ASC -> RowSortOrder.Type.DESC;
                case DESC -> RowSortOrder.Type.UNSORTED;
                case UNSORTED -> RowSortOrder.Type.ASC;
            };
        }
        sortColumns(columns, next, additive);
    }

    @RequiredUIAccess
    public void sortColumns(List<ModelIndex<GridColumn>> columns, RowSortOrder.Type order, boolean additive) {
        myStructureSnapshot = null;
        if (!additive) {
            mySortOrders.clear();
        }
        for (ModelIndex<GridColumn> column : columns) {
            int key = column.asInteger();
            if (order == RowSortOrder.Type.UNSORTED) {
                mySortOrders.remove(key);
            }
            else {
                int existing = mySortOrders.getOrDefault(key, 0);
                int priority = existing != 0 ? Math.abs(existing) : mySortOrders.size() + 1;
                mySortOrders.put(key, order == RowSortOrder.Type.ASC ? -priority : priority);
            }
        }
        renumberSortPriorities();
        applySorting();
    }

    @RequiredUIAccess
    public void resetSorting() {
        if (mySortOrders.isEmpty()) {
            return;
        }
        myStructureSnapshot = null;
        mySortOrders.clear();
        applySorting();
    }

    private void renumberSortPriorities() {
        List<Map.Entry<Integer, Integer>> entries = sortedByPriority();
        mySortOrders.clear();
        int priority = 1;
        for (Map.Entry<Integer, Integer> entry : entries) {
            mySortOrders.put(entry.getKey(), entry.getValue() < 0 ? -priority : priority);
            priority++;
        }
    }

    private List<Map.Entry<Integer, Integer>> sortedByPriority() {
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(mySortOrders.entrySet());
        entries.sort(Comparator.comparingInt(entry -> Math.abs(entry.getValue())));
        return entries;
    }

    private boolean isSortingViaModel() {
        GridSortingModel<GridRow, GridColumn> sortingModel = myHookUp.getSortingModel();
        return sortingModel != null && sortingModel.isSortingEnabled();
    }

    @RequiredUIAccess
    private void applySorting() {
        if (myView != null) {
            myView.headersChanged();
        }
        GridSortingModel<GridRow, GridColumn> sortingModel = myHookUp.getSortingModel();
        if (sortingModel != null && sortingModel.isSortingEnabled()) {
            List<RowSortOrder<ModelIndex<GridColumn>>> ordering = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : sortedByPriority()) {
                ModelIndex<GridColumn> column = ModelIndex.forColumn(getMutationModel(), entry.getKey());
                ordering.add(entry.getValue() < 0 ? RowSortOrder.asc(column) : RowSortOrder.desc(column));
            }
            sortingModel.setOrdering(ordering);
            loadFirstPage();
        }
        else {
            onRowsChanged(null);
        }
    }

    // endregion

    // region columns visibility

    public boolean isColumnEnabled(ModelIndex<GridColumn> column) {
        return !myHiddenColumns.contains(column.asInteger());
    }

    @RequiredUIAccess
    public void setColumnEnabled(ModelIndex<GridColumn> column, boolean state) {
        boolean changed = state ? myHiddenColumns.remove(column.asInteger()) : myHiddenColumns.add(column.asInteger());
        if (changed) {
            myStructureSnapshot = null;
            clearContextMenuTarget();
            cancelEditIfAny();
            rebuildColumns();
            mySelectedViewColumns = new int[0];
            myLeadViewColumn = -1;
            if (myView != null) {
                myView.structureChanged();
                myView.rowsChanged();
            }
            refreshRowLines();
        }
    }

    @RequiredUIAccess
    public void resetView() {
        myStructureSnapshot = null;
        cancelEditIfAny();
        boolean hadHidden = !myHiddenColumns.isEmpty();
        myHiddenColumns.clear();
        mySortOrders.clear();
        if (hadHidden) {
            clearContextMenuTarget();
            rebuildColumns();
            myLeadViewColumn = -1;
            if (myView != null) {
                myView.structureChanged();
            }
            refreshRowLines();
        }
        applySorting();
    }

    public boolean isViewModified() {
        return !mySortOrders.isEmpty() || !myHiddenColumns.isEmpty();
    }

    // endregion

    // region column widths

    /**
     * The width of a column in characters of the grid font, or {@code 0} when the grid has none for it - the frontend sizes the
     * column itself then. A width is kept by the name of the column, and by its index for a column whose name is not unique or
     * changed, so it survives a change of the columns of the data source.
     */
    public int getColumnWidth(ModelIndex<GridColumn> column) {
        int modelColumn = column.asInteger();
        ColumnKey key = keyOf(modelColumn);
        if (key == null) {
            return 0;
        }
        if (key.unique()) {
            Integer chars = myWidthsByName.get(key.name());
            if (chars != null) {
                return chars;
            }
        }
        WidthEntry entry = myWidthsByIndex.get(modelColumn);
        if (entry == null) {
            return 0;
        }
        // not the width of a column which is still there under its name, at another index
        String entryName = entry.name();
        boolean sameColumn = entryName == null || entryName.equals(key.name()) || !myColumnNames.contains(entryName);
        return sameColumn ? entry.chars() : 0;
    }

    /**
     * {@link #getColumnWidth} of a view column, for the frontend.
     */
    public int getViewColumnWidth(int viewColumn) {
        return getColumnWidth(toModelColumn(viewColumn));
    }

    /**
     * {@link DataGrid#setColumnWidth}: the frontend is told with {@link View#columnWidthsChanged()}.
     */
    @RequiredUIAccess
    public void setColumnWidth(ModelIndex<GridColumn> column, int chars) {
        if (!storeWidth(column.asInteger(), chars)) {
            return;
        }
        if (myView != null) {
            myView.columnWidthsChanged();
        }
    }

    /**
     * The frontend reports the width of a view column which the user resized - or which it sized itself, once its rows are loaded.
     * The frontend is not told back.
     *
     * @param chars the width in characters of the grid font: the width in pixels divided by the average character width of the
     *              grid font, without the padding of the cell
     */
    @RequiredUIAccess
    public void onColumnResized(int viewColumn, int chars) {
        storeWidth(viewToModelColumn(viewColumn), chars);
    }

    /**
     * {@link DataGrid#fitColumnWidths}: every visible column gets the width of its longest line among the header and the loaded
     * rows, as the cells show it ({@link #getCellLines}), and the header keeps room for its sort marker.
     *
     * @param maxChars the widest a column gets, or {@code 0} for no limit
     */
    @RequiredUIAccess
    public void fitColumnWidths(int maxChars) {
        for (int viewColumn = 0; viewColumn < getViewColumnCount(); viewColumn++) {
            storeWidth(myViewToModelColumns[viewColumn], computeFitWidth(viewColumn, maxChars));
        }
        if (myView != null) {
            myView.columnWidthsChanged();
        }
    }

    /**
     * The width {@link #fitColumnWidths} gives a view column, in characters of the grid font, at least {@code 1}.
     *
     * @param maxChars the widest the column gets, or {@code 0} for no limit
     */
    public int computeFitWidth(int viewColumn, int maxChars) {
        GridColumn column = getColumn(viewColumn);
        int chars = 0;
        if (column != null) {
            chars = textWidth(getName(column)) + (isColumnSortable(viewColumn) ? SORT_MARKER_CHARS : 0);
        }
        int rowCount = getViewRowCount();
        for (int viewRow = 0; viewRow < rowCount && (maxChars <= 0 || chars < maxChars); viewRow++) {
            for (String line : getCellLines(viewRow, viewColumn)) {
                chars = Math.max(chars, textWidth(line));
            }
        }
        chars = Math.max(1, chars);
        return maxChars > 0 ? Math.min(chars, maxChars) : chars;
    }

    private static int textWidth(String text) {
        return text.codePointCount(0, text.length());
    }

    /**
     * @return whether the column exists, so the width was kept
     */
    private boolean storeWidth(int modelColumn, int chars) {
        ColumnKey key = keyOf(modelColumn);
        if (key == null) {
            return false;
        }
        int width = Math.max(1, chars);
        if (key.unique()) {
            myWidthsByName.put(key.name(), width);
        }
        myWidthsByIndex.put(modelColumn, new WidthEntry(isNameGenerated(modelColumn) ? null : key.name(), width));
        return true;
    }

    // endregion

    // region context menu

    /**
     * The frontend reports the gesture which asks for a context menu, before it fires the {@link consulo.ui.event.ContextMenuEvent}
     * of the grid, whose listener shows the menu. The cell, the row or the column it was opened on is selected unless it is
     * selected already - a row header selects the whole row, a column header the whole column - so the actions of the menu act on
     * what it was opened on.
     * <p/>
     * The open edit is committed first, as the actions of the menu act on the values. When the commit is refused, or the grid waits
     * for the user's answer about the edit, the editor stays open with its value, nothing is selected, and no menu opens: the
     * frontend does not fire the event then.
     * <p/>
     * The area and the column are kept for the actions ({@link #getContextArea()}, {@link #getContextColumn()}) until the next
     * gesture on the grid: another context menu, a click on a header, a column move, a change of the columns, or a selection which
     * differs from the one the menu was opened with.
     *
     * @param viewRow    the row under the pointer, or {@code -1} for a column header or the empty part of the grid
     * @param viewColumn the column under the pointer, or {@code -1} for a row header or the empty part of the grid
     * @return whether the menu opens: {@code false} while the open edit could not be committed
     */
    @RequiredUIAccess
    public boolean onContextMenuRequested(GridHitArea area, int viewRow, int viewColumn) {
        DataGridEditSession session = myEditSession;
        if (session != null && (session.isWaitingForAnswer() || !stopEditing())) {
            return false;
        }

        boolean validRow = viewRow >= 0 && viewRow < getViewRowCount();
        boolean validColumn = viewColumn >= 0 && viewColumn < getViewColumnCount();
        GridHitArea hitArea = switch (area) {
            case CELL -> validRow && validColumn ? GridHitArea.CELL : GridHitArea.EMPTY;
            case ROW_HEADER -> validRow ? GridHitArea.ROW_HEADER : GridHitArea.EMPTY;
            case COLUMN_HEADER -> validColumn ? GridHitArea.COLUMN_HEADER : GridHitArea.EMPTY;
            case EMPTY -> GridHitArea.EMPTY;
        };

        switch (hitArea) {
            case CELL -> {
                if (!(contains(mySelectedViewRows, viewRow) && contains(mySelectedViewColumns, viewColumn))) {
                    setSelectionInner(new int[]{viewRow}, new int[]{viewColumn}, viewRow, viewColumn);
                }
            }
            case ROW_HEADER -> {
                if (!contains(mySelectedViewRows, viewRow)) {
                    int[] columns = allViewColumns();
                    setSelectionInner(new int[]{viewRow}, columns, viewRow, columns.length == 0 ? -1 : 0);
                }
            }
            case COLUMN_HEADER -> {
                if (!contains(mySelectedViewColumns, viewColumn)) {
                    int[] rows = allViewRows();
                    setSelectionInner(rows, new int[]{viewColumn}, rows.length == 0 ? -1 : 0, viewColumn);
                }
            }
            case EMPTY -> {
            }
        }

        myContextArea = hitArea;
        myContextModelColumn = hitArea == GridHitArea.CELL || hitArea == GridHitArea.COLUMN_HEADER
            ? viewToModelColumn(viewColumn)
            : -1;
        myContextViewRows = mySelectedViewRows.clone();
        myContextViewColumns = mySelectedViewColumns.clone();
        return true;
    }

    /**
     * {@link DataGrid#getContextArea()}: the part of the grid the last context menu was opened on, {@link GridHitArea#EMPTY} when
     * none is open, or the gesture is over.
     */
    public GridHitArea getContextArea() {
        return myContextArea;
    }

    /**
     * {@link DataGrid#getContextColumn()}: the column of the cell or column header the last context menu was opened on, or an index
     * of {@code -1}.
     */
    public ModelIndex<GridColumn> getContextColumn() {
        return ModelIndex.forColumn(getMutationModel(), myContextModelColumn);
    }

    /**
     * Forgets what the last context menu was opened on; the code which shows the menu may call it once the menu is closed.
     */
    public void clearContextMenuTarget() {
        myContextArea = GridHitArea.EMPTY;
        myContextModelColumn = -1;
        myContextViewRows = new int[0];
        myContextViewColumns = new int[0];
    }

    /**
     * Commits the open edit, or drops it when the commit is refused, before a gesture moves the selection away from it.
     *
     * @return {@code false} while the grid waits for the user's answer about the edit; the selection stays then
     */
    @RequiredUIAccess
    private boolean stopOrCancelEditing() {
        if (stopEditing()) {
            return true;
        }
        DataGridEditSession session = myEditSession;
        if (session != null && session.isWaitingForAnswer()) {
            return false;
        }
        cancelEditing();
        return true;
    }

    // endregion

    // region column move

    /**
     * Whether the user may drag a column to another place: the data source is editable, can move its columns, and no long request
     * of it runs. A frontend enables the dragging of its column headers by it.
     */
    public boolean canMoveColumns() {
        return isEditable() && myHookUp.getMutator() instanceof GridMutator.ColumnsMutator && myGrid.isReady() && !isEditingBlocked();
    }

    /**
     * The user dropped a dragged column at another place. The move is sent to the data source
     * ({@link GridMutator.ColumnsMutator#moveColumn} with a {@link MoveColumnsRequestPlace}); once the data source has the new order,
     * the view columns follow it through {@link View#structureChanged()}, and the widths, the selection, the hidden columns and the
     * sort orders move along. Editing is blocked while the data source moves the column. When the data source refuses the move, the
     * view columns are rebuilt in the order of the data source.
     *
     * @return {@code false} when the column is not moved - the frontend puts its column back at once; {@code true} when the move was
     * sent, and the frontend may keep its view of the move until the next {@link View#structureChanged()}
     */
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public boolean onColumnMoved(int fromViewColumn, int toViewColumn) {
        clearContextMenuTarget();
        int columnCount = getViewColumnCount();
        if (fromViewColumn == toViewColumn
            || fromViewColumn < 0 || fromViewColumn >= columnCount
            || toViewColumn < 0 || toViewColumn >= columnCount
            || !canMoveColumns()) {
            return false;
        }
        DataGridEditSession session = myEditSession;
        if (session != null && session.isWaitingForAnswer()) {
            return false;
        }

        if (!(myHookUp.getMutator() instanceof GridMutator.ColumnsMutator<?, ?> columnsMutator)) {
            return false;
        }
        GridMutator.ColumnsMutator<GridRow, GridColumn> mutator = (GridMutator.ColumnsMutator<GridRow, GridColumn>) columnsMutator;
        ModelIndex<GridColumn> from = toModelColumn(fromViewColumn);
        ModelIndex<GridColumn> to = toModelColumn(toViewColumn);
        int fromModel = from.asInteger();
        int toModel = to.asInteger();

        boolean wasEditing = isEditing();
        stopOrCancelEditing();

        Runnable send = () -> {
            MoveColumnsRequestPlace place = new MoveColumnsRequestPlace(myGrid, this::openLongAction,
                () -> onUIThread(() -> adjustColumnsAfterMove(fromModel, toModel)));
            GridRequestSource source = new GridRequestSource(place);
            source.getActionCallback().doWhenRejected(() -> onUIThread(this::onColumnMoveRejected));
            mutator.moveColumn(source, from, to);
        };
        if (wasEditing) {
            // a committed value reaches the mutator later (setValueAt), at the old indices of the columns - move after it
            myPendingColumnMoves++;
            myUIAccess.give(() -> {
                myPendingColumnMoves--;
                if (!myDisposed) {
                    send.run();
                }
            });
        }
        else {
            send.run();
        }
        return true;
    }

    /**
     * Whether editing is blocked: a column move is on its way to the data source, or a long request of it runs
     * ({@link LongActionRequestPlace}). No edit starts meanwhile.
     */
    public boolean isEditingBlocked() {
        return myPendingColumnMoves > 0 || myLongActionCount > 0;
    }

    /**
     * The loading state of {@link LongActionRequestPlace#getLoadingUI()}: the grid is busy and editing is blocked until the returned
     * value is closed. It may be opened and closed on any thread.
     */
    private AutoCloseable openLongAction() {
        onUIThread(() -> {
            myLongActionCount++;
            cancelEditIfAny();
            updateToolbar();
        });
        AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                onUIThread(() -> {
                    myLongActionCount = Math.max(0, myLongActionCount - 1);
                    updateToolbar();
                });
            }
        };
    }

    /**
     * {@link MoveColumnsRequestPlace#adjustColumnsUI()}: the data source has the new order of the columns. What was kept by the index
     * of a column - the widths of columns without a unique name, and the selection, hidden columns and sort orders of the change
     * of the columns which is still in progress - moves along with the column.
     */
    @RequiredUIAccess
    private void adjustColumnsAfterMove(int from, int to) {
        if (myDisposed) {
            return;
        }
        IntUnaryOperator move = index -> movedIndex(index, from, to);

        Map<Integer, WidthEntry> widths = new HashMap<>();
        for (Map.Entry<Integer, WidthEntry> entry : myWidthsByIndex.entrySet()) {
            widths.put(move.applyAsInt(entry.getKey()), entry.getValue());
        }
        myWidthsByIndex.clear();
        myWidthsByIndex.putAll(widths);

        StructureSnapshot snapshot = myStructureSnapshot;
        View view = myView;
        if (snapshot == null) {
            // the change of the columns is over: what was found by name stays, the widths follow
            if (view != null) {
                view.columnWidthsChanged();
            }
            return;
        }

        cancelEditIfAny();
        rebuildColumns();
        restoreColumnAttributes(snapshot, move);
        rebuildColumns();
        rebuildRows();
        restoreSelection(snapshot, move);
        if (view != null) {
            view.structureChanged();
            view.rowsChanged();
            view.selectionChanged();
        }
        refreshRowLines();
        updateToolbar();
    }

    /**
     * @return the index a column at {@code index} has once the column at {@code from} moved to {@code to}
     */
    private static int movedIndex(int index, int from, int to) {
        if (index == from) {
            return to;
        }
        if (from < to && index > from && index <= to) {
            return index - 1;
        }
        if (from > to && index >= to && index < from) {
            return index + 1;
        }
        return index;
    }

    /**
     * The data source refused the move: the view columns are rebuilt in its order, which undoes the move a frontend kept.
     */
    @RequiredUIAccess
    private void onColumnMoveRejected() {
        View view = myView;
        if (view != null) {
            view.structureChanged();
            view.rowsChanged();
            view.selectionChanged();
        }
    }

    // endregion

    // region selection

    public int[] getSelectedViewRows() {
        return mySelectedViewRows.clone();
    }

    public int[] getSelectedViewColumns() {
        return mySelectedViewColumns.clone();
    }

    /**
     * The frontend reports what the user selected in its native table. The lead cell - which {@link #editSelectedCell()} edits -
     * is then the first selected cell.
     */
    @RequiredUIAccess
    public void onNativeSelectionChanged(int[] viewRows, int[] viewColumns, boolean adjusting) {
        onNativeSelectionChanged(viewRows, viewColumns, adjusting, -1, -1);
    }

    /**
     * The frontend reports what the user selected in its native table, and its lead cell: the cell the selection was last
     * extended to, which {@link #editSelectedCell()} edits.
     */
    @RequiredUIAccess
    public void onNativeSelectionChanged(int[] viewRows, int[] viewColumns, boolean adjusting, int leadViewRow, int leadViewColumn) {
        // a frontend which applied the selection of the grid may report it back - that is not a change by the user
        if (!sameIndices(viewRows, mySelectedViewRows) || !sameIndices(viewColumns, mySelectedViewColumns)) {
            myStructureSnapshot = null;
        }
        if (!sameIndices(viewRows, myContextViewRows) || !sameIndices(viewColumns, myContextViewColumns)) {
            clearContextMenuTarget();
        }
        mySelectedViewRows = viewRows.clone();
        mySelectedViewColumns = viewColumns.clone();
        myLeadViewRow = leadViewRow;
        myLeadViewColumn = leadViewColumn;
        onSelectionChangedInner(adjusting);
    }

    private static boolean sameIndices(int[] a, int[] b) {
        if (a.length != b.length) {
            return false;
        }
        int[] sortedA = a.clone();
        int[] sortedB = b.clone();
        Arrays.sort(sortedA);
        Arrays.sort(sortedB);
        return Arrays.equals(sortedA, sortedB);
    }

    @RequiredUIAccess
    private void setSelectionInner(int[] viewRows, int[] viewColumns) {
        setSelectionInner(viewRows, viewColumns, -1, -1);
    }

    /**
     * Sets the selection of the grid and tells the frontend. A selection which is set replaces the one a change of the model in
     * progress would put back ({@link #structureSnapshot()}).
     */
    @RequiredUIAccess
    private void setSelectionInner(int[] viewRows, int[] viewColumns, int leadViewRow, int leadViewColumn) {
        myStructureSnapshot = null;
        mySelectedViewRows = viewRows;
        mySelectedViewColumns = viewColumns;
        myLeadViewRow = leadViewRow;
        myLeadViewColumn = leadViewColumn;
        if (myView != null) {
            myView.selectionChanged();
        }
        onSelectionChangedInner(false);
    }

    @RequiredUIAccess
    private void onSelectionChangedInner(boolean adjusting) {
        PendingEdit pendingEdit = myPendingEdit;
        if (pendingEdit != null && !pendingEdit.isFor(toModelRow(getLeadViewRow()), toModelColumn(getLeadViewColumn()))) {
            myPendingEdit = null;
        }
        if (!adjusting) {
            updateEditButtons();
        }
        for (DataGridListener listener : myListeners) {
            listener.onSelectionChanged(myGrid, adjusting);
        }
    }

    /**
     * The view row of the lead cell, or {@code -1} when nothing is selected.
     */
    public int getLeadViewRow() {
        if (contains(mySelectedViewRows, myLeadViewRow)) {
            return myLeadViewRow;
        }
        return mySelectedViewRows.length == 0 ? -1 : mySelectedViewRows[0];
    }

    /**
     * The view column of the lead cell, or {@code -1} when nothing is selected.
     */
    public int getLeadViewColumn() {
        if (contains(mySelectedViewColumns, myLeadViewColumn)) {
            return myLeadViewColumn;
        }
        return mySelectedViewColumns.length == 0 ? -1 : mySelectedViewColumns[0];
    }

    private int[] allViewColumns() {
        int[] result = new int[getViewColumnCount()];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return result;
    }

    private int[] allViewRows() {
        int[] result = new int[getViewRowCount()];
        for (int i = 0; i < result.length; i++) {
            result[i] = i;
        }
        return result;
    }

    /**
     * @return {@code from}, {@code from + 1}, ... {@code to}
     */
    private static int[] range(int from, int to) {
        return IntStream.rangeClosed(from, to).toArray();
    }

    private static int[] union(int[] a, int[] b) {
        return IntStream.concat(Arrays.stream(a), Arrays.stream(b)).distinct().sorted().toArray();
    }

    private int[] viewRowsOf(ModelIndexSet<GridRow> rows) {
        return Arrays.stream(rows.asArray()).map(model -> toViewRow(ModelIndex.forRow(getMutationModel(), model)))
            .filter(view -> view >= 0).distinct().sorted().toArray();
    }

    private int[] viewColumnsOf(ModelIndexSet<GridColumn> columns) {
        return Arrays.stream(columns.asArray()).map(model -> toViewColumn(ModelIndex.forColumn(getMutationModel(), model)))
            .filter(view -> view >= 0).distinct().sorted().toArray();
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    private ModelIndexSet<GridRow> modelRowsOf(int[] viewRows) {
        return ModelIndexSet.forRows(getMutationModel(), toModelRows(viewRows));
    }

    private ModelIndexSet<GridColumn> modelColumnsOf(int[] viewColumns) {
        int[] result = new int[viewColumns.length];
        for (int i = 0; i < viewColumns.length; i++) {
            result[i] = toModelColumn(viewColumns[i]).asInteger();
        }
        return ModelIndexSet.forColumns(getMutationModel(), result);
    }

    public SelectionModel<GridRow, GridColumn> getSelectionModel() {
        return mySelectionModel;
    }

    private record GridSelectionImpl(ModelIndexSet<GridRow> rows,
                                     ModelIndexSet<GridColumn> columns) implements GridSelection<GridRow, GridColumn> {
        @Override
        public void addSelectedColumns(CoreGrid<GridRow, GridColumn> grid, ModelIndexSet<GridColumn> additionalColumns) {
            // a stored selection is a snapshot - the columns are added to the live selection of the grid
            int[] merged = merge(columns.asArray(), additionalColumns.asArray());
            grid.getSelectionModel().setSelection(rows, ModelIndexSet.forColumns(grid, merged));
        }

        @Override
        public ModelIndexSet<GridRow> getSelectedRows() {
            return rows;
        }

        @Override
        public ModelIndexSet<GridColumn> getSelectedColumns() {
            return columns;
        }

        private static int[] merge(int[] a, int[] b) {
            return IntStream.concat(Arrays.stream(a), Arrays.stream(b)).distinct().sorted().toArray();
        }
    }

    private static final GridSelectionTracker NO_TRACKER = new GridSelectionTracker() {
        @Override
        public void performOperation(Operation operation) {
        }

        @Override
        public boolean canPerformOperation(Operation operation) {
            return false;
        }
    };

    private final class SelectionModelImpl implements SelectionModel<GridRow, GridColumn> {
        @Override
        public GridSelection<GridRow, GridColumn> store() {
            return new GridSelectionImpl(modelRowsOf(mySelectedViewRows), modelColumnsOf(mySelectedViewColumns));
        }

        @Override
        @RequiredUIAccess
        public void restore(GridSelection<GridRow, GridColumn> selection) {
            setSelection(selection.getSelectedRows(), selection.getSelectedColumns());
        }

        @Override
        public GridSelection<GridRow, GridColumn> fit(GridSelection<GridRow, GridColumn> selection) {
            return new GridSelectionImpl(modelRowsOf(viewRowsOf(selection.getSelectedRows())),
                modelColumnsOf(viewColumnsOf(selection.getSelectedColumns())));
        }

        @Override
        public GridSelectionTracker getTracker() {
            return NO_TRACKER;
        }

        @Override
        @RequiredUIAccess
        public void setSelection(ModelIndexSet<GridRow> rows, ModelIndexSet<GridColumn> columns) {
            setSelectionInner(viewRowsOf(rows), viewColumnsOf(columns));
        }

        @Override
        @RequiredUIAccess
        public void setSelection(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
            int viewRow = toViewRow(row);
            int viewColumn = toViewColumn(column);
            setSelectionInner(viewRow >= 0 ? new int[]{viewRow} : new int[0], viewColumn >= 0 ? new int[]{viewColumn} : new int[0]);
        }

        @Override
        @RequiredUIAccess
        public void setRowSelection(ModelIndexSet<GridRow> selection, boolean selectAtLeastOneCell) {
            setSelectionInner(viewRowsOf(selection), allViewColumns());
        }

        @Override
        @RequiredUIAccess
        public void setRowSelection(ModelIndex<GridRow> selection, boolean selectAtLeastOneCell) {
            int viewRow = toViewRow(selection);
            setSelectionInner(viewRow >= 0 ? new int[]{viewRow} : new int[0], allViewColumns());
        }

        @Override
        @RequiredUIAccess
        public void addRowSelection(ModelIndexSet<GridRow> selection) {
            int[] rows = IntStream.concat(Arrays.stream(mySelectedViewRows), Arrays.stream(viewRowsOf(selection)))
                .distinct().sorted().toArray();
            setSelectionInner(rows, mySelectedViewColumns.length == 0 ? allViewColumns() : mySelectedViewColumns);
        }

        @Override
        @RequiredUIAccess
        public void setColumnSelection(ModelIndexSet<GridColumn> selection, boolean selectAtLeastOneCell) {
            setSelectionInner(allViewRows(), viewColumnsOf(selection));
        }

        @Override
        @RequiredUIAccess
        public void setColumnSelection(ModelIndex<GridColumn> selection, boolean selectAtLeastOneCell) {
            int viewColumn = toViewColumn(selection);
            setSelectionInner(allViewRows(), viewColumn >= 0 ? new int[]{viewColumn} : new int[0]);
        }

        @Override
        public boolean isSelectionEmpty() {
            return mySelectedViewRows.length == 0 || mySelectedViewColumns.length == 0;
        }

        @Override
        public boolean isSelected(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
            return contains(mySelectedViewRows, toViewRow(row)) && contains(mySelectedViewColumns, toViewColumn(column));
        }

        @Override
        public boolean isSelected(ViewIndex<GridRow> row, ViewIndex<GridColumn> column) {
            return contains(mySelectedViewRows, row.asInteger()) && contains(mySelectedViewColumns, column.asInteger());
        }

        @Override
        public boolean isSelectedColumn(ModelIndex<GridColumn> column) {
            return contains(mySelectedViewColumns, toViewColumn(column));
        }

        @Override
        public boolean isSelectedRow(ModelIndex<GridRow> row) {
            return contains(mySelectedViewRows, toViewRow(row));
        }

        @Override
        public int getSelectedRowCount() {
            return mySelectedViewRows.length;
        }

        @Override
        public int getSelectedColumnCount() {
            return mySelectedViewColumns.length;
        }

        @Override
        @RequiredUIAccess
        public void selectWholeRow() {
            setSelectionInner(mySelectedViewRows, allViewColumns());
        }

        @Override
        @RequiredUIAccess
        public void selectWholeColumn() {
            setSelectionInner(allViewRows(), mySelectedViewColumns);
        }

        @Override
        @RequiredUIAccess
        public void clearSelection() {
            setSelectionInner(new int[0], new int[0]);
        }

        @Override
        public ModelIndex<GridRow> getSelectedRow() {
            return toModelRow(mySelectedViewRows.length == 0 ? -1 : mySelectedViewRows[0]);
        }

        @Override
        public ModelIndex<GridRow> getLeadSelectionRow() {
            return toModelRow(getLeadViewRow());
        }

        @Override
        public ModelIndex<GridColumn> getLeadSelectionColumn() {
            return toModelColumn(getLeadViewColumn());
        }

        @Override
        public ModelIndexSet<GridRow> getSelectedRows() {
            return modelRowsOf(mySelectedViewRows);
        }

        @Override
        public ModelIndex<GridColumn> getSelectedColumn() {
            return toModelColumn(mySelectedViewColumns.length == 0 ? -1 : mySelectedViewColumns[0]);
        }

        @Override
        public ModelIndexSet<GridColumn> getSelectedColumns() {
            return modelColumnsOf(mySelectedViewColumns);
        }
    }

    // endregion

    // region index conversion and data support

    public RawIndexConverter getRawIndexConverter() {
        return myIndexConverter;
    }

    private final class IndexConverterImpl implements RawIndexConverter {
        @Override
        public boolean isValidViewRowIdx(int viewRowIdx) {
            return viewRowIdx >= 0 && viewRowIdx < getViewRowCount();
        }

        @Override
        public boolean isValidViewColumnIdx(int viewColumnIdx) {
            return viewColumnIdx >= 0 && viewColumnIdx < getViewColumnCount();
        }

        @Override
        public IntUnaryOperator row2View() {
            return model -> model >= 0 && model < myModelToViewRows.length ? myModelToViewRows[model] : -1;
        }

        @Override
        public IntUnaryOperator column2View() {
            return model -> toViewColumn(ModelIndex.forColumn(getMutationModel(), model));
        }

        @Override
        public PairPairFunction<Integer> rowAndColumn2Model() {
            return (row, column) -> Pair.create(row2Model().applyAsInt(row), column2Model().applyAsInt(column));
        }

        @Override
        public PairPairFunction<Integer> rowAndColumn2View() {
            return (row, column) -> Pair.create(row2View().applyAsInt(row), column2View().applyAsInt(column));
        }

        @Override
        public IntUnaryOperator row2Model() {
            return view -> view >= 0 && view < myViewToModelRows.length ? myViewToModelRows[view] : -1;
        }

        @Override
        public IntUnaryOperator column2Model() {
            return view -> view >= 0 && view < myViewToModelColumns.length ? myViewToModelColumns[view] : -1;
        }
    }

    public GridDataSupport getDataSupport() {
        return myDataSupport;
    }

    public CoreResultView getResultView() {
        return myResultView;
    }

    private final class DataSupportImpl implements GridDataSupport {
        @Override
        @SuppressWarnings("unchecked")
        public void revert(ModelIndexSet<GridRow> rows, ModelIndexSet<GridColumn> columns) {
            GridMutator.DatabaseMutator<GridRow, GridColumn> mutator =
                ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.DatabaseMutator.class);
            if (mutator != null) {
                mutator.revert(new GridRequestSource(new DataGridRequestPlace(myGrid, rows, columns)), rows, columns);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isDeletedRows(ModelIndexSet<GridRow> rows) {
            GridMutator.RowsMutator<GridRow, GridColumn> mutator = ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.RowsMutator.class);
            return mutator != null && mutator.isDeletedRows(rows);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isModified(ModelIndex<GridRow> row, ModelIndex<GridColumn> column) {
            GridMutator.DatabaseMutator<GridRow, GridColumn> mutator =
                ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.DatabaseMutator.class);
            return mutator != null && mutator.getMutationType(row, column) != null;
        }

        @Override
        public boolean hasPendingChanges() {
            GridMutator<GridRow, GridColumn> mutator = myHookUp.getMutator();
            return mutator != null && mutator.hasPendingChanges();
        }

        @Override
        public boolean hasUnparsedValues() {
            GridMutator<GridRow, GridColumn> mutator = myHookUp.getMutator();
            return mutator != null && mutator.hasUnparsedValues();
        }

        @Override
        public boolean hasMutator() {
            return myHookUp.getMutator() != null;
        }

        @Override
        public boolean hasRowMutator() {
            return myHookUp.getMutator() instanceof GridMutator.RowsMutator;
        }

        @Override
        public boolean canRevert() {
            return myHookUp.getMutator() instanceof GridMutator.DatabaseMutator && hasPendingChanges();
        }

        @Override
        public boolean isSubmitImmediately() {
            GridMutator<GridRow, GridColumn> mutator = myHookUp.getMutator();
            return mutator != null && mutator.isUpdateImmediately();
        }

        @Override
        public void finishBuildingAndApply(List<CellMutation.Builder> mutations) {
            GridMutator<GridRow, GridColumn> mutator = myHookUp.getMutator();
            if (mutator == null || mutations.isEmpty()) {
                return;
            }
            List<CellMutation> built = new ArrayList<>(mutations.size());
            for (CellMutation.Builder builder : mutations) {
                built.add(builder.build());
            }
            mutator.mutate(new GridRequestSource(new DataGridRequestPlace(myGrid)), built, true);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isDeletedColumn(ModelIndex<GridColumn> column) {
            GridMutator.ColumnsMutator<GridRow, GridColumn> mutator =
                ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.ColumnsMutator.class);
            return mutator != null && mutator.isDeletedColumn(column);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean isInsertedColumn(ModelIndex<GridColumn> column) {
            GridMutator.ColumnsMutator<GridRow, GridColumn> mutator =
                ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.ColumnsMutator.class);
            return mutator != null && mutator.isInsertedColumn(column);
        }

        @Override
        @SuppressWarnings("unchecked")
        public int getInsertedColumnsCount() {
            GridMutator.ColumnsMutator<GridRow, GridColumn> mutator =
                ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.ColumnsMutator.class);
            return mutator == null ? 0 : mutator.getInsertedColumnsCount();
        }
    }

    // endregion

    // region editing

    /**
     * The value {@link #editSelectedCellWithValue} opens the editor of the lead cell with, instead of the value of the cell, and
     * whether the commit moves to the next cell. Both wait for the next {@link #startEditing} of that cell, because a frontend may
     * open its editor later, after a round trip to its client. They are dropped by any other {@link #startEditing}, when the lead
     * cell changes, and when the rows or columns are reloaded.
     */
    private record PendingEdit(ModelIndex<GridRow> row,
                               ModelIndex<GridColumn> column,
                               @Nullable Object value,
                               boolean shouldMoveFocus) {
        boolean isFor(ModelIndex<GridRow> rowIdx, ModelIndex<GridColumn> columnIdx) {
            return row.equals(rowIdx) && column.equals(columnIdx);
        }
    }

    /**
     * Cells are edited when the grid has editor factories and an editor helper ({@link GridCellEditorFactoryProvider#set},
     * {@link GridCellEditorHelper#set}). A grid without them is never edited. A read-only grid still opens its editors,
     * read-only.
     */
    public boolean isCellEditingAllowed() {
        return !myDisposed
            && GridCellEditorFactoryProvider.get(myGrid) != null
            && GridCellEditorHelper.GRID_CELL_EDITOR_HELPER_KEY.get(myGrid) != null;
    }

    /**
     * A hint whether the cells of a column may be edited, for a frontend which refuses an edit before it asks
     * {@link #startEditing}: the column exists, is neither a row id nor virtual, and its kind is not
     * {@link GridTypeKind#OTHER} ({@link GridCellEditorHelper#guessTypeKindForColumn}). Whether a cell is edited is decided per
     * cell, by {@link #startEditing} - the factories cannot answer for a column without a row.
     */
    public boolean isColumnEditable(int viewColumn) {
        if (!isCellEditingAllowed() || isEditingBlocked()) {
            return false;
        }
        GridColumn column = getColumn(viewColumn);
        if (column == null || GridUtilCore.isRowId(column) || GridUtilCore.isVirtualColumn(column)) {
            return false;
        }
        GridCellRequest<GridRow, GridColumn> request = GridCellRequest.requestColumn(myGrid, toModelColumn(viewColumn));
        return GridCellEditorHelper.get(myGrid).guessTypeKindForColumn(request) != GridTypeKind.OTHER;
    }

    /**
     * Whether a cell is edited: it has an editor factory, its column is neither a row id nor virtual, and editing is not blocked
     * ({@link #isEditingBlocked()}).
     */
    public boolean isCellEditable(int viewRow, int viewColumn) {
        if (!isCellEditingAllowed() || isEditingBlocked()) {
            return false;
        }
        ModelIndex<GridRow> rowIdx = toModelRow(viewRow);
        ModelIndex<GridColumn> columnIdx = toModelColumn(viewColumn);
        PendingEdit pendingEdit = myPendingEdit;
        Object currentValue = pendingEdit != null && pendingEdit.isFor(rowIdx, columnIdx) ? pendingEdit.value() : null;
        return getEditorFactory(createRequest(rowIdx, columnIdx, currentValue)) != null;
    }

    public boolean isEditing() {
        return myEditSession != null;
    }

    public @Nullable DataGridEditSession getEditSession() {
        return myEditSession;
    }

    /**
     * Opens an edit of a cell for the native editor of the frontend.
     * <p/>
     * An edit which is already open for this cell is returned again (a frontend which opens its editor again for the same
     * edit). An edit open for another cell is one whose native editor the frontend lost: it is committed, or dropped when it
     * cannot be.
     * <p/>
     * When the editor decided its value when it was created ({@link consulo.ui.grid.editor.GridCellEditorPresentation.Kind#NONE}:
     * a boolean set by a typed key, or toggled in check box mode), the value is committed at once and nothing is returned.
     *
     * @return the edit the native editor shows, or {@code null} when the cell is not edited
     */
    @RequiredUIAccess
    public @Nullable DataGridEditSession startEditing(int viewRow, int viewColumn, GridEditInitiator initiator) {
        ModelIndex<GridRow> rowIdx = toModelRow(viewRow);
        ModelIndex<GridColumn> columnIdx = toModelColumn(viewColumn);
        PendingEdit pendingEdit = myPendingEdit;
        myPendingEdit = null;
        if (pendingEdit != null && !pendingEdit.isFor(rowIdx, columnIdx)) {
            pendingEdit = null;
        }

        DataGridEditSession current = myEditSession;
        if (current != null) {
            if (current.isHosted() && current.isEditingCell(rowIdx, columnIdx) && pendingEdit == null) {
                return current;
            }
            if (!commitEditing()) {
                discardEditing();
            }
        }

        if (!isCellEditingAllowed() || !myGrid.isReady() || isEditingBlocked()) {
            return null;
        }

        Object currentValue = pendingEdit == null ? null : pendingEdit.value();
        GridCellRequest<GridRow, GridColumn> request = createRequest(rowIdx, columnIdx, currentValue);
        GridCellEditorFactory editorFactory = getEditorFactory(request);
        if (editorFactory == null) {
            return null;
        }

        Object value = getMutationModel().getValueAt(rowIdx, columnIdx);
        boolean shouldMoveFocus = pendingEdit == null || pendingEdit.shouldMoveFocus();
        GridCellEditor editor = editorFactory.createEditor(request, initiator);

        boolean inSelection = contains(mySelectedViewRows, viewRow) && contains(mySelectedViewColumns, viewColumn);
        ModelIndexSet<GridRow> targetRows = inSelection
            ? modelRowsOf(mySelectedViewRows)
            : ModelIndexSet.forRows(getMutationModel(), rowIdx.asInteger());
        ModelIndexSet<GridColumn> targetColumns = inSelection
            ? modelColumnsOf(mySelectedViewColumns)
            : ModelIndexSet.forColumns(getMutationModel(), columnIdx.asInteger());

        DataGridEditSession session = new DataGridEditSession(this, rowIdx, columnIdx, editor, initiator, targetRows, targetColumns,
            shouldMoveFocus, editorFactory.allowsUniqueMultiEdit());
        myEditSession = session;
        if (currentValue != null && !Comparing.equal(currentValue, value)) {
            fireValueEdited(currentValue);
        }
        editor.setEditingListener(object -> onValueEditedInEditor(session, object));

        if (!session.isHosted()) {
            // the decided value is fired (so a multi-cell edit writes it into the selection) and the edit ends; an editor which
            // fired its value already (the default boolean editor does when its editing listener is installed) is not fired twice
            Object decidedValue = editor.getValue();
            if (!session.hasCommonValue() || !Comparing.equal(session.getCommonValue(), decidedValue)) {
                fireValueEdited(decidedValue);
            }
            if (!commitEditing() && !session.isWaitingForAnswer()) {
                discardEditing();
            }
            return null;
        }
        return session;
    }

    /**
     * Commits the open edit:
     * <ol>
     * <li>a mutator which cannot take the value without losing unsubmitted changes makes the grid ask the user first;</li>
     * <li>a read-only grid refuses a changed value;</li>
     * <li>the editor may refuse its text ({@link GridCellEditor#stop()}, for example text which does not parse);</li>
     * <li>otherwise the edit is closed, and its value goes to every cell it writes.</li>
     * </ol>
     * The frontend calls it for every gesture which commits: Enter, Tab, a click on another cell, the focus leaving the grid.
     *
     * @return {@code true} when the edit is closed, or none was open; {@code false} when it stays open - the session's
     * {@link DataGridEditSession#getError() error} tells why, or the grid waits for the user's
     * {@link DataGridEditSession#isWaitingForAnswer() answer}
     */
    @RequiredUIAccess
    public boolean commitEditing() {
        DataGridEditSession session = myEditSession;
        if (session == null) {
            return true;
        }
        if (session.isWaitingForAnswer()) {
            return false;
        }

        GridCellEditor editor = session.getEditor();
        ModelIndexSet<GridRow> editingRow = ModelIndexSet.forRows(getMutationModel(), session.getRowIdx().asInteger());
        ModelIndexSet<GridColumn> editingColumn = ModelIndexSet.forColumns(getMutationModel(), session.getColumnIdx().asInteger());
        boolean multiEditing = isMultiEditingAllowed();
        ModelIndexSet<GridRow> rows = multiEditing ? session.getTargetRows() : editingRow;
        ModelIndexSet<GridColumn> columns = multiEditing ? session.getTargetColumns() : editingColumn;

        if (!isSafeToUpdate(session, rows, columns, editor.getValue())) {
            return false;
        }

        if (!isEditable() && !getMutationModel().allValuesEqualTo(editingRow, editingColumn, editor.getValue())) {
            // the refusal shows as the error of the edit
            LocalizeValue message = LocalizeValue.localizeTODO("This table is read-only. Changes cannot be applied.");
            session.setError(new UnparsedValue.ParsingError(message.get()));
            return false;
        }
        if (!editor.stop()) {
            session.setError(editor.getError());
            return false;
        }

        // the edit is closed, and the value of the editor goes to the cells
        Object value = editor.getValue();
        boolean moveToNextCell = session.shouldMoveFocus();
        closeSession(session);
        setValueAt(rows, columns, value, moveToNextCell);
        return true;
    }

    /**
     * Closes the open edit without writing its value. A frontend also calls it when its native editor goes away without a commit:
     * the widget is torn down, or the editor failed to open.
     */
    @RequiredUIAccess
    public void discardEditing() {
        DataGridEditSession session = myEditSession;
        if (session == null) {
            return;
        }
        session.getEditor().cancel();
        closeSession(session);
    }

    /**
     * {@link DataGrid#stopEditing()}: the native editor commits, and closes when the commit is accepted.
     */
    @RequiredUIAccess
    public boolean stopEditing() {
        DataGridEditSession session = myEditSession;
        if (session == null) {
            return true;
        }
        View view = myView;
        if (session.isHosted() && view != null) {
            if (!view.stopCellEditor()) {
                return false;
            }
            if (myEditSession != session) {
                return true;
            }
        }
        // no native editor shows the edit
        return commitEditing();
    }

    /**
     * {@link DataGrid#cancelEditing()}: the native editor closes without a commit.
     */
    @RequiredUIAccess
    public void cancelEditing() {
        DataGridEditSession session = myEditSession;
        if (session == null) {
            return;
        }
        View view = myView;
        if (session.isHosted() && view != null) {
            view.cancelCellEditor();
        }
        if (myEditSession == session) {
            discardEditing();
        }
    }

    /**
     * {@link DataGrid#editSelectedCell()}: the native editor of the lead cell opens.
     */
    @RequiredUIAccess
    public void editSelectedCell() {
        myPendingEdit = null;
        editLeadCell();
    }

    /**
     * The editor of the lead cell opens with this value instead of the value of the cell.
     *
     * @param shouldMoveFocus whether the commit moves the selection to the next cell
     */
    @RequiredUIAccess
    public void editSelectedCellWithValue(@Nullable Object value, boolean shouldMoveFocus) {
        int leadRow = getLeadViewRow();
        int leadColumn = getLeadViewColumn();
        if (leadRow == -1 || leadColumn == -1) {
            return;
        }
        myPendingEdit = new PendingEdit(toModelRow(leadRow), toModelColumn(leadColumn), value, shouldMoveFocus);
        editLeadCell();
    }

    @RequiredUIAccess
    private void editLeadCell() {
        int leadRow = getLeadViewRow();
        int leadColumn = getLeadViewColumn();
        View view = myView;
        if (leadRow == -1 || leadColumn == -1 || view == null) {
            return;
        }
        view.editCellAt(leadRow, leadColumn, GridEditInitiator.ACTION);
    }

    /**
     * A value was typed into the editor, so the edit writes it into every selected cell. No selected column is taken as unique,
     * which would need a single row or {@link DataGridEditSession#allowsUniqueMultiEdit()}, and the selected columns are taken as
     * editable together.
     */
    public boolean isMultiEditingAllowed() {
        DataGridEditSession session = myEditSession;
        return session != null && session.hasCommonValue();
    }

    private GridCellRequest<GridRow, GridColumn> createRequest(ModelIndex<GridRow> rowIdx,
                                                               ModelIndex<GridColumn> columnIdx,
                                                               @Nullable Object currentValue) {
        GridCellRequest<GridRow, GridColumn> request = GridCellRequest.request(myGrid, rowIdx, columnIdx);
        if (currentValue != null) {
            request = GridCellRequest.overrideValue(request, currentValue);
        }
        return request;
    }

    private static @Nullable GridCellEditorFactory getEditorFactory(GridCellRequest<GridRow, GridColumn> request) {
        if (!request.isValid()) {
            return null;
        }
        GridCellEditorFactory editorFactory = GridCellEditorFactoryProvider.provideEditorFactory(request);
        GridColumn dataColumn = request.getColumn();
        return dataColumn != null && !GridUtilCore.isRowId(dataColumn) && !GridUtilCore.isVirtualColumn(dataColumn)
            ? editorFactory
            : null;
    }

    /**
     * The editor changed its value: the value is fired to the listeners of the grid, and the cells of a multi-cell edit show it.
     */
    @RequiredUIAccess
    private void onValueEditedInEditor(DataGridEditSession session, @Nullable Object value) {
        if (myEditSession != session) {
            return;
        }
        fireValueEdited(value);
        if (session.isMultiCell() && isMultiEditingAllowed()) {
            repaintTargets(session);
        }
    }

    /**
     * Ends the edit: the editor is disposed, and the cells which showed its value are repainted.
     */
    @RequiredUIAccess
    private void closeSession(DataGridEditSession session) {
        boolean previewShown = session.isMultiCell() && isMultiEditingAllowed();
        myEditSession = null;
        Disposer.dispose(session.getEditor());
        if (previewShown) {
            repaintTargets(session);
        }
    }

    @RequiredUIAccess
    private void repaintTargets(DataGridEditSession session) {
        View view = myView;
        if (view == null) {
            return;
        }
        int first = Integer.MAX_VALUE;
        int last = -1;
        for (int model : session.getTargetRows().asArray()) {
            int viewRow = model >= 0 && model < myModelToViewRows.length ? myModelToViewRows[model] : -1;
            if (viewRow >= 0) {
                first = Math.min(first, viewRow);
                last = Math.max(last, viewRow);
            }
        }
        if (last >= 0) {
            view.cellsChanged(first, last);
        }
    }

    /**
     * Whether the mutator takes the value without losing unsubmitted changes. Otherwise the user is asked; the answer comes later,
     * so the commit is refused while the question is open, and made again once the user answered Yes.
     */
    @RequiredUIAccess
    private boolean isSafeToUpdate(DataGridEditSession session,
                                   ModelIndexSet<GridRow> rows,
                                   ModelIndexSet<GridColumn> columns,
                                   @Nullable Object newValue) {
        GridMutator<GridRow, GridColumn> mutator = myHookUp.getMutator();
        if (mutator == null || mutator.isUpdateSafe(rows, columns, newValue)) {
            return true;
        }
        if (session.isIgnoreUnsubmittedChanges()) {
            session.setIgnoreUnsubmittedChanges(false);
            return true;
        }
        session.setWaitingForAnswer(true);
        showIgnoreUnsubmittedChangesYesNoDialog(yes -> onIgnoreUnsubmittedChangesAnswer(session, yes));
        return false;
    }

    @RequiredUIAccess
    private void onIgnoreUnsubmittedChangesAnswer(DataGridEditSession session, boolean yes) {
        session.setWaitingForAnswer(false);
        if (myEditSession != session) {
            // the edit was closed meanwhile
            return;
        }
        if (!yes) {
            // a text editor stays open. An edit without a native editor, and a list whose option was chosen, are cancelled - their
            // value is decided, so there is nothing left to edit
            if (!session.isHosted()) {
                discardEditing();
            }
            else if (session.getPresentation().kind() == GridCellEditorPresentation.Kind.LIST) {
                cancelEditing();
            }
            return;
        }

        session.setIgnoreUnsubmittedChanges(true);
        View view = myView;
        if (session.isHosted() && view != null) {
            // the native editor commits through commitEditing, which does not ask again
            view.stopCellEditor();
        }
        if (myEditSession == session && session.isIgnoreUnsubmittedChanges()) {
            // no native editor committed it
            if (!commitEditing() && !session.isHosted()) {
                discardEditing();
            }
        }
    }

    /**
     * Asks whether to lose the unsubmitted changes; the answer comes later, on the UI thread.
     */
    @RequiredUIAccess
    private void showIgnoreUnsubmittedChangesYesNoDialog(@RequiredUIAccess Consumer<Boolean> answer) {
        MessageBoxes.yesNo()
            .asWarning()
            .title(LocalizeValue.localizeTODO("Ignore Unsubmitted Changes"))
            .text(LocalizeValue.localizeTODO("Changes are not submitted. Data will be lost. Continue?"))
            .showAsync(myGrid)
            .whenComplete((yes, error) -> onUIThread(() -> answer.accept(Boolean.TRUE.equals(yes))));
    }

    /**
     * Writes a value into cells: the value goes to the mutator, unless every cell already holds it, and the selection moves to the
     * next cell once the mutator is done.
     */
    @RequiredUIAccess
    private void setValueAt(ModelIndexSet<GridRow> rows,
                            ModelIndexSet<GridColumn> columns,
                            @Nullable Object value,
                            boolean moveToNextCell) {
        GridMutator<GridRow, GridColumn> mutator = myHookUp.getMutator();
        GridModel<GridRow, GridColumn> model = getMutationModel();
        ModelIndexSet<GridRow> validRows = ModelIndexSet.forRows(model, valid(rows));
        ModelIndexSet<GridColumn> validColumns = ModelIndexSet.forColumns(model, valid(columns));
        Runnable moveToNextCellRunnable = moveToNextCell ? createMoveToNextCell(rows, columns) : null;

        if (mutator == null || validRows.size() == 0 || model.allValuesEqualTo(validRows, validColumns, value)) {
            if (moveToNextCellRunnable != null) {
                myUIAccess.give(moveToNextCellRunnable);
            }
            return;
        }

        GridRequestSource source = new GridRequestSource(new DataGridRequestPlace(myGrid));
        myUIAccess.give(() -> {
            if (myDisposed) {
                return;
            }
            if (moveToNextCellRunnable != null) {
                source.getActionCallback().doWhenDone(() -> onUIThread(moveToNextCellRunnable));
            }
            mutator.mutate(source, GridUtilCore.createMutations(validRows, validColumns, value), true);
        });
    }

    private <T> int[] valid(ModelIndexSet<T> set) {
        return set.asList().stream().filter(index -> index.isValid(myGrid)).mapToInt(ModelIndex::asInteger).toArray();
    }

    /**
     * Moves from the last cell of the written ones; it is remembered by its model indices, which sorting does not change.
     */
    private @Nullable Runnable createMoveToNextCell(ModelIndexSet<GridRow> rows, ModelIndexSet<GridColumn> columns) {
        int lastViewRow = -1;
        for (ModelIndex<GridRow> row : rows.asIterable()) {
            lastViewRow = Math.max(lastViewRow, toViewRow(row));
        }
        int lastViewColumn = -1;
        for (ModelIndex<GridColumn> column : columns.asIterable()) {
            lastViewColumn = Math.max(lastViewColumn, toViewColumn(column));
        }
        if (lastViewRow < 0 || lastViewColumn < 0) {
            return null;
        }
        ModelIndex<GridRow> rowIdx = toModelRow(lastViewRow);
        ModelIndex<GridColumn> columnIdx = toModelColumn(lastViewColumn);
        return () -> moveToNextCell(rowIdx, columnIdx);
    }

    /**
     * A single selected cell, which is still the edited one, moves to the next row - or to the next column in an inserted row.
     * The focus is not checked: the controller does not know it.
     */
    @RequiredUIAccess
    private void moveToNextCell(ModelIndex<GridRow> rowIdx, ModelIndex<GridColumn> columnIdx) {
        if (mySelectedViewRows.length != 1 || mySelectedViewColumns.length != 1 || isEditing()) {
            return;
        }

        // if selection has already been changed by Tab or Shift-Tab we don't need to change it
        if (!Comparing.equal(mySelectionModel.getSelectedRow(), rowIdx)
            || !Comparing.equal(mySelectionModel.getSelectedColumn(), columnIdx)) {
            return;
        }

        int viewRow = toViewRow(rowIdx);
        int viewColumn = toViewColumn(columnIdx);
        if (viewRow < 0 || viewColumn < 0) {
            return;
        }
        if (isInsertedRow(rowIdx)) {
            viewColumn = viewColumn + 1 < getViewColumnCount() ? viewColumn + 1 : viewColumn;
        }
        else {
            viewRow = viewRow + 1 < getViewRowCount() ? viewRow + 1 : viewRow;
        }

        setSelectionInner(new int[]{viewRow}, new int[]{viewColumn});
        if (myView != null) {
            myView.scrollToCell(viewRow, viewColumn);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isInsertedRow(ModelIndex<GridRow> row) {
        GridMutator.RowsMutator<GridRow, GridColumn> mutator = ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.RowsMutator.class);
        return mutator != null && mutator.isInsertedRow(row);
    }

    @SuppressWarnings("unchecked")
    private GridMutator.@Nullable DatabaseMutator<GridRow, GridColumn> getDatabaseMutator() {
        return ObjectUtil.tryCast(myHookUp.getMutator(), GridMutator.DatabaseMutator.class);
    }

    // endregion

    // region edit commands

    /**
     * The Submit button: there are pending changes, none of them unparsed, and no request is running.
     */
    public boolean canSubmit() {
        GridMutator.DatabaseMutator<GridRow, GridColumn> mutator = getDatabaseMutator();
        return mutator != null
            && !myHookUp.isReadOnly()
            && mutator.hasPendingChanges()
            && !mutator.hasUnparsedValues()
            && isReadyForRequest();
    }

    /**
     * {@link DataGrid#submit()}: commits the open edit, then submits the pending changes, as a request of the grid: the grid is busy
     * while it runs, its error shows in the status line, and the next request which finishes without one clears it.
     *
     * @return done at once when there is nothing to submit - no {@link GridMutator.DatabaseMutator}, or no pending change; rejected
     * at once when the open edit could not be committed; otherwise the callback of the submit request, which is done once the data
     * source reports the submit as finished
     */
    @RequiredUIAccess
    public ActionCallback submit() {
        boolean wasEditing = isEditing();
        if (!stopEditing()) {
            return ActionCallback.REJECTED;
        }
        if (wasEditing) {
            // the committed value reaches the mutator later (setValueAt) - submit after it
            ActionCallback result = new ActionCallback();
            myUIAccess.give(() -> submitPendingChanges().notify(result));
            return result;
        }
        return submitPendingChanges();
    }

    @RequiredUIAccess
    private ActionCallback submitPendingChanges() {
        GridMutator.DatabaseMutator<GridRow, GridColumn> mutator = getDatabaseMutator();
        if (myDisposed || mutator == null || !mutator.hasPendingChanges()) {
            return ActionCallback.DONE;
        }
        GridSelection<GridRow, GridColumn> selection = mySelectionModel.store();
        DataGridRequestPlace place = new DataGridRequestPlace(myGrid, mutator.getAffectedRows(), ModelIndexSet.forColumns(myGrid, -1));
        GridRequestSource submitted = issue(place, source -> {
            source.getActionCallback().doWhenDone(() -> onUIThread(() -> mySelectionModel.restore(mySelectionModel.fit(selection))));
            mutator.submit(source, true);
        });
        return submitted == null ? ActionCallback.REJECTED : submitted.getActionCallback();
    }

    /**
     * The Revert button is visible while changes are kept until a submit, or while there are changes.
     */
    private boolean isRevertVisible() {
        return getDatabaseMutator() != null && (!myDataSupport.isSubmitImmediately() || hasChanges()) && !myHookUp.isReadOnly();
    }

    /**
     * The Revert button: a change lies under the selection.
     */
    public boolean canRevertSelection() {
        return isRevertVisible() && hasChanges() && myDataSupport.canRevert() && hasChangeUnderSelection();
    }

    /**
     * Cancels the open edit, then reverts the changes under the selection.
     */
    @RequiredUIAccess
    public void revertSelection() {
        cancelEditing();
        if (!myDataSupport.hasRowMutator()) {
            return;
        }
        ModelIndexSet<GridColumn> columns = mySelectionModel.getSelectedColumns();
        ModelIndexSet<GridRow> rows = mySelectionModel.getSelectedRows();
        if (columns.size() != 0 && rows.size() != 0) {
            myDataSupport.revert(rows, columns);
        }
    }

    private boolean hasChanges() {
        return myDataSupport.hasPendingChanges() || myDataSupport.getInsertedColumnsCount() > 0;
    }

    private boolean hasChangeUnderSelection() {
        ModelIndexSet<GridRow> rows = mySelectionModel.getSelectedRows();
        ModelIndexSet<GridColumn> columns = mySelectionModel.getSelectedColumns();
        for (ModelIndex<GridColumn> column : columns.asIterable()) {
            if (myDataSupport.isDeletedColumn(column) || myDataSupport.isInsertedColumn(column)) {
                return true;
            }
        }
        for (ModelIndex<GridRow> rowIdx : rows.asIterable()) {
            ModelIndexSet<GridRow> rowIdxSet = ModelIndexSet.forRows(getMutationModel(), rowIdx.asInteger());
            if (myDataSupport.isDeletedRows(rowIdxSet) || isInsertedRow(rowIdx)) {
                return true;
            }
            for (ModelIndex<GridColumn> columnIdx : columns.asIterable()) {
                if (myDataSupport.isModified(rowIdx, columnIdx)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The Set NULL button: the grid is editable, something is selected, and every selected column is nullable
     * ({@link GridCellEditorHelper#isNullable}) and neither a row id nor virtual.
     */
    public boolean canSetSelectionToNull() {
        if (!isEditable() || mySelectionModel.isSelectionEmpty()) {
            return false;
        }
        GridCellEditorHelper helper = GridCellEditorHelper.GRID_CELL_EDITOR_HELPER_KEY.get(myGrid);
        for (int viewColumn : mySelectedViewColumns) {
            GridColumn column = getColumn(viewColumn);
            if (column == null || GridUtilCore.isRowId(column) || GridUtilCore.isVirtualColumn(column)) {
                return false;
            }
            if (helper != null && !helper.isNullable(myGrid, toModelColumn(viewColumn))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Cancels the open edit, then writes {@link ReservedCellValue#NULL} into the selected cells; the selection stays.
     */
    @RequiredUIAccess
    public void setSelectionToNull() {
        cancelEditing();
        if (!canSetSelectionToNull()) {
            return;
        }
        setValueAt(mySelectionModel.getSelectedRows(), mySelectionModel.getSelectedColumns(), ReservedCellValue.NULL, false);
    }

    // endregion

    // region the rest of DataGrid

    public String getName(GridColumn column) {
        String name = column.getName();
        return name.isEmpty() ? myAppearance.getAnonymousColumnName() : name;
    }

    public ObjectFormatter getObjectFormatter() {
        ObjectFormatter formatter = myFormatter;
        if (formatter == null) {
            formatter = myFormatterProvider.apply(myGrid);
            myFormatter = formatter;
        }
        return formatter;
    }

    @RequiredUIAccess
    public void setObjectFormatterProvider(Function<DataGrid, ObjectFormatter> provider) {
        myFormatterProvider = provider;
        myFormatter = null;
        cancelEditIfAny();
        if (myView != null) {
            myView.rowsChanged();
        }
    }

    public void addDataGridListener(DataGridListener listener, Disposable disposable) {
        myListeners.add(listener);
        Disposer.register(disposable, () -> myListeners.remove(listener));
    }

    public void fireContentChanged(GridRequestSource.@Nullable RequestPlace place) {
        for (DataGridListener listener : myListeners) {
            listener.onContentChanged(myGrid, place);
        }
    }

    public void fireValueEdited(@Nullable Object value) {
        // the cells of a multi-cell edit show the last value of the editor
        DataGridEditSession session = myEditSession;
        if (session != null) {
            session.setCommonValue(value);
        }
        for (DataGridListener listener : myListeners) {
            listener.onValueEdited(myGrid, value);
        }
    }

    public boolean isEditable() {
        return myHookUp.getMutator() != null && !myHookUp.isReadOnly();
    }

    /**
     * {@link DataGrid#setCells}: no editor is involved - the value goes to the mutator, once the user agreed to
     * lose the unsubmitted changes, if the mutator asks for that.
     */
    @RequiredUIAccess
    public void setCells(ModelIndexSet<GridRow> rows, ModelIndexSet<GridColumn> columns, @Nullable Object value) {
        GridMutator<GridRow, GridColumn> mutator = myHookUp.getMutator();
        if (mutator == null) {
            return;
        }
        Runnable mutate = () -> mutator.mutate(new GridRequestSource(new DataGridRequestPlace(myGrid, rows, columns)), rows,
            columns, value, true);
        if (mutator.isUpdateSafe(rows, columns, value)) {
            mutate.run();
        }
        else {
            showIgnoreUnsubmittedChangesYesNoDialog(yes -> {
                if (yes && !myDisposed) {
                    mutate.run();
                }
            });
        }
    }

    @RequiredUIAccess
    public void showCell(int absoluteRowIdx, ModelIndex<GridColumn> column) {
        ModelIndex<GridRow> row = myHookUp.getPageModel().findRow(absoluteRowIdx);
        int viewRow = toViewRow(row);
        int viewColumn = toViewColumn(column);
        if (viewRow < 0) {
            return;
        }
        setSelectionInner(new int[]{viewRow}, viewColumn >= 0 ? new int[]{viewColumn} : new int[0]);
        if (myView != null) {
            myView.scrollToCell(viewRow, Math.max(viewColumn, 0));
        }
    }

    public ModelIndexSet<GridColumn> getVisibleColumns() {
        return modelColumnsOf(allViewColumns());
    }

    public ModelIndexSet<GridRow> getVisibleRows() {
        return modelRowsOf(allViewRows());
    }

    @Override
    public void dispose() {
        myDisposed = true;
        myView = null;
        myListeners.clear();
        myPendingEdit = null;
        DataGridEditSession session = myEditSession;
        myEditSession = null;
        if (session != null) {
            Disposer.dispose(session.getEditor());
        }
    }

    // endregion
}
