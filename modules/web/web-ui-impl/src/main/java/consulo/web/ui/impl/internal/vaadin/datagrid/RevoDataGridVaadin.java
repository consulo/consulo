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
package consulo.web.ui.impl.internal.vaadin.datagrid;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

/**
 * The client side of the web data grid: {@code <consulo-data-grid>}, a light DOM element over RevoGrid. It holds no data
 * of its own - the state is five JSON properties ({@link #setConfig}, {@link #setColumns}, {@link #setRowModel},
 * {@link #setSelection}, {@link #setFont}) which Flow re-sends whenever the element is attached again, and the rows are
 * fetched by the element in chunks as it scrolls.
 * <p/>
 * Events it fires (all on this element, detail fields as named):
 * <ul>
 * <li>{@code consulo-grid-rows-needed} {gen, from, to} - rows {@code [from, to)} of row generation {@code gen}</li>
 * <li>{@code consulo-grid-header-click} {column, extend, toggle} - a press on a column header outside its sort marker:
 * {@code extend} with shift, {@code toggle} with control or meta</li>
 * <li>{@code consulo-grid-header-sort} {column, additive} - a press on the sort marker of a column header</li>
 * <li>{@code consulo-grid-selection} / {@code consulo-grid-selection-adjusting} {rowStart, rowEnd, colStart, colEnd,
 * leadRow, leadCol} - the lead is RevoGrid's focused cell, or -1</li>
 * <li>{@code consulo-grid-row-toggle} {row} - a control press on a cell</li>
 * <li>{@code consulo-grid-viewport} {gen, from, to} - rows {@code [from, to)} are on screen, sent with every scroll frame</li>
 * <li>{@code consulo-grid-copy} {id, rows: [[first, last], ...], cols: [[first, last], ...]} - a copy of rows the element
 * does not hold; answered by {@link #resolveCopy}</li>
 * <li>{@code consulo-grid-contextmenu} {area: CELL|COLUMN_HEADER|ROW_HEADER|EMPTY, row, col, x, y, screenX, screenY,
 * keyboard, key, alt, ctrl, shift, meta} - a context menu gesture, hit-tested: {@code row} / {@code col} are view indices or
 * -1; {@code x} / {@code y} are relative to the parent element of this one (the box which holds the grid and its paging
 * bar). {@code keyboard} for the menu key or shift F10 ({@code key}: ContextMenu|F10), placed at the lead cell</li>
 * <li>{@code consulo-grid-column-resize} {column, chars} - the user dragged the width of a column: its width in characters
 * of the grid font, without the padding of the cell</li>
 * <li>{@code consulo-grid-column-move} {from, to} - the user dropped a dragged column at another place. The element keeps
 * its columns as they are: they come in the new order with the next {@link #setColumns}</li>
 * </ul>
 * Cell editing - the element hosts a native field in the cell, the controller decides everything else. Each edit the element
 * opens gets a new {@code seq}, which every message about it carries, so an answer for an edit which is over is dropped:
 * <ul>
 * <li>{@code consulo-grid-edit-start} {seq, row, col, typed, mouse} - an edit opened in the cell: by a double click
 * ({@code mouse}), Enter, F2, a typed key ({@code typed}), or by {@link #requestEdit}; answered by {@link #beginEdit}, or by
 * {@link #endEdit} when the cell is not edited</li>
 * <li>{@code consulo-grid-edit-input} {seq, text} - the text of the field changed</li>
 * <li>{@code consulo-grid-edit-commit} {seq, text, option, reason: enter|navigate|blur} - the field commits: Enter, a move
 * to another cell (the field is gone already), the focus leaving the grid; {@code option} is the chosen option of a list,
 * or -1. Answered by {@link #endEdit} when accepted, by {@link #editError} when refused and the field stays</li>
 * <li>{@code consulo-grid-edit-cancel} {seq} - Escape, a list closed without a choice, or an edit no field could open
 * for</li>
 * </ul>
 * The element names the keys it handles itself in {@code consulo-own-keys}, which the keymap of the page leaves to it.
 *
 * @since 2026-10-03
 */
@Tag("consulo-data-grid")
@NpmPackage(value = "@revolist/revogrid", version = "4.28.2")
// the standalone build of revogrid imports @stencil/core/internal/client without declaring it
@NpmPackage(value = "@stencil/core", version = "4.43.5")
@JsModule("./consulo-data-grid/consulo-data-grid.ts")
public class RevoDataGridVaadin extends Component implements FromVaadinComponentWrapper, HasSize {
    public static final String ROWS_NEEDED_EVENT = "consulo-grid-rows-needed";
    public static final String HEADER_CLICK_EVENT = "consulo-grid-header-click";
    public static final String HEADER_SORT_EVENT = "consulo-grid-header-sort";
    public static final String SELECTION_EVENT = "consulo-grid-selection";
    public static final String SELECTION_ADJUSTING_EVENT = "consulo-grid-selection-adjusting";
    public static final String ROW_TOGGLE_EVENT = "consulo-grid-row-toggle";
    public static final String VIEWPORT_EVENT = "consulo-grid-viewport";
    public static final String COPY_EVENT = "consulo-grid-copy";
    public static final String CONTEXT_MENU_EVENT = "consulo-grid-contextmenu";
    public static final String COLUMN_RESIZE_EVENT = "consulo-grid-column-resize";
    public static final String COLUMN_MOVE_EVENT = "consulo-grid-column-move";
    public static final String EDIT_START_EVENT = "consulo-grid-edit-start";
    public static final String EDIT_INPUT_EVENT = "consulo-grid-edit-input";
    public static final String EDIT_COMMIT_EVENT = "consulo-grid-edit-commit";
    public static final String EDIT_CANCEL_EVENT = "consulo-grid-edit-cancel";

    private final consulo.ui.Component myOwner;

    public RevoDataGridVaadin(consulo.ui.Component owner) {
        myOwner = owner;
    }

    /**
     * {rowNumbers, horizontalLines, striped, hoverRows, transparentColumnHeader, transparentRowHeader, chunkSize, editable,
     * rowLines, moveColumns} - {@code editable}: cells open editors (an edit is still refused per cell by the answer to
     * {@code consulo-grid-edit-start}); {@code rowLines}: the text lines of every row, which sets the row pitch;
     * {@code moveColumns}: columns can be dragged to another place ({@code consulo-grid-column-move})
     */
    public void setConfig(String json) {
        getElement().setProperty("config", json);
    }

    /**
     * {gen, items: [{title, sort, tooltip, align: left|center|right, sortable, widthCh}]} in view column order - {@code sort}:
     * the sort marker of the header, empty when the column is not sorted; {@code widthCh}: the width in characters of the
     * grid font, without the padding of the cell, which the element adds. A new {@code gen} is a new layout of the columns.
     */
    public void setColumns(String json) {
        getElement().setProperty("columns", json);
    }

    /**
     * {gen, count, rowNumberWidthCh}. A new {@code gen} marks every row stale; rows of an older generation are ignored.
     */
    public void setRowModel(String json) {
        getElement().setProperty("rowModel", json);
    }

    /**
     * {seq, rows: [[first, last], ...], cols: [[first, last], ...], lead?: [row, column]} - inclusive runs of view indices,
     * and the lead cell, which a rectangle keeps focused when it is one of its corners. {@code seq} grows on every push, so
     * the same selection set again still reaches the client.
     */
    public void setSelection(String json) {
        getElement().setProperty("selection", json);
    }

    /**
     * {family, size} - the font of the cells, the headers and the cell editors: a font family name and a size in pixels
     * (a size of 0 or less keeps the default size).
     */
    public void setFont(String json) {
        getElement().setProperty("font", json);
    }

    /**
     * Rows {@code [from, from + r.length)}: {v: [css colour...], r: [{n: row number text, t: [cell text...], f?: [flags...],
     * b?: [background...], c?: [foreground...], m?: row header background}]}. The text of a cell holds its lines separated by
     * {@code \n}; flags 1 null, 2 grayed, 4 error are text styles; a colour is a 1-based index into {@code v}, or 0 for none.
     * {@code v} holds css colour values: {@code var(--consulo-...)} for a colour of the style, which follows the style, a literal
     * colour ({@code #rrggbb}, {@code rgba(...)}) for any other one.
     */
    public void putRows(int gen, int from, String json) {
        getElement().callJsFunction("putRows", gen, from, json);
    }

    public void invalidateRows(int gen, int firstRow, int lastRow) {
        getElement().callJsFunction("invalidateRows", gen, firstRow, lastRow);
    }

    public void scrollToCell(int row, int column) {
        getElement().callJsFunction("scrollToCell", row, column);
    }

    public void focusGrid() {
        getElement().callJsFunction("focusGrid");
    }

    /**
     * The editor of edit {@code seq}: {kind: text|multiline|list, text, typed, selectAll, readOnly, readOnlyHint, align,
     * placeholder, options, selected, error: {message, offset} | null}. The field shows {@code text} followed by
     * {@code typed} and the keys typed while it waited for this.
     */
    public void beginEdit(int seq, String json) {
        getElement().callJsFunction("beginEdit", seq, json);
    }

    /**
     * Closes the field of edit {@code seq} without writing anything: the commit was accepted, the edit was cancelled, or the
     * cell is not edited.
     */
    public void endEdit(int seq) {
        getElement().callJsFunction("endEdit", seq);
    }

    /**
     * The commit of edit {@code seq} was refused and its field stays: {message, offset}, or an empty string when there is
     * nothing to show (the grid asks the user first).
     */
    public void editError(int seq, String json) {
        getElement().callJsFunction("editError", seq, json);
    }

    /**
     * Opens the field of a cell, focusing the cell first when it is not the focused one; the element answers with
     * {@code consulo-grid-edit-start}, and with {@code consulo-grid-edit-cancel} when no field could open there.
     */
    public void requestEdit(int row, int column, String typed) {
        getElement().callJsFunction("requestEdit", row, column, typed);
    }

    /**
     * The tab separated text of the copy {@code id} the element asked for.
     */
    public void resolveCopy(int id, String tsv) {
        getElement().callJsFunction("resolveCopy", id, tsv);
    }

    @Override
    public consulo.ui.@Nullable Component toUIComponent() {
        return myOwner;
    }
}
