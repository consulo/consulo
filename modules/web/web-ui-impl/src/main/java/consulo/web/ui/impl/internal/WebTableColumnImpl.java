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

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-02
 */
public class WebTableColumnImpl<Item, Value> implements TableColumn<Item, Value> {
    private final WebTableImpl<Item> myTable;
    private final Function<Item, Value> myValueProvider;
    private final Grid.Column<Item> myColumn;

    private TextItemRender<Value> myTextRender = TextItemRender.defaultRender();
    private @Nullable ComponentItemRender<Value> myComponentRender;

    public WebTableColumnImpl(WebTableImpl<Item> table, Function<Item, Value> valueProvider) {
        myTable = table;
        myValueProvider = valueProvider;

        myColumn = table.toVaadinComponent().addColumn(new ComponentRenderer<>(this::renderCell));
    }

    private com.vaadin.flow.component.Component renderCell(Item item) {
        Value value = myValueProvider.apply(item);
        RenderItem<Value> renderItem = RenderItem.of(value, myTable.isSelected(item));

        com.vaadin.flow.component.Component component;
        if (myComponentRender != null) {
            component = ((ToVaadinComponentWrapper) myComponentRender.render(renderItem)).toVaadinComponent();
        }
        else {
            WebItemPresentationImpl presentation = new WebItemPresentationImpl();
            myTextRender.render(presentation, renderItem);
            component = presentation.toComponent();
        }

        myTable.applyItemHeight(component, item);
        return component;
    }

    @Override
    public TableColumn<Item, Value> setHeader(LocalizeValue header) {
        myColumn.setHeader(header.get());
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TextItemRender<Value> render) {
        myTextRender = render;
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
        myColumn.setWidth(pixels + "px").setFlexGrow(0);
        return this;
    }

    @Override
    public TableColumn<Item, Value> setResizable(boolean resizable) {
        myColumn.setResizable(resizable);
        return this;
    }

    @Override
    public TableColumn<Item, Value> setHorizontalAlignment(HorizontalAlignment alignment) {
        myColumn.setTextAlign(switch (alignment) {
            case LEFT -> ColumnTextAlign.START;
            case CENTER -> ColumnTextAlign.CENTER;
            case RIGHT -> ColumnTextAlign.END;
        });
        return this;
    }

    @Override
    public TableColumn<Item, Value> setSortable(@Nullable Comparator<Value> comparator) {
        if (comparator == null) {
            myColumn.setSortable(false);
            return this;
        }

        myColumn.setSortable(true);
        myColumn.setComparator((a, b) ->
            Comparator.nullsFirst(comparator).compare(myValueProvider.apply(a), myValueProvider.apply(b)));
        return this;
    }

    @Override
    public TableColumn<Item, Value> setEditor(@Nullable TableItemEditor<Item, Value> editor) {
        if (editor == null) {
            myColumn.setEditorComponent((com.vaadin.flow.component.Component) null);
            return this;
        }

        myColumn.setEditorComponent(item -> {
            ValueComponent<Value> component = editor.createComponent(item);
            component.setValue(myValueProvider.apply(item), false);
            component.addValueListener(event -> editor.commit(item, event.getValue()));
            return ((ToVaadinComponentWrapper) component).toVaadinComponent();
        });
        return this;
    }
}
