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
    private final WebTableImpl<Item> myTable;
    private final Function<Item, Value> myValueProvider;

    private TableItemRender<Item, Value> myRender = TableItemRender.of(TextItemRender.defaultRender());
    private @Nullable ComponentItemRender<Value> myComponentRender;
    private @Nullable TableItemEditor<Item, Value> myEditor;

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
        return myRender;
    }

    @Override
    protected @Nullable ComponentItemRender<Value> getComponentRender() {
        return myComponentRender;
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
    protected void decorateCell(Component cell, Item row, @Nullable Item item) {
        myTable.applyItemHeight(cell, item);
    }

    @Override
    public TableColumn<Item, Value> setHeader(LocalizeValue header) {
        applyHeader(header);
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TableItemRender<Item, Value> render) {
        myRender = render;
        myComponentRender = null;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TextItemRender<Value> render) {
        myRender = TableItemRender.of(render);
        myComponentRender = null;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(ComponentItemRender<Value> render) {
        myComponentRender = render;
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
        applyAlignment(alignment);
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
