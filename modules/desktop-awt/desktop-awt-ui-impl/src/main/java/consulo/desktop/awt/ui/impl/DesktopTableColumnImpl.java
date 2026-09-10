/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.ex.awt.AbstractTableCellEditor;
import consulo.ui.ex.awt.ColoredTableCellRenderer;
import consulo.ui.ex.awt.ColumnInfo;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

import java.util.Comparator;
import java.util.function.Function;

/**
 * The column is its own spec - it answers name, width and comparator from its own fields, so it is
 * usable before the swing table exists.
 *
 * @author VISTALL
 * @since 2020-09-15
 */
public class DesktopTableColumnImpl<Item, Value> extends ColumnInfo<Item, Value> implements TableColumn<Item, Value> {
    private final DesktopTableImpl<Item> myTable;
    private final Function<Item, Value> myValueProvider;

    private TableItemRender<Item, Value> myRender = TableItemRender.of(TextItemRender.defaultRender());

    /**
     * A table asks for a renderer per row, so the bridge is held here instead of being built per call - a
     * reusable render can only keep its component while the bridge outlives a single paint.
     */
    private @Nullable DesktopComponentItemTableCellRenderer<Value> myComponentRenderer;

    private @Nullable Comparator<Value> myComparator;
    private @Nullable TableItemEditor<Item, Value> myEditor;

    private int myWidth = -1;
    private boolean myResizable = true;
    private HorizontalAlignment myAlignment = HorizontalAlignment.LEFT;

    public DesktopTableColumnImpl(DesktopTableImpl<Item> table, LocalizeValue header, Function<Item, Value> valueProvider) {
        super(header);
        myTable = table;
        myValueProvider = valueProvider;
    }

    @Override
    public @Nullable Value valueOf(Item item) {
        return myValueProvider.apply(item);
    }

    @Override
    public TableColumn<Item, Value> setHeader(LocalizeValue header) {
        setName(header);
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TextItemRender<Value> render) {
        myRender = TableItemRender.of(render);
        myComponentRenderer = null;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TableItemRender<Item, Value> render) {
        myRender = render;
        myComponentRenderer = null;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(ComponentItemRender<Value> render) {
        myComponentRenderer = new DesktopComponentItemTableCellRenderer<>(render);
        return this;
    }

    @Override
    public TableColumn<Item, Value> setWidth(int pixels) {
        myWidth = pixels;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setResizable(boolean resizable) {
        myResizable = resizable;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setHorizontalAlignment(HorizontalAlignment alignment) {
        myAlignment = alignment;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setSortable(@Nullable Comparator<Value> comparator) {
        myComparator = comparator;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setEditor(@Nullable TableItemEditor<Item, Value> editor) {
        myEditor = editor;
        return this;
    }

    public boolean isResizable() {
        return myResizable;
    }

    @Override
    public int getWidth(JTable table) {
        return myWidth;
    }

    @Override
    public @Nullable Comparator<Item> getComparator() {
        Comparator<Value> comparator = myComparator;
        if (comparator == null) {
            return null;
        }
        return Comparator.comparing(this::valueOf, Comparator.nullsFirst(comparator));
    }

    @Override
    public @Nullable TableCellRenderer getRenderer(Item item) {
        if (myComponentRenderer != null) {
            return myComponentRenderer;
        }

        return new ColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
                setTextAlign(toSwingAlignment(myAlignment));

                // the band belongs to the row, so it is painted under every column - a selected row keeps the
                // selection fill, which is the one the user is actually looking for
                if (!selected) {
                    ColorValue rowBackground = myTable.getRowBackground(item);
                    if (rowBackground != null) {
                        setBackground(TargetAWT.to(rowBackground));
                    }
                }

                myRender.render(new DesktopTextItemPresentationImpl(this), RenderItem.of(valueOf(item), selected), item);
            }
        };
    }

    @Override
    public boolean isCellEditable(Item item) {
        return myEditor != null && myEditor.isEditable(item);
    }

    @Override
    public @Nullable TableCellEditor getEditor(Item item) {
        TableItemEditor<Item, Value> editor = myEditor;
        if (editor == null) {
            return null;
        }

        return new AbstractTableCellEditor() {
            private ValueComponent<Value> myValueComponent;

            @Override
            public java.awt.Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                myValueComponent = editor.createComponent(item);
                myValueComponent.setValue(valueOf(item), false);

                java.awt.Component awtComponent = TargetAWT.to(myValueComponent);

                // the editor sits in the row it edits, so it is painted like one - a widget left on its own
                // look and feel fill would otherwise flash a box of a different color the moment editing starts
                DesktopComponentItemRenderBase.applyRowColors(
                    awtComponent,
                    isSelected ? table.getSelectionBackground() : table.getBackground(),
                    isSelected ? table.getSelectionForeground() : table.getForeground()
                );

                // a button carries its whole value in the click, so there is nothing further to type and
                // waiting for focus to leave would only lose the change
                if (awtComponent instanceof AbstractButton) {
                    myValueComponent.addValueListener(event -> stopCellEditing());
                }

                return awtComponent;
            }

            @Override
            public Object getCellEditorValue() {
                return myValueComponent == null ? null : myValueComponent.getValue();
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(Item item, Value value) {
        if (myEditor != null) {
            myEditor.commit(item, value);
        }
    }

    private static int toSwingAlignment(HorizontalAlignment alignment) {
        return switch (alignment) {
            case LEFT -> SwingConstants.LEFT;
            case CENTER -> SwingConstants.CENTER;
            case RIGHT -> SwingConstants.RIGHT;
        };
    }
}
