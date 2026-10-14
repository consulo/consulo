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
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.color.ColorValue;
import consulo.web.ui.impl.internal.base.ToVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public abstract class WebGridColumnBase<Row, Item, Value> {
    private final Grid.Column<Row> myColumn;

    protected WebGridColumnBase(Grid<Row> grid) {
        myColumn = grid.addColumn(new ComponentRenderer<>(this::renderCell, this::updateCell));
    }

    public Grid.Column<Row> getGridColumn() {
        return myColumn;
    }

    protected abstract @Nullable Item itemOf(Row row);

    protected abstract @Nullable Value valueOf(Row row, Item item);

    protected abstract boolean isSelected(Row row);

    protected abstract TableItemRenderer<Item, Value> getRenderer();

    protected abstract @Nullable ComponentItemRenderer<Value> getComponentRenderer();

    protected abstract @Nullable TableItemEditor<Item, Value> getEditor();

    protected abstract void decorateCell(Component cell, Row row, @Nullable Item item, @Nullable ColorValue background);

    protected abstract Component wrapCell(Component content);

    protected @Nullable ColorValue rowBackground(Row row, Item item) {
        return null;
    }

    protected void afterEdit(Row row, Item item) {
    }

    private Component renderCell(Row row) {
        Item item = itemOf(row);
        if (item == null) {
            return decorated(wrapCell(new Span()), row, null, null);
        }

        TableItemEditor<Item, Value> editor = getEditor();
        if (editor != null && editor.isEditable(item)) {
            WebGridCellEditor<Item, Value> cellEditor =
                new WebGridCellEditor<>(editor, item, valueOf(row, item), () -> afterEdit(row, item));

            Component cell = wrapCell(cellEditor.getVaadinComponent());
            ComponentUtil.setData(cell, WebGridCellEditor.class, cellEditor);
            return decorated(cell, row, item, unselectedRowBackground(row, item));
        }

        RenderItem<Value> renderItem = RenderItem.of(valueOf(row, item), isSelected(row));

        ComponentItemRenderer<Value> componentRenderer = getComponentRenderer();
        if (componentRenderer != null) {
            Component content = ((ToVaadinComponentWrapper) componentRenderer.render(renderItem)).toVaadinComponent();
            return decorated(wrapCell(content), row, item, unselectedRowBackground(row, item));
        }

        WebItemPresentationImpl presentation = new WebItemPresentationImpl();
        presentation.withBackgroundColor(unselectedRowBackground(row, item));
        getRenderer().render(presentation, renderItem, item);
        return decorated(wrapCell(presentation.toComponent()), row, item, presentation.getBackgroundColor());
    }

    @SuppressWarnings("unchecked")
    private Component updateCell(Component cell, Row row) {
        Item item = itemOf(row);
        WebGridCellEditor<Item, Value> cellEditor = ComponentUtil.getData(cell, WebGridCellEditor.class);
        if (item == null || cellEditor == null || !cellEditor.isEditing(getEditor(), item)) {
            return renderCell(row);
        }

        cellEditor.showValue(valueOf(row, item));
        return decorated(cell, row, item, unselectedRowBackground(row, item));
    }

    private @Nullable ColorValue unselectedRowBackground(Row row, Item item) {
        return isSelected(row) ? null : rowBackground(row, item);
    }

    private Component decorated(Component cell, Row row, @Nullable Item item, @Nullable ColorValue background) {
        decorateCell(cell, row, item, background);
        return cell;
    }

    protected static Component wrapCell(Component content, String className) {
        Div cell = new Div(content);
        cell.addClassName(className);
        return cell;
    }

    protected static String justifyContent(HorizontalAlignment alignment) {
        return switch (alignment) {
            case LEFT -> "flex-start";
            case CENTER -> "center";
            case RIGHT -> "flex-end";
        };
    }

    protected void applyHeader(LocalizeValue header) {
        myColumn.setHeader(header.get());
    }

    protected void applyWidth(int pixels) {
        myColumn.setWidth(pixels + "px").setFlexGrow(0);
    }

    protected void applyResizable(boolean resizable) {
        myColumn.setResizable(resizable);
    }

    protected void applyAlignment(HorizontalAlignment alignment) {
        myColumn.setTextAlign(switch (alignment) {
            case LEFT -> ColumnTextAlign.START;
            case CENTER -> ColumnTextAlign.CENTER;
            case RIGHT -> ColumnTextAlign.END;
        });
    }
}
