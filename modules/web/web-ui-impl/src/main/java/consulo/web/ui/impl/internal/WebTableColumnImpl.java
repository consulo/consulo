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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import consulo.localize.LocalizeValue;
import consulo.ui.ComponentItemRender;
import consulo.ui.HorizontalAlignment;
import consulo.ui.TableColumn;
import consulo.ui.TableItemEditor;
import consulo.ui.TableItemRender;
import consulo.ui.TextItemRender;
import consulo.ui.color.ColorValue;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebTableColumnImpl<Item, Value> extends WebGridColumnBase<Item, Item, Value> implements TableColumn<Item, Value> {
    private static final String CELL_CLASS = "web-table-cell";
    private static final String FILLED_CELL_CLASS = "web-table-cell-filled";
    private static final String CELL_BACKGROUND = "--consulo-table-cell-background";

    private final WebTableImpl<Item> myTable;
    private final Function<Item, Value> myValueProvider;

    private TableItemRender<Item, Value> myRenderer = TableItemRender.of(TextItemRender.defaultRenderer());
    private @Nullable ComponentItemRender<Value> myComponentRenderer;
    private @Nullable TableItemEditor<Item, Value> myEditor;
    private HorizontalAlignment myAlignment = HorizontalAlignment.LEFT;

    public WebTableColumnImpl(WebTableImpl<Item> table, Function<Item, Value> valueProvider) {
        super(table.toVaadinComponent());
        myTable = table;
        myValueProvider = valueProvider;
    }

    @Override
    protected Item itemOf(Item row) {
        return row;
    }

    @Override
    protected Value valueOf(Item row, Item item) {
        return myValueProvider.apply(item);
    }

    @Override
    protected boolean isSelected(Item row) {
        return myTable.isSelected(row);
    }

    @Override
    protected TableItemRender<Item, Value> getRender() {
        return myRenderer;
    }

    @Override
    protected @Nullable ComponentItemRender<Value> getComponentRender() {
        return myComponentRenderer;
    }

    @Override
    protected @Nullable TableItemEditor<Item, Value> getEditor() {
        return myEditor;
    }

    @Override
    protected @Nullable ColorValue rowBackground(Item row, Item item) {
        return myTable.getRowBackground(item);
    }

    @Override
    protected Component wrapCell(Component content) {
        return wrapCell(content, CELL_CLASS);
    }

    @Override
    protected void decorateCell(Component cell, Item row, @Nullable Item item, @Nullable ColorValue background) {
        myTable.applyItemHeight(cell, item);

        Element element = cell.getElement();
        Style style = element.getStyle();
        style.set("justify-content", justifyContent(myAlignment));

        element.getClassList().set(FILLED_CELL_CLASS, background != null);
        if (background != null) {
            style.set(CELL_BACKGROUND, WebColors.toCssColor(background));
        }
        else {
            style.remove(CELL_BACKGROUND);
        }
    }

    @Override
    public TableColumn<Item, Value> setHeader(LocalizeValue header) {
        applyHeader(header);
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TableItemRender<Item, Value> renderer) {
        myRenderer = renderer;
        myComponentRenderer = null;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TextItemRender<Value> renderer) {
        myRenderer = TableItemRender.of(renderer);
        myComponentRenderer = null;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(ComponentItemRender<Value> renderer) {
        myComponentRenderer = renderer;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setWidth(int pixels) {
        applyWidth(pixels);
        return this;
    }

    @Override
    public TableColumn<Item, Value> setResizable(boolean resizable) {
        applyResizable(resizable);
        return this;
    }

    @Override
    public TableColumn<Item, Value> setHorizontalAlignment(HorizontalAlignment alignment) {
        myAlignment = alignment;
        applyAlignment(alignment);
        myTable.toVaadinComponent().getDataProvider().refreshAll();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setSortable(@Nullable Comparator<Value> comparator) {
        Grid.Column<Item> column = getGridColumn();
        if (comparator == null) {
            column.setSortable(false);
            return this;
        }

        column.setSortable(true);
        column.setComparator((a, b) ->
            Comparator.nullsFirst(comparator).compare(myValueProvider.apply(a), myValueProvider.apply(b)));
        return this;
    }

    @Override
    public TableColumn<Item, Value> setEditor(@Nullable TableItemEditor<Item, Value> editor) {
        myEditor = editor;
        myTable.toVaadinComponent().getDataProvider().refreshAll();
        return this;
    }
}
