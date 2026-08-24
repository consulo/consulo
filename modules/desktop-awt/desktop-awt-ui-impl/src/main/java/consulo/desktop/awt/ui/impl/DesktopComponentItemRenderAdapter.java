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
package consulo.desktop.awt.ui.impl;

import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;

import javax.swing.*;
import java.awt.*;
import java.util.function.IntSupplier;

/**
 * Bridges a {@link ComponentItemRender} onto a swing list cell renderer.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public class DesktopComponentItemRenderAdapter<E> extends DesktopComponentItemRenderBase<E> implements ListCellRenderer<E> {
    private final IntSupplier myHoveredIndex;

    public DesktopComponentItemRenderAdapter(ComponentItemRender<E> render, IntSupplier hoveredIndex) {
        super(render);
        myHoveredIndex = hoveredIndex;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
        return render(
            RenderItem.of(value, isSelected, index == myHoveredIndex.getAsInt()),
            background(list, index, isSelected),
            foreground(list, isSelected)
        );
    }

    /**
     * A hovered row is drawn on a band of its own rather than the one the selection uses - a list which does not move
     * its selection with the pointer would otherwise show two rows which both look selected.
     */
    private Color background(JList<?> list, int index, boolean isSelected) {
        if (isSelected) {
            return list.getSelectionBackground();
        }

        return index >= 0 && index == myHoveredIndex.getAsInt()
            ? TargetAWT.to(ComponentColors.HOVER_BACKGROUND)
            : list.getBackground();
    }

    private static Color foreground(JList<?> list, boolean isSelected) {
        return isSelected ? list.getSelectionForeground() : list.getForeground();
    }
}
