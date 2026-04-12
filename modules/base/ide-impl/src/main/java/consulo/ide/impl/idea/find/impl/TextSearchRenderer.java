// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.ide.impl.idea.find.impl;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.usage.TextChunk;

import javax.swing.*;
import java.awt.*;

/**
 * Custom ListCellRenderer for rendering SearchEverywhereItem objects in the search results list.
 */
final class TextSearchRenderer extends JPanel implements ListCellRenderer<SearchEverywhereItem> {
    private static final SimpleTextAttributes ORDINAL_ATTRIBUTES =
        new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, new JBColor(0x999999, 0x999999));
    private static final SimpleTextAttributes REPEATED_FILE_ATTRIBUTES =
        new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, new JBColor(0xCCCCCC, 0x555555));

    private final ColoredListCellRenderer<SearchEverywhereItem> myUsageRenderer;
    private final ColoredListCellRenderer<SearchEverywhereItem> myFileRenderer;

    TextSearchRenderer() {
        myUsageRenderer = new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(
                JList<? extends SearchEverywhereItem> list,
                SearchEverywhereItem value,
                int index,
                boolean selected,
                boolean hasFocus
            ) {
                SearchEverywhereItem.UsagePresentation presentation = value.getPresentation();
                TextChunk[] text = presentation.getText();

                // skip line number (first chunk), render the rest
                for (int i = 1; i < text.length; i++) {
                    TextChunk chunk = text[i];
                    SimpleTextAttributes attributes = getChunkAttributes(chunk, selected);
                    append(chunk.getText(), attributes);
                }

                setIcon(PlatformIconGroup.nodesTextarea());
                //noinspection UseDPIAwareInsets
                setIpad(new Insets(0, 0, 0, getIpad().right));
                setBorder(null);
            }
        };

        myFileRenderer = new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(
                JList<? extends SearchEverywhereItem> list,
                SearchEverywhereItem value,
                int index,
                boolean selected,
                boolean hasFocus
            ) {
                SearchEverywhereItem.UsagePresentation presentation = value.getPresentation();
                TextChunk[] text = presentation.getText();

                String fileString = presentation.getFileString();
                String prevFileString = findPrevFile(list, index);
                SimpleTextAttributes attributes =
                    fileString.equals(prevFileString) ? REPEATED_FILE_ATTRIBUTES : ORDINAL_ATTRIBUTES;

                append(fileString, attributes);
                if (text.length > 0) {
                    append(" " + text[0].getText(), ORDINAL_ATTRIBUTES);
                }
                setBorder(null);
            }
        };

        setLayout(new BorderLayout());
        add(myUsageRenderer, BorderLayout.CENTER);
        add(myFileRenderer, BorderLayout.EAST);
        setBorder(JBUI.Borders.empty(2, 2, 2, 0));
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends SearchEverywhereItem> list,
        SearchEverywhereItem value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        myUsageRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        myFileRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        setBackground(myUsageRenderer.getBackground());
        if (!isSelected) {
            ColorValue colorValue = value.getPresentation().getBackgroundColor();
            if (colorValue != null) {
                Color color = TargetAWT.to(colorValue);
                setBackground(color);
                myUsageRenderer.setBackground(color);
                myFileRenderer.setBackground(color);
            }
        }
        return this;
    }

    private static String findPrevFile(JList<? extends SearchEverywhereItem> list, int index) {
        if (index <= 0) return null;
        Object prev = list.getModel().getElementAt(index - 1);
        if (prev instanceof SearchEverywhereItem item) {
            return item.getPresentation().getFileString();
        }
        return null;
    }

    private static SimpleTextAttributes getChunkAttributes(TextChunk textChunk, boolean selected) {
        SimpleTextAttributes attributes = textChunk.getSimpleAttributesIgnoreBackground();
        if (attributes.getFontStyle() != Font.BOLD) {
            return attributes;
        }
        return new SimpleTextAttributes(
            null, attributes.getFgColor(), attributes.getWaveColor(),
            attributes.getStyle() & ~SimpleTextAttributes.STYLE_BOLD
                | (selected ? SimpleTextAttributes.STYLE_SEARCH_MATCH : SimpleTextAttributes.STYLE_PLAIN)
        );
    }
}
