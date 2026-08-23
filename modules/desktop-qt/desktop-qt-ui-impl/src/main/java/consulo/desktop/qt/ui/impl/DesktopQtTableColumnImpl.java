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
package consulo.desktop.qt.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.ComponentItemRender;
import consulo.ui.HorizontalAlignment;
import consulo.ui.TableColumn;
import consulo.ui.TableItemEditor;
import consulo.ui.TextItemRender;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-08-23
 */
public class DesktopQtTableColumnImpl<Item, Value> implements TableColumn<Item, Value> {
    private final DesktopQtTableImpl<Item> myTable;
    private final Function<Item, Value> myValueProvider;
    private final int myIndex;

    private LocalizeValue myHeader;
    private TextItemRender<Value> myTextRender = TextItemRender.defaultRender();
    private @Nullable ComponentItemRender<Value> myComponentRender;
    private @Nullable Comparator<Value> myComparator;
    private @Nullable TableItemEditor<Item, Value> myEditor;

    private int myWidth = -1;
    private boolean myResizable = true;
    private HorizontalAlignment myAlignment = HorizontalAlignment.LEFT;

    public DesktopQtTableColumnImpl(DesktopQtTableImpl<Item> table, int index, LocalizeValue header, Function<Item, Value> valueProvider) {
        myTable = table;
        myIndex = index;
        myHeader = header;
        myValueProvider = valueProvider;
    }

    public int getIndex() {
        return myIndex;
    }

    public LocalizeValue getHeader() {
        return myHeader;
    }

    public Function<Item, Value> getValueProvider() {
        return myValueProvider;
    }

    public TextItemRender<Value> getTextRender() {
        return myTextRender;
    }

    public @Nullable ComponentItemRender<Value> getComponentRender() {
        return myComponentRender;
    }

    public @Nullable Comparator<Value> getComparator() {
        return myComparator;
    }

    public @Nullable TableItemEditor<Item, Value> getEditor() {
        return myEditor;
    }

    public int getWidth() {
        return myWidth;
    }

    public boolean isResizable() {
        return myResizable;
    }

    public HorizontalAlignment getAlignment() {
        return myAlignment;
    }

    @Override
    public TableColumn<Item, Value> setHeader(LocalizeValue header) {
        myHeader = header;
        myTable.updateHeaders();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TextItemRender<Value> render) {
        myTextRender = render;
        myComponentRender = null;
        myTable.updateRows();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(ComponentItemRender<Value> render) {
        myComponentRender = render;
        myTable.updateRows();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setWidth(int pixels) {
        myWidth = pixels;
        myTable.updateColumnLayout();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setResizable(boolean resizable) {
        myResizable = resizable;
        myTable.updateColumnLayout();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setHorizontalAlignment(HorizontalAlignment alignment) {
        myAlignment = alignment;
        myTable.updateRows();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setSortable(@Nullable Comparator<Value> comparator) {
        myComparator = comparator;
        myTable.updateSortable();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setEditor(@Nullable TableItemEditor<Item, Value> editor) {
        myEditor = editor;
        myTable.updateRows();
        return this;
    }
}
