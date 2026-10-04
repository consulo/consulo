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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import consulo.application.Application;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorFontType;
import consulo.colorScheme.event.EditorColorsListener;
import consulo.disposer.Disposable;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Point2D;
import consulo.ui.TextAttribute;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.ContextMenuEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.font.Font;
import consulo.ui.grid.DataGrid;
import consulo.ui.grid.DataGridAppearance;
import consulo.ui.grid.GridColumn;
import consulo.ui.grid.GridDataHookUp;
import consulo.ui.grid.GridHitArea;
import consulo.ui.grid.GridRow;
import consulo.ui.grid.ModelIndex;
import consulo.ui.grid.RowSortOrder;
import consulo.ui.grid.editor.GridCellEditorPresentation;
import consulo.ui.grid.editor.GridEditInitiator;
import consulo.ui.grid.editor.UnparsedValue;
import consulo.ui.impl.style.StyleColorKeys;
import consulo.ui.internal.DataGridAppearanceImpl;
import consulo.ui.internal.DataGridController;
import consulo.ui.internal.DataGridControllerOwner;
import consulo.ui.internal.DataGridEditSession;
import consulo.ui.style.StyleColorValue;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.base.WebInputDetails;
import consulo.web.ui.impl.internal.vaadin.datagrid.RevoDataGridVaadin;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * The web {@link DataGrid}: the paging bar of its {@link DataGridController} over {@code <consulo-data-grid>}, a RevoGrid
 * which holds nothing but the text the controller rendered for the rows on screen.
 * <p/>
 * A press on a column header selects the column (shift extends, control or meta toggles); sorting is the controller's - a
 * press on the sort marker of the header is handed to it and the rows come back reordered. The selection is a rectangle of
 * cells, the way RevoGrid selects; a press on a row number selects the row, a control press toggles a row. A context menu
 * gesture is hit-tested by the element and reported to the controller before the {@link ContextMenuEvent} of the grid fires.
 * <p/>
 * Cells are edited in a native field the element hosts in the cell: the controller opens the edit, parses and writes the
 * value, and moves to the next cell; the element only exchanges the text and the gestures (see {@link RevoDataGridVaadin}).
 *
 * @since 2026-10-03
 */
public class WebDataGridImpl extends VaadinComponentDelegate<WebDataGridImpl.Vaadin> implements DataGridControllerOwner {
    private static final String TOOLBAR_CLASS = "web-data-grid-toolbar";

    private static final int WIDTH_SAMPLE_ROWS = 100;
    private static final int MIN_COLUMN_CHARS = DataGridController.FIT_MIN_CHARS;
    private static final int MAX_COLUMN_CHARS = DataGridController.FIT_MAX_CHARS;
    /**
     * Room the header of a sortable column keeps for its sort marker.
     */
    private static final int SORT_MARKER_CHARS = 2;

    /**
     * The keys which open the context menu of the lead cell, as the element reports them.
     */
    private static final KeyCode CONTEXT_MENU_KEY = KeyCode.of(0x20D, "VK_CONTEXT_MENU");
    private static final KeyCode F10_KEY = KeyCode.of(0x79, "VK_F10");

    /**
     * The css colour of each {@link StyleColorValue} which has a style variable: {@code var(--consulo-...)}, so that it follows
     * the style on the client.
     */
    private static final Map<StyleColorValue, String> ourStyleCssColors = new ConcurrentHashMap<>();

    /**
     * Rows per request of the client - a page of 1000 rows or an unlimited one is never sent whole.
     */
    private static final int CHUNK_SIZE = 200;

    /**
     * The client reports the rows on screen with every scroll frame - only the position it settles at matters here.
     */
    private static final int VIEWPORT_DEBOUNCE_MS = 150;

    /**
     * The client reports the text of an open editor with every keystroke. The commit carries the text anyway; the copy here
     * is for an edit stopped through the API ({@link DataGrid#stopEditing()}), and for the cells of a multi-cell edit, which
     * show what is typed.
     */
    private static final int EDIT_INPUT_DEBOUNCE_MS = 150;

    // the text styles of a cell; its colours go as css colours (see sendRows)
    private static final int FLAG_NULL = 1;
    private static final int FLAG_GRAYED = 2;
    private static final int FLAG_ERROR = 4;

    private static final String REASON_ENTER = "enter";
    private static final String REASON_NAVIGATE = "navigate";
    private static final String REASON_BLUR = "blur";

    @StyleSheet("/dataGrid/webDataGrid.css")
    public class Vaadin extends VerticalLayout implements FromVaadinComponentWrapper {
        public Vaadin() {
            setMargin(false);
            setPadding(false);
            setSpacing(false);

            // the grid scrolls inside, so the box has to take the size it is given rather than the size of its rows
            setSizeFull();
            getStyle().set("min-height", "0").set("min-width", "0");
        }

        @Override
        public consulo.ui.@Nullable Component toUIComponent() {
            return WebDataGridImpl.this;
        }
    }

    private final DataGridController myController;
    private final RevoDataGridVaadin myTable;
    private final com.vaadin.flow.component.Component myToolbar;

    /**
     * The config last sent to the client - it is sent again only when it changed.
     */
    private String myConfigJson = "";

    /**
     * The widths the grid measures itself, in characters of the grid font without the padding of the cell, for the columns
     * the controller has no width for ({@link DataGridController#getViewColumnWidth} is {@code 0}).
     */
    private int[] myAutoWidths = new int[0];
    private boolean myColumnWidthsPending;
    /**
     * Set by a structure change made while rows are present: the widths measured then are provisional, they are measured
     * again on the first data change after the reload sent together with the structure change.
     */
    private boolean myColumnWidthsProvisional;

    private int myColumnsGeneration;
    private int myRowsGeneration;
    private int mySelectionSeq;

    /**
     * The rows on screen as the client last reported them: a reload or a change of cells sends them along at once, so
     * a re-sort does not wait for a round trip.
     */
    private int myVisibleFrom;
    private int myVisibleTo;

    /**
     * The client edit which shows the edit session of the controller: its {@code seq}, or -1. An event or a call about any
     * other edit is stale.
     */
    private int myEditSeq = -1;
    private @Nullable DataGridEditSession myEditSession;
    /**
     * The selection when the edit opened - put back when a move to another cell is refused, together with the editor.
     */
    private int[] myEditSelectionRows = new int[0];
    private int[] myEditSelectionColumns = new int[0];
    /**
     * A selection the controller set while an edit was open: it is pushed once the edit is over, because setting it on the
     * client would close the editor there (RevoGrid drops the editor with the focus).
     */
    private boolean mySelectionDeferred;

    @RequiredUIAccess
    public WebDataGridImpl(GridDataHookUp<GridRow, GridColumn> hookUp, BiConsumer<DataGrid, DataGridAppearance> configurator) {
        myController = new DataGridController(this, hookUp);
        configurator.accept(this, myController.getAppearance());

        myTable = new RevoDataGridVaadin(this);
        installListeners();

        myToolbar = TargetVaadin.to(myController.getToolbar());
        myToolbar.addClassName(TOOLBAR_CLASS);
        myToolbar.getStyle().set("flex-shrink", "0");
        myToolbar.setVisible(myController.isToolbarVisible());

        Vaadin root = toVaadinComponent();
        root.add(myToolbar, myTable);

        pushConfig();
        myTable.setFont(fontJson());
        rebuildColumns();
        pushRowModel();
        pushSelection();

        myController.setView(new ViewImpl());
        myController.start();

        // the grid is set in the editor font of the colour scheme, and follows a change of the scheme; the connection goes
        // with the controller
        Application.get().getMessageBus().connect(myController).subscribe(EditorColorsListener.class, scheme -> onEditorSchemeChanged());
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public DataGridController getController() {
        return myController;
    }

    @Override
    public void focus() {
        myTable.focusGrid();
    }

    /**
     * The element reports a context menu gesture after hit-testing it ({@code consulo-grid-contextmenu}), and the
     * {@link ContextMenuEvent} is fired from there, once the controller knows what the menu was opened on - so no listener of
     * the box itself, which would fire for any press in it, without knowing what is under the pointer.
     */
    @Override
    public <C extends consulo.ui.Component, E extends ComponentEvent<C>> Disposable addListener(Class<? extends E> eventClass,
                                                                                                ComponentEventListener<C, E> listener) {
        if (eventClass == ContextMenuEvent.class) {
            return dataObject().addListener(eventClass, listener);
        }
        return super.addListener(eventClass, listener);
    }

    private String configJson() {
        DataGridAppearanceImpl appearance = myController.getAppearance();
        ObjectNode config = JsonNodeFactory.instance.objectNode();
        config.put("rowNumbers", myController.isShowRowNumbers());
        config.put("horizontalLines", appearance.isShowHorizontalLines());
        config.put("striped", appearance.isStriped());
        config.put("hoverRows", appearance.isHoveredRowBgHighlightingEnabled());
        config.put("transparentColumnHeader", appearance.isTransparentColumnHeaderBackground());
        config.put("transparentRowHeader", appearance.isTransparentRowHeaderBackground());
        config.put("chunkSize", CHUNK_SIZE);
        config.put("editable", myController.isCellEditingAllowed());
        config.put("rowLines", Math.max(1, myController.getRowLines()));
        config.put("moveColumns", myController.canMoveColumns());
        return config.toString();
    }

    /**
     * Sends the config when it changed: the row lines, whether columns can be dragged and whether cells can be edited follow
     * the data source.
     */
    @RequiredUIAccess
    private void pushConfig() {
        String json = configJson();
        if (!json.equals(myConfigJson)) {
            myConfigJson = json;
            myTable.setConfig(json);
        }
    }

    /**
     * The plain editor font of the global colour scheme, the font of the cells, the headers and the cell editors - the font the
     * editors of the page are set in. Nothing of the scheme is readable in the browser, so it travels as the editor font does.
     */
    private static String fontJson() {
        Font font = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("family", font.getFamily());
        node.put("size", font.getFontSize());
        return node.toString();
    }

    private void onEditorSchemeChanged() {
        // a scheme change may arrive off the ui thread; a detached grid takes the font when it is attached again
        UIAccess uiAccess = getUIAccess();
        if (uiAccess != null) {
            uiAccess.giveIfNeed(() -> myTable.setFont(fontJson()));
        }
    }

    // region client events

    private void installListeners() {
        Element element = myTable.getElement();

        // fetching rows changes nothing, and a request dropped while a modal dialog makes the grid inert would leave
        // its rows blank until the client asks again - so these two are taken while inert or disabled too
        listen(element, RevoDataGridVaadin.ROWS_NEEDED_EVENT, data -> {
            if (data.path("event.detail.gen").asInt(-1) == myRowsGeneration) {
                sendRows(data.path("event.detail.from").asInt(0), data.path("event.detail.to").asInt(0));
            }
        }, "gen", "from", "to")
            .allowInert()
            .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS);

        listen(element, RevoDataGridVaadin.VIEWPORT_EVENT, data -> {
            myVisibleFrom = Math.max(0, data.path("event.detail.from").asInt(0));
            myVisibleTo = Math.max(myVisibleFrom, data.path("event.detail.to").asInt(0));
        }, "from", "to")
            .allowInert()
            .setDisabledUpdateMode(DisabledUpdateMode.ALWAYS)
            .debounce(VIEWPORT_DEBOUNCE_MS);

        listen(element, RevoDataGridVaadin.COPY_EVENT, this::onCopy, "id", "rows", "cols");

        listen(element, RevoDataGridVaadin.HEADER_CLICK_EVENT, data -> {
            int column = data.path("event.detail.column").asInt(-1);
            if (column >= 0 && column < myController.getViewColumnCount()) {
                myController.onColumnHeaderClicked(column,
                    data.path("event.detail.extend").asBoolean(false),
                    data.path("event.detail.toggle").asBoolean(false));
            }
        }, "column", "extend", "toggle");

        listen(element, RevoDataGridVaadin.HEADER_SORT_EVENT, data -> {
            int column = data.path("event.detail.column").asInt(-1);
            if (column >= 0 && column < myController.getViewColumnCount()) {
                myController.onColumnHeaderSortClicked(column, data.path("event.detail.additive").asBoolean(false));
            }
        }, "column", "additive");

        listen(element, RevoDataGridVaadin.CONTEXT_MENU_EVENT, this::onContextMenu,
            "area", "row", "col", "x", "y", "screenX", "screenY", "keyboard", "key", "alt", "ctrl", "shift", "meta");

        listen(element, RevoDataGridVaadin.COLUMN_RESIZE_EVENT, data -> {
            int column = data.path("event.detail.column").asInt(-1);
            int chars = data.path("event.detail.chars").asInt(0);
            if (column >= 0 && column < myController.getViewColumnCount() && chars > 0) {
                myController.onColumnResized(column, chars);
            }
        }, "column", "chars");

        // the element never reorders its columns itself: they come back in the new order once the data source moved the
        // column (structureChanged), and stay as they are when the move is refused
        listen(element, RevoDataGridVaadin.COLUMN_MOVE_EVENT, data -> {
            int from = data.path("event.detail.from").asInt(-1);
            int to = data.path("event.detail.to").asInt(-1);
            int columnCount = myController.getViewColumnCount();
            if (from >= 0 && from < columnCount && to >= 0 && to < columnCount && from != to) {
                myController.onColumnMoved(from, to);
            }
        }, "from", "to");

        String[] range = {"rowStart", "rowEnd", "colStart", "colEnd", "leadRow", "leadCol"};
        listen(element, RevoDataGridVaadin.SELECTION_EVENT, data -> onSelection(data, false), range);
        listen(element, RevoDataGridVaadin.SELECTION_ADJUSTING_EVENT, data -> onSelection(data, true), range);

        listen(element, RevoDataGridVaadin.ROW_TOGGLE_EVENT, data -> onRowToggled(data.path("event.detail.row").asInt(-1)), "row");

        listen(element, RevoDataGridVaadin.EDIT_START_EVENT, this::onEditStart, "seq", "row", "col", "typed", "mouse");
        listen(element, RevoDataGridVaadin.EDIT_INPUT_EVENT, this::onEditInput, "seq", "text")
            .debounce(EDIT_INPUT_DEBOUNCE_MS);
        listen(element, RevoDataGridVaadin.EDIT_COMMIT_EVENT, this::onEditCommit, "seq", "text", "option", "reason");
        listen(element, RevoDataGridVaadin.EDIT_CANCEL_EVENT, this::onEditCancel, "seq");

        // the editor of the client goes with the element - an edit it showed is over
        myTable.addDetachListener(event -> onDetached());
        // the scheme changed while the element was not attached
        myTable.addAttachListener(event -> myTable.setFont(fontJson()));
    }

    private static DomListenerRegistration listen(Element element, String event, Consumer<JsonNode> handler, String... detailFields) {
        DomListenerRegistration registration = element.addEventListener(event, domEvent -> handler.accept(domEvent.getEventData()));
        for (String field : detailFields) {
            registration.addEventData("event.detail." + field);
        }
        return registration;
    }

    @RequiredUIAccess
    private void onSelection(JsonNode data, boolean adjusting) {
        int rowCount = myController.getViewRowCount();
        int columnCount = myController.getViewColumnCount();
        int rowStart = Math.max(0, data.path("event.detail.rowStart").asInt(0));
        int rowEnd = Math.min(rowCount - 1, data.path("event.detail.rowEnd").asInt(-1));
        int colStart = Math.max(0, data.path("event.detail.colStart").asInt(0));
        int colEnd = Math.min(columnCount - 1, data.path("event.detail.colEnd").asInt(-1));
        int[] rows = rowEnd >= rowStart ? IntStream.rangeClosed(rowStart, rowEnd).toArray() : new int[0];
        int[] columns = colEnd >= colStart ? IntStream.rangeClosed(colStart, colEnd).toArray() : new int[0];
        // RevoGrid's focused cell, where its editor opens - the cell editSelectedCell() edits
        int leadRow = data.path("event.detail.leadRow").asInt(-1);
        int leadColumn = data.path("event.detail.leadCol").asInt(-1);
        if (leadRow < rowStart || leadRow > rowEnd || leadColumn < colStart || leadColumn > colEnd) {
            leadRow = -1;
            leadColumn = -1;
        }
        // the grid already shows what was selected - nothing is pushed back
        myController.onNativeSelectionChanged(rows, columns, adjusting, leadRow, leadColumn);
    }

    @RequiredUIAccess
    private void onRowToggled(int row) {
        if (row < 0 || row >= myController.getViewRowCount()) {
            return;
        }
        Set<Integer> rows = new TreeSet<>();
        for (int selected : myController.getSelectedViewRows()) {
            rows.add(selected);
        }
        if (!rows.remove(row)) {
            rows.add(row);
        }
        myController.onNativeSelectionChanged(rows.stream().mapToInt(Integer::intValue).toArray(), allViewColumns(), false);
        // a toggled set of rows is no rectangle - the client draws it from the controller's selection
        pushSelectionUnlessEditing();
    }

    /**
     * A copy of rows the client does not hold (never scrolled to, or stale after a sort): the tab separated texts of the
     * cells, as the grid shows them - the lines of a cell joined by {@code ⏎}, as the client copies them.
     */
    @RequiredUIAccess
    private void onCopy(JsonNode data) {
        int rowCount = myController.getViewRowCount();
        int columnCount = myController.getViewColumnCount();
        StringBuilder tsv = new StringBuilder();
        for (JsonNode run : data.path("event.detail.rows")) {
            int first = Math.max(0, run.path(0).asInt(0));
            int last = Math.min(rowCount - 1, run.path(1).asInt(-1));
            for (int row = first; row <= last; row++) {
                if (!tsv.isEmpty()) {
                    tsv.append('\n');
                }
                boolean firstColumn = true;
                for (JsonNode columnRun : data.path("event.detail.cols")) {
                    int colStart = Math.max(0, columnRun.path(0).asInt(0));
                    int colEnd = Math.min(columnCount - 1, columnRun.path(1).asInt(-1));
                    for (int column = colStart; column <= colEnd; column++) {
                        if (!firstColumn) {
                            tsv.append('\t');
                        }
                        firstColumn = false;
                        tsv.append(render(row, column).text().replace('\n', '⏎'));
                    }
                }
            }
        }
        myTable.resolveCopy(data.path("event.detail.id").asInt(0), tsv.toString());
    }

    /**
     * {@code consulo-grid-contextmenu}: the element hit-tested a context menu gesture. The controller selects what the menu was
     * opened on ({@link DataGridController#onContextMenuRequested}), then the {@link ContextMenuEvent} of the grid fires, at a
     * position relative to the grid. The menu key and shift F10 open the menu of the lead cell.
     */
    @RequiredUIAccess
    private void onContextMenu(JsonNode data) {
        boolean keyboard = data.path("event.detail.keyboard").asBoolean(false);
        GridHitArea area;
        int row;
        int column;
        if (keyboard) {
            area = GridHitArea.CELL;
            row = myController.getLeadViewRow();
            column = myController.getLeadViewColumn();
        }
        else {
            area = toHitArea(data.path("event.detail.area").asString(""));
            row = data.path("event.detail.row").asInt(-1);
            column = data.path("event.detail.col").asInt(-1);
        }
        if (!myController.onContextMenuRequested(area, row, column)) {
            // the open edit could not be committed: it stays, with its error, and no menu opens
            return;
        }

        int x = data.path("event.detail.x").asInt(0);
        int y = data.path("event.detail.y").asInt(0);
        int screenX = data.path("event.detail.screenX").asInt(0);
        int screenY = data.path("event.detail.screenY").asInt(0);
        boolean alt = data.path("event.detail.alt").asBoolean(false);
        boolean ctrl = data.path("event.detail.ctrl").asBoolean(false);
        boolean shift = data.path("event.detail.shift").asBoolean(false);
        boolean meta = data.path("event.detail.meta").asBoolean(false);
        InputDetails details;
        if (keyboard) {
            EnumSet<ModifiedInputDetails.Modifier> modifiers = EnumSet.noneOf(ModifiedInputDetails.Modifier.class);
            if (alt) {
                modifiers.add(ModifiedInputDetails.Modifier.ALT);
            }
            if (ctrl) {
                modifiers.add(ModifiedInputDetails.Modifier.CTRL);
            }
            if (shift) {
                modifiers.add(ModifiedInputDetails.Modifier.SHIFT);
            }
            if (meta) {
                modifiers.add(ModifiedInputDetails.Modifier.META);
            }
            KeyCode key = "F10".equals(data.path("event.detail.key").asString("")) ? F10_KEY : CONTEXT_MENU_KEY;
            details = new KeyboardInputDetails(new Point2D(x, y), new Point2D(screenX, screenY), modifiers, key);
        }
        else {
            details = WebInputDetails.mouse(x, y, screenX, screenY, 2, alt, ctrl, shift, meta);
        }
        getListenerDispatcher(ContextMenuEvent.class).onEvent(new ContextMenuEvent(this, details));
    }

    private static GridHitArea toHitArea(String area) {
        return switch (area) {
            case "CELL" -> GridHitArea.CELL;
            case "COLUMN_HEADER" -> GridHitArea.COLUMN_HEADER;
            case "ROW_HEADER" -> GridHitArea.ROW_HEADER;
            default -> GridHitArea.EMPTY;
        };
    }

    // endregion

    // region columns

    @RequiredUIAccess
    private void rebuildColumns() {
        myColumnsGeneration++;
        myAutoWidths = new int[myController.getViewColumnCount()];
        myColumnWidthsPending = true;
        updateColumnWidths();
        pushColumns();
    }

    /**
     * The columns with their widths: the width the controller keeps for a column (one the user dragged, or one set through the
     * grid), else the width measured here - in characters of the grid font, without the padding of the cell, which the client
     * adds.
     */
    @RequiredUIAccess
    private void pushColumns() {
        ObjectNode columns = JsonNodeFactory.instance.objectNode();
        columns.put("gen", myColumnsGeneration);
        ArrayNode items = columns.putArray("items");
        for (int i = 0; i < myController.getViewColumnCount(); i++) {
            ObjectNode item = items.addObject();
            item.put("title", columnTitle(i));
            item.put("sort", sortMarker(i));
            item.put("tooltip", myController.getColumnTooltip(i));
            item.put("align", toAlign(myController.getColumnAlignment(i)));
            item.put("sortable", myController.isColumnSortable(i));
            int width = myController.getViewColumnWidth(i);
            if (width <= 0) {
                width = i < myAutoWidths.length ? myAutoWidths[i] : MIN_COLUMN_CHARS;
            }
            item.put("widthCh", width);
        }
        myTable.setColumns(columns.toString());
    }

    private String columnTitle(int viewColumn) {
        GridColumn column = myController.getColumn(viewColumn);
        return column == null ? "" : myController.getName(column);
    }

    /**
     * The sort marker of a column header: an arrow, and the priority when several columns are sorted; empty when the column is
     * not sorted.
     */
    private String sortMarker(int viewColumn) {
        ModelIndex<GridColumn> column = myController.toModelColumn(viewColumn);
        RowSortOrder.Type order = myController.getSortOrder(column);
        if (order == RowSortOrder.Type.UNSORTED) {
            return "";
        }
        String arrow = order == RowSortOrder.Type.ASC ? "▲" : "▼";
        return myController.countSortedColumns() > 1 ? arrow + myController.getThenBySortOrder(column) : arrow;
    }

    /**
     * Sizes the columns by their header and the lines of the first rows, in characters of the grid font. Once rows are there, the
     * width of a column the controller keeps none for is reported to it, so the column keeps it when the columns change - as on
     * every frontend.
     */
    @RequiredUIAccess
    private void updateColumnWidths() {
        int rowCount = myController.getViewRowCount();
        int sampleRows = Math.min(rowCount, WIDTH_SAMPLE_ROWS);
        for (int column = 0; column < myAutoWidths.length; column++) {
            int chars = textWidth(columnTitle(column)) + (myController.isColumnSortable(column) ? SORT_MARKER_CHARS : 0);
            for (int row = 0; row < sampleRows; row++) {
                for (String line : myController.getCellLines(row, column)) {
                    chars = Math.max(chars, textWidth(line));
                }
            }
            myAutoWidths[column] = Math.max(MIN_COLUMN_CHARS, Math.min(MAX_COLUMN_CHARS, chars));
        }
        if (rowCount > 0) {
            myColumnWidthsPending = false;
            for (int column = 0; column < myAutoWidths.length; column++) {
                if (myController.getViewColumnWidth(column) <= 0) {
                    myController.onColumnResized(column, myAutoWidths[column]);
                }
            }
        }
    }

    private static int textWidth(String text) {
        return text.codePointCount(0, text.length());
    }

    /**
     * The width of the row numbers in characters of the grid font: the longest number, and the padding of the row header cells,
     * which RevoGrid pads by 1em on each side - 4ch of a monospaced font cover the 2em.
     */
    private int rowNumberWidth() {
        int digits = 2;
        int rowCount = myController.getViewRowCount();
        for (int row = 0; row < rowCount; row++) {
            digits = Math.max(digits, myController.getRowNumberText(row).length());
        }
        return digits + 4;
    }

    private static String toAlign(HorizontalAlignment alignment) {
        return switch (alignment) {
            case LEFT -> "left";
            case CENTER -> "center";
            case RIGHT -> "right";
        };
    }

    // endregion

    // region rows

    @RequiredUIAccess
    private void pushRowModel() {
        myRowsGeneration++;
        ObjectNode model = JsonNodeFactory.instance.objectNode();
        model.put("gen", myRowsGeneration);
        model.put("count", myController.getViewRowCount());
        model.put("rowNumberWidthCh", rowNumberWidth());
        myTable.setRowModel(model.toString());

        // the rows on screen, without waiting for the client to ask
        sendRows(visibleChunksFrom(), visibleChunksTo());
    }

    /**
     * The first row of the chunks on screen - the client asks for whole chunks, so a chunk sent in part would still be
     * stale there and asked for again.
     */
    private int visibleChunksFrom() {
        return (myVisibleFrom / CHUNK_SIZE) * CHUNK_SIZE;
    }

    private int visibleChunksTo() {
        int to = myVisibleTo > myVisibleFrom ? ((myVisibleTo + CHUNK_SIZE - 1) / CHUNK_SIZE) * CHUNK_SIZE : 0;
        return Math.min(myController.getViewRowCount(), to);
    }

    @RequiredUIAccess
    private void sendRows(int from, int to) {
        int rowCount = myController.getViewRowCount();
        int start = Math.max(0, from);
        int end = Math.min(rowCount, to);
        if (start >= end) {
            return;
        }
        int columnCount = myController.getViewColumnCount();
        boolean rowNumbers = myController.isShowRowNumbers();
        // the colours of the cells as indices into the css colours of this response (1-based, 0 = none): a coloured
        // column repeats its colour in every row, which is resolved to its css colour once
        Map<String, Integer> colors = new LinkedHashMap<>();
        Map<ColorValue, Integer> colorIndices = new HashMap<>();
        ArrayNode rows = JsonNodeFactory.instance.arrayNode();
        for (int row = start; row < end; row++) {
            ObjectNode node = rows.addObject();
            node.put("n", rowNumbers ? myController.getRowNumberText(row) : "");
            ArrayNode texts = node.putArray("t");
            int[] flags = null;
            int[] backgrounds = null;
            int[] foregrounds = null;
            for (int column = 0; column < columnCount; column++) {
                RenderedCell cell = render(row, column);
                texts.add(cell.text());
                if (cell.flags() != 0) {
                    if (flags == null) {
                        flags = new int[columnCount];
                    }
                    flags[column] = cell.flags();
                }
                ColorValue background = cell.background();
                if (background != null) {
                    if (backgrounds == null) {
                        backgrounds = new int[columnCount];
                    }
                    backgrounds[column] = colorIndex(colors, colorIndices, background);
                }
                ColorValue foreground = cell.foreground();
                if (foreground != null) {
                    if (foregrounds == null) {
                        foregrounds = new int[columnCount];
                    }
                    foregrounds[column] = colorIndex(colors, colorIndices, foreground);
                }
            }
            putInts(node, "f", flags);
            putInts(node, "b", backgrounds);
            putInts(node, "c", foregrounds);
            ColorValue rowHeaderBackground = myController.getRowHeaderBackground(row);
            if (rowHeaderBackground != null) {
                node.put("m", colorIndex(colors, colorIndices, rowHeaderBackground));
            }
        }
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        ArrayNode cssColors = payload.putArray("v");
        for (String color : colors.keySet()) {
            cssColors.add(color);
        }
        payload.set("r", rows);
        myTable.putRows(myRowsGeneration, start, payload.toString());
    }

    /**
     * The index of a colour in the css colours of a response (1-based, 0 = none). A colour is resolved to its css colour
     * ({@link #toCssColor}) the first time the response meets it; {@code colorIndices} remembers the index of each colour met.
     */
    private static int colorIndex(Map<String, Integer> colors, Map<ColorValue, Integer> colorIndices, ColorValue color) {
        Integer index = colorIndices.get(color);
        if (index != null) {
            return index;
        }
        String cssColor = toCssColor(color);
        if (cssColor == null) {
            index = 0;
        }
        else {
            index = colors.get(cssColor);
            if (index == null) {
                index = colors.size() + 1;
                colors.put(cssColor, index);
            }
        }
        colorIndices.put(color, index);
        return index;
    }

    private static void putInts(ObjectNode node, String name, int @Nullable [] values) {
        if (values == null) {
            return;
        }
        ArrayNode array = node.putArray(name);
        for (int value : values) {
            array.add(value);
        }
    }

    /**
     * The text the controller renders for a cell, the {@code FLAG_*} styles of its fragments, and its colours, which
     * {@link #sendRows} sends as css colours (see {@link #toCssColor}): the background from the color model (for example the
     * one of a pending change), the text color its value is rendered in. A selected cell keeps both - the client paints the
     * selection over them.
     */
    @RequiredUIAccess
    private RenderedCell render(int viewRow, int viewColumn) {
        WebItemPresentationImpl presentation = new WebItemPresentationImpl();
        // the background goes as a css colour of its own, under the selection the client paints
        myController.renderCell(presentation, viewRow, viewColumn, false);
        StringBuilder text = new StringBuilder();
        int flags = myController.isNullValue(viewRow, viewColumn) ? FLAG_NULL : 0;
        ColorValue foreground = null;
        for (WebItemPresentationImpl.Fragment fragment : presentation.getFragments()) {
            text.append(fragment.text());
            TextAttribute attribute = fragment.attribute();
            if (attribute == TextAttribute.GRAYED) {
                flags |= FLAG_GRAYED;
            }
            else if (attribute == TextAttribute.ERROR) {
                flags |= FLAG_ERROR;
            }
            else if (attribute != null && foreground == null) {
                foreground = attribute.getForegroundColor();
            }
        }
        if (flags != 0) {
            // null, reserved and failed values keep the text style of the client
            foreground = null;
        }
        return new RenderedCell(text.toString(), flags, myController.getCellBackground(viewRow, viewColumn), foreground);
    }

    private record RenderedCell(String text, int flags, @Nullable ColorValue background, @Nullable ColorValue foreground) {
    }

    /**
     * A colour as the css value the client sets: a colour of the style which has a style variable ({@code --consulo-...}, see
     * {@link WebStyleCssRegistry}) as {@code var(--consulo-...)}, so that it follows a change of the style on the client; any
     * other colour as the literal colour of its rgb, its alpha included ({@link WebColors#toCssColor}). {@code null} for no
     * colour.
     */
    private static @Nullable String toCssColor(@Nullable ColorValue color) {
        if (color == null) {
            return null;
        }
        if (color instanceof StyleColorValue styleColor) {
            String cssColor = ourStyleCssColors.get(styleColor);
            if (cssColor != null) {
                return cssColor;
            }
            String key = StyleColorKeys.getKey(styleColor);
            if (key != null) {
                cssColor = "var(" + WebStyleCssRegistry.toVariableName(key) + ")";
                ourStyleCssColors.put(styleColor, cssColor);
                return cssColor;
            }
        }
        return WebColors.toCssColor(color);
    }

    @RequiredUIAccess
    private void reloadRows() {
        if (myColumnWidthsProvisional) {
            // the reload sent together with the structure change - its rows are the ones rebuildColumns() just measured
            myColumnWidthsProvisional = false;
        }
        else if (myColumnWidthsPending) {
            updateColumnWidths();
            pushColumns();
        }
        pushRowModel();
        // the controller keeps the selection over the change - a selection it cleared is cleared here too
        pushSelection();
    }

    @RequiredUIAccess
    private void refreshRows(int firstViewRow, int lastViewRow) {
        // rows replaced in place after the columns changed are the first data of those columns
        if (myColumnWidthsPending) {
            updateColumnWidths();
            pushColumns();
        }
        int last = Math.min(lastViewRow, myController.getViewRowCount() - 1);
        if (last < firstViewRow) {
            return;
        }
        myTable.invalidateRows(myRowsGeneration, firstViewRow, last);
        // the changed rows on screen go along at once, the others are asked for when they scroll in
        sendRows(Math.max(firstViewRow, visibleChunksFrom()), Math.min(last + 1, visibleChunksTo()));
    }

    // endregion

    // region editing

    /**
     * {@code consulo-grid-edit-start}: the client opened an edit in a cell, and the controller creates the editor of the cell
     * ({@link DataGridController#startEditing}). The client shows its field once {@link RevoDataGridVaadin#beginEdit} tells it
     * which.
     */
    @RequiredUIAccess
    private void onEditStart(JsonNode data) {
        int seq = data.path("event.detail.seq").asInt(-1);
        if (seq < 0) {
            return;
        }
        int row = data.path("event.detail.row").asInt(-1);
        int column = data.path("event.detail.col").asInt(-1);
        String typed = data.path("event.detail.typed").asString("");
        boolean mouse = data.path("event.detail.mouse").asBoolean(false);

        DataGridEditSession session = null;
        if (row >= 0 && row < myController.getViewRowCount() && column >= 0 && column < myController.getViewColumnCount()) {
            GridEditInitiator initiator;
            if (!typed.isEmpty()) {
                initiator = GridEditInitiator.typed(typed);
            }
            else {
                initiator = mouse ? GridEditInitiator.MOUSE : GridEditInitiator.ACTION;
            }
            // the same cell again (an editor opened anew after a refused move) gets the edit which is open, with its error
            session = myController.startEditing(row, column, initiator);
        }
        if (session == null) {
            // the cell is not edited, or the editor decided its value at once (a boolean set by a key), which is written already
            editClosed();
            myTable.endEdit(seq);
            return;
        }
        myEditSeq = seq;
        myEditSession = session;
        myEditSelectionRows = myController.getSelectedViewRows();
        myEditSelectionColumns = myController.getSelectedViewColumns();
        myTable.beginEdit(seq, editorJson(session));
    }

    /**
     * {@code consulo-grid-edit-input}: the text of the field changed - the edit gets it as every change of its text, so the
     * cells of a multi-cell edit show it, and a commit through the API has it.
     */
    @RequiredUIAccess
    private void onEditInput(JsonNode data) {
        DataGridEditSession session = getEditSession(data.path("event.detail.seq").asInt(-1));
        if (session != null) {
            setText(session, data.path("event.detail.text").asString(""));
        }
    }

    /**
     * {@code consulo-grid-edit-commit} - the editor stops ({@link DataGridController#commitEditing()}), for each gesture which
     * commits: Enter in the field, a move to another cell, the focus leaving the grid.
     */
    @RequiredUIAccess
    private void onEditCommit(JsonNode data) {
        int seq = data.path("event.detail.seq").asInt(-1);
        DataGridEditSession session = getEditSession(seq);
        if (session == null) {
            return;
        }
        GridCellEditorPresentation presentation = session.getPresentation();
        if (presentation.kind() == GridCellEditorPresentation.Kind.LIST) {
            int option = data.path("event.detail.option").asInt(-1);
            if (option >= 0 && option < presentation.options().size()) {
                session.selectOption(option);
            }
        }
        else {
            setText(session, data.path("event.detail.text").asString(""));
        }

        if (myController.commitEditing() || myController.getEditSession() != session) {
            editClosed();
            myTable.endEdit(seq);
            return;
        }

        switch (data.path("event.detail.reason").asString(REASON_ENTER)) {
            case REASON_NAVIGATE -> reopenRefusedEdit(session);
            case REASON_BLUR -> {
                if (session.isWaitingForAnswer()) {
                    // the question about unsubmitted changes took the focus - the editor stays until it is answered
                    myTable.editError(seq, "");
                }
                else {
                    // the focus went to another component: the edit is committed, or else discarded
                    myController.discardEditing();
                    editClosed();
                    myTable.endEdit(seq);
                }
            }
            default -> myTable.editError(seq, errorJson(session));
        }
    }

    /**
     * A move to another cell whose commit was refused. The selection stays on the edited cell: the grid stops the editor
     * before it changes the selection, and the editor stays when that fails. RevoGrid has moved already and dropped its
     * editor, so the selection is put back - the one selection pushed while an edit is open - and the editor opened there
     * again, which shows the text and the error of the edit.
     */
    @RequiredUIAccess
    private void reopenRefusedEdit(DataGridEditSession session) {
        int row = session.getViewRow();
        int column = session.getViewColumn();
        if (row < 0 || column < 0) {
            int seq = myEditSeq;
            myController.discardEditing();
            editClosed();
            myTable.endEdit(seq);
            return;
        }
        boolean inSelection = contains(myEditSelectionRows, row) && contains(myEditSelectionColumns, column);
        int[] rows = inSelection ? myEditSelectionRows : new int[]{row};
        int[] columns = inSelection ? myEditSelectionColumns : new int[]{column};
        myController.onNativeSelectionChanged(rows, columns, false, row, column);
        pushSelection();
        // the client focuses the cell and opens its editor, and answers with consulo-grid-edit-start - or with
        // consulo-grid-edit-cancel when no editor could open, which drops the edit
        myTable.requestEdit(row, column, "");
    }

    /**
     * {@code consulo-grid-edit-cancel} - the editor is cancelled ({@link DataGridController#discardEditing()}): Escape, a list
     * closed without a choice, or an edit the client could not open a field for.
     */
    @RequiredUIAccess
    private void onEditCancel(JsonNode data) {
        if (getEditSession(data.path("event.detail.seq").asInt(-1)) != null) {
            myController.discardEditing();
            editClosed();
        }
    }

    /**
     * A grid which is hidden commits its edit, or else discards it - with the text the client reported last.
     */
    @RequiredUIAccess
    private void onDetached() {
        DataGridEditSession session = myEditSession;
        if (session != null && myController.getEditSession() == session && !myController.commitEditing()
            && myController.getEditSession() == session && !session.isWaitingForAnswer()) {
            myController.discardEditing();
        }
        editClosed();
    }

    /**
     * The edit session the client edit {@code seq} shows, or {@code null} when that edit is stale.
     */
    private @Nullable DataGridEditSession getEditSession(int seq) {
        DataGridEditSession session = myController.getEditSession();
        if (seq < 0 || seq != myEditSeq || session == null || session != myEditSession) {
            return null;
        }
        return session;
    }

    /**
     * Only a change of the text is one: setting the text an edit already holds would count as typed, and write it into every
     * cell of a multi-cell edit.
     */
    @RequiredUIAccess
    private static void setText(DataGridEditSession session, String text) {
        GridCellEditorPresentation presentation = session.getPresentation();
        if (presentation.isTextKind() && !presentation.readOnly() && !text.equals(session.getText())) {
            session.setText(text);
        }
    }

    /**
     * The client edit is over - or never showed an edit session.
     */
    @RequiredUIAccess
    private void editClosed() {
        myEditSeq = -1;
        myEditSession = null;
        flushDeferredSelection();
    }

    @RequiredUIAccess
    private void flushDeferredSelection() {
        if (mySelectionDeferred && !myController.isEditing()) {
            pushSelection();
        }
    }

    private static String editorJson(DataGridEditSession session) {
        GridCellEditorPresentation presentation = session.getPresentation();
        ObjectNode spec = JsonNodeFactory.instance.objectNode();
        spec.put("kind", switch (presentation.kind()) {
            case MULTILINE_TEXT -> "multiline";
            case LIST -> "list";
            case TEXT, NONE -> "text";
        });
        spec.put("text", presentation.text());
        spec.put("typed", session.getTypedText());
        spec.put("selectAll", presentation.selectAll());
        spec.put("readOnly", presentation.readOnly());
        spec.put("readOnlyHint", presentation.readOnlyHint().get());
        spec.put("align", toAlign(presentation.alignment()));
        spec.put("placeholder", presentation.placeholder());
        ArrayNode options = spec.putArray("options");
        for (String option : presentation.options()) {
            options.add(option);
        }
        spec.put("selected", presentation.selectedOption());
        UnparsedValue.ParsingError error = session.getError();
        if (error != null) {
            spec.set("error", toJson(error));
        }
        else {
            spec.putNull("error");
        }
        return spec.toString();
    }

    /**
     * Why the commit was refused, as {@link RevoDataGridVaadin#editError} takes it.
     */
    private static String errorJson(DataGridEditSession session) {
        UnparsedValue.ParsingError error = session.getError();
        return error == null ? "" : toJson(error).toString();
    }

    private static ObjectNode toJson(UnparsedValue.ParsingError error) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("message", error.message());
        node.put("offset", error.offset());
        return node;
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) {
            if (v == value) {
                return true;
            }
        }
        return false;
    }

    // endregion

    // region selection

    /**
     * Shows the selection of the controller: one rectangle when its rows and columns are each one run, row tints when not.
     */
    @RequiredUIAccess
    private void pushSelection() {
        mySelectionDeferred = false;
        ObjectNode selection = JsonNodeFactory.instance.objectNode();
        selection.put("seq", ++mySelectionSeq);
        selection.set("rows", toRuns(myController.getSelectedViewRows(), myController.getViewRowCount()));
        selection.set("cols", toRuns(myController.getSelectedViewColumns(), myController.getViewColumnCount()));
        int leadRow = myController.getLeadViewRow();
        int leadColumn = myController.getLeadViewColumn();
        if (leadRow >= 0 && leadColumn >= 0) {
            selection.putArray("lead").add(leadRow).add(leadColumn);
        }
        myTable.setSelection(selection.toString());
    }

    /**
     * Setting the selection on the client while it shows an editor would close the editor there, and commit it - so a
     * selection the controller sets while an edit is open waits for the edit to end.
     */
    @RequiredUIAccess
    private void pushSelectionUnlessEditing() {
        if (myController.isEditing()) {
            mySelectionDeferred = true;
            return;
        }
        pushSelection();
    }

    private static ArrayNode toRuns(int[] indices, int count) {
        int[] sorted = IntStream.of(indices).filter(index -> index >= 0 && index < count).distinct().sorted().toArray();
        ArrayNode runs = JsonNodeFactory.instance.arrayNode();
        int i = 0;
        while (i < sorted.length) {
            int start = sorted[i];
            int end = start;
            while (i + 1 < sorted.length && sorted[i + 1] == end + 1) {
                end = sorted[++i];
            }
            runs.addArray().add(start).add(end);
            i++;
        }
        return runs;
    }

    private int[] allViewColumns() {
        return IntStream.range(0, myController.getViewColumnCount()).toArray();
    }

    // endregion

    private final class ViewImpl implements DataGridController.View {
        @Override
        @RequiredUIAccess
        public void structureChanged() {
            pushConfig();
            rebuildColumns();
            if (myController.getViewRowCount() > 0) {
                myColumnWidthsPending = true;
                myColumnWidthsProvisional = true;
            }
        }

        @Override
        @RequiredUIAccess
        public void rowsChanged() {
            pushConfig();
            reloadRows();
        }

        @Override
        @RequiredUIAccess
        public void cellsChanged(int firstViewRow, int lastViewRow) {
            pushConfig();
            refreshRows(firstViewRow, lastViewRow);
            // an edit the controller ended by itself (a value decided at once, written after a question) leaves no other trace here
            flushDeferredSelection();
        }

        /**
         * The widths the controller keeps changed (set or fitted through the grid): the columns go again with them.
         */
        @Override
        @RequiredUIAccess
        public void columnWidthsChanged() {
            pushColumns();
        }

        /**
         * The rows show another number of lines: the client takes the new pitch from the config, and the rows it holds are
         * laid out for the previous lines - they go again, and the columns are measured again by their new lines.
         */
        @Override
        @RequiredUIAccess
        public void rowHeightsChanged() {
            pushConfig();
            int rowCount = myController.getViewRowCount();
            if (rowCount == 0) {
                return;
            }
            updateColumnWidths();
            pushColumns();
            myTable.invalidateRows(myRowsGeneration, 0, rowCount - 1);
            sendRows(visibleChunksFrom(), visibleChunksTo());
        }

        @Override
        @RequiredUIAccess
        public void toolbarVisibilityChanged() {
            myToolbar.setVisible(myController.isToolbarVisible());
            // the bar changes with the state of the data source, which decides whether columns can be dragged too
            pushConfig();
        }

        @Override
        @RequiredUIAccess
        public void headersChanged() {
            pushColumns();
        }

        @Override
        @RequiredUIAccess
        public void selectionChanged() {
            pushSelectionUnlessEditing();
        }

        @Override
        @RequiredUIAccess
        public void scrollToCell(int viewRow, int viewColumn) {
            myTable.scrollToCell(viewRow < myController.getViewRowCount() ? viewRow : -1,
                viewColumn < myController.getViewColumnCount() ? viewColumn : -1);
        }

        /**
         * The client focuses the cell, opens its editor there and asks {@link DataGridController#startEditing} for the edit
         * ({@code consulo-grid-edit-start}).
         */
        @Override
        @RequiredUIAccess
        public void editCellAt(int viewRow, int viewColumn, GridEditInitiator initiator) {
            myTable.requestEdit(viewRow, viewColumn, initiator.isKey() ? initiator.typedText() : "");
        }

        /**
         * Commits with the text the client reported last ({@code consulo-grid-edit-input}); a refused commit shows its error in
         * the field, which stays.
         */
        @Override
        @RequiredUIAccess
        public boolean stopCellEditor() {
            DataGridEditSession session = myController.getEditSession();
            if (session == null) {
                return true;
            }
            int seq = myEditSeq;
            if (myController.commitEditing()) {
                editClosed();
                if (seq >= 0) {
                    myTable.endEdit(seq);
                }
                return true;
            }
            if (seq >= 0 && myController.getEditSession() == session) {
                myTable.editError(seq, errorJson(session));
            }
            return false;
        }

        @Override
        @RequiredUIAccess
        public void cancelCellEditor() {
            int seq = myEditSeq;
            myController.discardEditing();
            editClosed();
            if (seq >= 0) {
                myTable.endEdit(seq);
            }
        }
    }
}
