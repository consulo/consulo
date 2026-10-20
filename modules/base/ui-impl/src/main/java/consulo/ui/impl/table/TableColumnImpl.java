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

import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.font.Font;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class TableColumnImpl<Item, Value> implements TableColumn<Item, Value> {
    private final TableColumnOwner myOwner;
    private final Function<Item, Value> myValueProvider;
    private final int myIndex;

    private volatile LocalizeValue myHeader;
    private volatile TableItemRenderer<Item, Value> myRenderer = TableItemRenderer.of(TextItemRenderer.defaultRenderer());
    private volatile @Nullable ComponentItemRenderer<Value> myComponentRenderer;
    private volatile @Nullable Comparator<Value> myComparator;
    private volatile @Nullable TableItemEditor<Item, Value> myEditor;

    private volatile int myWidth = -1;
    private volatile boolean myResizable = true;
    private volatile HorizontalAlignment myAlignment = HorizontalAlignment.LEFT;

    private volatile @Nullable UnaryOperator<Font> myFontBuilder;
    private @Nullable Font myBaseFont;
    private @Nullable Font myBuiltFont;

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

    public TableItemRenderer<Item, Value> getRenderer() {
        return myRenderer;
    }

    public TableItemRenderer<Item, Value> getTextRenderer() {
        return myComponentRenderer == null ? myRenderer : TableItemRenderer.of(TextItemRenderer.defaultRenderer());
    }

    public @Nullable ComponentItemRenderer<Value> getComponentRenderer() {
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

    /**
     * The font of the cells of this column, built from {@code baseFont} - the font the table draws with. Built
     * once and kept until the table font changes.
     *
     * @return {@code null} when the column has no font builder
     */
    public @Nullable Font getFont(Font baseFont) {
        UnaryOperator<Font> builder = myFontBuilder;
        if (builder == null) {
            return null;
        }
        if (!baseFont.equals(myBaseFont)) {
            myBuiltFont = builder.apply(baseFont);
            myBaseFont = baseFont;
        }
        return myBuiltFont;
    }

    public boolean hasFontBuilder() {
        return myFontBuilder != null;
    }

    @Override
    public TableColumn<Item, Value> setHeader(LocalizeValue header) {
        myHeader = header;
        myOwner.headerChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRenderer(TableItemRenderer<Item, Value> renderer) {
        myRenderer = renderer;
        myComponentRenderer = null;
        myOwner.rendererChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRenderer(TextItemRenderer<Value> renderer) {
        myRenderer = TableItemRenderer.of(renderer);
        myComponentRenderer = null;
        myOwner.rendererChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setRenderer(ComponentItemRenderer<Value> renderer) {
        myComponentRenderer = renderer;
        myOwner.rendererChanged();
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
        myOwner.rendererChanged();
        return this;
    }

    @Override
    public TableColumn<Item, Value> setFontBuilder(@Nullable UnaryOperator<Font> builder) {
        myFontBuilder = builder;
        myBaseFont = null;
        myBuiltFont = null;
        myOwner.rendererChanged();
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
        myOwner.rendererChanged();
        return this;
    }
}
