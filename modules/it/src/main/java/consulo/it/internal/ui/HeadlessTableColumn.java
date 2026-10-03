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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.ComponentItemRender;
import consulo.ui.HorizontalAlignment;
import consulo.ui.TableColumn;
import consulo.ui.TableItemEditor;
import consulo.ui.TableItemRender;
import consulo.ui.TextItemRender;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author VISTALL
 */
public class HeadlessTableColumn<Item, Value> implements TableColumn<Item, Value> {
    private final Function<Item, Value> myValueProvider;

    private LocalizeValue myHeader;
    private @Nullable TableItemRender<Item, Value> myTableRender;
    private @Nullable TextItemRender<Value> myTextRender;
    private @Nullable ComponentItemRender<Value> myComponentRender;
    private @Nullable Comparator<Value> myComparator;
    private @Nullable TableItemEditor<Item, Value> myEditor;
    private int myWidth = -1;
    private boolean myResizable = true;
    private HorizontalAlignment myAlignment = HorizontalAlignment.LEFT;

    public HeadlessTableColumn(LocalizeValue header, Function<Item, Value> valueProvider) {
        myHeader = header;
        myValueProvider = valueProvider;
    }

    public Value getValue(Item item) {
        return myValueProvider.apply(item);
    }

    public LocalizeValue getHeader() {
        return myHeader;
    }

    public @Nullable TableItemRender<Item, Value> getTableRender() {
        return myTableRender;
    }

    public @Nullable TextItemRender<Value> getTextRender() {
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
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TextItemRender<Value> render) {
        myTextRender = render;
        myComponentRender = null;
        myTableRender = null;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(ComponentItemRender<Value> render) {
        myComponentRender = render;
        myTextRender = null;
        myTableRender = null;
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TableItemRender<Item, Value> render) {
        myTableRender = render;
        myTextRender = null;
        myComponentRender = null;
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
}
