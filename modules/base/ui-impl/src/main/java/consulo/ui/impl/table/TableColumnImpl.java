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
package consulo.ui.impl.table;

import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class TableColumnImpl<Item, Value> implements TableColumn<Item, Value> {
    private final TableColumnOwner myOwner;
    private final Function<Item, Value> myValueProvider;
    private final int myIndex;

    private volatile LocalizeValue myHeader;
    private volatile TableItemRender<Item, Value> myRenderer = TableItemRender.of(TextItemRender.defaultRenderer());
    private volatile @Nullable ComponentItemRender<Value> myComponentRenderer;
    private volatile @Nullable Comparator<Value> myComparator;
    private volatile @Nullable TableItemEditor<Item, Value> myEditor;

    private volatile int myWidth = -1;
    private volatile boolean myResizable = true;
    private volatile HorizontalAlignment myAlignment = HorizontalAlignment.LEFT;

    public TableColumnImpl(TableColumnOwner owner, int index, LocalizeValue header, Function<Item, Value> valueProvider) {
        myOwner = owner;
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

    public Value getValue(Item item) {
        return myValueProvider.apply(item);
    }

    @SuppressWarnings("deprecation")
    public TableItemRender<Item, Value> getRenderer() {
        return getRender();
    }

    @Deprecated
    @DeprecationInfo("Use #getRenderer")
    public TableItemRender<Item, Value> getRender() {
        return myRenderer;
    }

    @SuppressWarnings("deprecation")
    public TableItemRender<Item, Value> getTextRenderer() {
        return getTextRender();
    }

    @Deprecated
    @DeprecationInfo("Use #getTextRenderer")
    public TableItemRender<Item, Value> getTextRender() {
        return myComponentRenderer == null ? myRenderer : TableItemRender.of(TextItemRender.defaultRenderer());
    }

    @SuppressWarnings("deprecation")
    public @Nullable ComponentItemRender<Value> getComponentRenderer() {
        return getComponentRender();
    }

    @Deprecated
    @DeprecationInfo("Use #getComponentRenderer")
    public @Nullable ComponentItemRender<Value> getComponentRender() {
        return myComponentRenderer;
    }

    public @Nullable Comparator<Value> getComparator() {
        return myComparator;
    }

    public boolean isSortable() {
        return myComparator != null;
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
        myOwner.headerChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TableItemRender<Item, Value> renderer) {
        myRenderer = renderer;
        myComponentRenderer = null;
        myOwner.renderChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(TextItemRender<Value> renderer) {
        myRenderer = TableItemRender.of(renderer);
        myComponentRenderer = null;
        myOwner.renderChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRender(ComponentItemRender<Value> renderer) {
        myComponentRenderer = renderer;
        myOwner.renderChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setWidth(int pixels) {
        myWidth = pixels;
        myOwner.layoutChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setResizable(boolean resizable) {
        myResizable = resizable;
        myOwner.layoutChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setHorizontalAlignment(HorizontalAlignment alignment) {
        myAlignment = alignment;
        myOwner.renderChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setSortable(@Nullable Comparator<Value> comparator) {
        myComparator = comparator;
        myOwner.sortChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setEditor(@Nullable TableItemEditor<Item, Value> editor) {
        myEditor = editor;
        myOwner.renderChanged();
        return this;
    }
}
