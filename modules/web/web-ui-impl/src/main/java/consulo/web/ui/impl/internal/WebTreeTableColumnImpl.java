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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Style;
import consulo.localize.LocalizeValue;
import consulo.ui.ComponentItemRender;
import consulo.ui.HorizontalAlignment;
import consulo.ui.TableItemEditor;
import consulo.ui.TableItemRender;
import consulo.ui.color.ColorValue;
import consulo.ui.impl.table.TableColumnImpl;
import consulo.ui.impl.table.TableColumnOwner;
import consulo.ui.impl.tree.TreeNodeImpl;
import consulo.ui.impl.tree.TreeTableColumns;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class WebTreeTableColumnImpl<E, Value> extends WebGridColumnBase<WebTreeRow<E>, E, Value> implements TableColumnOwner {
    private static final String CELL_CLASS = "web-tree-cell";
    private static final String ROW_BACKGROUND = "--consulo-tree-row-background";

    private final WebTreeTableImpl<E> myTable;
    private final TableColumnImpl<E, Value> myState;

    public WebTreeTableColumnImpl(WebTreeTableImpl<E> table,
                                  TreeTableColumns<E> columns,
                                  LocalizeValue header,
                                  Function<E, Value> valueProvider) {
        super(table.toVaadinComponent());
        myTable = table;
        myState = columns.add(this, header, valueProvider);
    }

    public TableColumnImpl<E, Value> getState() {
        return myState;
    }

    public int getIndex() {
        return myState.getIndex();
    }

    void applyState() {
        applyHeader(myState.getHeader());
        applyAlignment(myState.getAlignment());
        applyLayout();
        applySortable();
    }

    void applySortable() {
        getGridColumn().setSortable(myState.isSortable());
    }

    private void applyLayout() {
        Grid.Column<WebTreeRow<E>> column = getGridColumn();
        int width = myState.getWidth();
        if (width >= 0) {
            column.setAutoWidth(false);
            applyWidth(width);
        }
        else {
            column.setAutoWidth(true).setFlexGrow(0);
        }
        applyResizable(myState.isResizable());
    }

    @Override
    public void headerChanged() {
        applyHeader(myState.getHeader());
    }

    @Override
    public void renderChanged() {
        applyAlignment(myState.getAlignment());
        myTable.refreshRows();
    }

    @Override
    public void layoutChanged() {
        applyLayout();
    }

    @Override
    public void sortChanged() {
        myTable.columnSortChanged(this);
    }

    @Override
    protected @Nullable E itemOf(WebTreeRow<E> row) {
        TreeNodeImpl<E> node = row.getNode();
        return node == null ? null : node.getValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected @Nullable Value valueOf(WebTreeRow<E> row, E item) {
        TreeNodeImpl<E> node = row.getNode();
        return node == null ? null : (Value) node.getColumnValue(myState.getIndex());
    }

    @Override
    protected boolean isSelected(WebTreeRow<E> row) {
        return myTable.isSelected(row);
    }

    @Override
    protected TableItemRender<E, Value> getRender() {
        return myState.getRender();
    }

    @Override
    protected @Nullable ComponentItemRender<Value> getComponentRender() {
        return myState.getComponentRender();
    }

    @Override
    protected @Nullable TableItemEditor<E, Value> getEditor() {
        return myState.getEditor();
    }

    @Override
    protected void afterEdit(WebTreeRow<E> row, E item) {
        TreeNodeImpl<E> node = row.getNode();
        if (node != null) {
            myTable.refreshItem(node, false);
        }
    }

    @Override
    protected Component wrapCell(Component content) {
        Div cell = new Div(content);
        cell.addClassName(CELL_CLASS);
        return cell;
    }

    @Override
    protected void decorateCell(Component cell, WebTreeRow<E> row, @Nullable E item) {
        Style style = cell.getElement().getStyle();
        style.set("justify-content", justifyContent(myState.getAlignment()));

        ColorValue background = myTable.rowBackground(row.getNode());
        if (background != null) {
            style.set(ROW_BACKGROUND, WebColors.toCssColor(background));
        }
        else {
            style.remove(ROW_BACKGROUND);
        }
    }

    private static String justifyContent(HorizontalAlignment alignment) {
        return switch (alignment) {
            case LEFT -> "flex-start";
            case CENTER -> "center";
            case RIGHT -> "flex-end";
        };
    }
}
