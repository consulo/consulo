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

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.ComponentItemRender;
import consulo.ui.RenderItem;
import consulo.ui.ReusableComponentItemRender;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.ComponentColors;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import java.awt.*;
import java.util.function.IntSupplier;

/**
 * Bridges a {@link ComponentItemRender} onto a swing cell renderer.
 * <p/>
 * A swing renderer is a rubber stamp, re-invoked on every paint, so a
 * {@link ReusableComponentItemRender} builds its component once and only rebinds per row.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public class DesktopComponentItemRenderAdapter<E> implements ListCellRenderer<E> {
    private final ComponentItemRender<E> myRender;
    private final IntSupplier myHoveredIndex;

    @SuppressWarnings("rawtypes")
    private consulo.ui.Component myReusedComponent;

    private boolean myMouseEventsAllowed;

    public DesktopComponentItemRenderAdapter(ComponentItemRender<E> render, IntSupplier hoveredIndex) {
        myRender = render;
        myHoveredIndex = hoveredIndex;
    }

    public boolean isMouseEventsAllowed() {
        return myMouseEventsAllowed;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected, boolean cellHasFocus) {
        RenderItem<E> item = RenderItem.of(value, isSelected, index == myHoveredIndex.getAsInt());

        Component component;
        try {
            component = render(item);
        }
        finally {
            myMouseEventsAllowed |= item.isMouseEventsAllowed();
        }

        if (component != null) {
            applySelection(component, background(list, index, isSelected), foreground(list, isSelected), true);
        }

        return component;
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

    /**
     * A row is painted by the list, not held by it, so the renderer is what draws the selection - the row is opaque
     * and carries the list colors, and the layouts inside it stay transparent so that background shows through.
     */
    private static void applySelection(Component component, Color background, Color foreground, boolean root) {
        if (root) {
            component.setBackground(background);

            if (component instanceof JComponent jComponent) {
                jComponent.setOpaque(true);
            }
        }
        else if (component instanceof JPanel panel && panel instanceof FromSwingComponentWrapper) {
            panel.setOpaque(false);
        }

        Color currentForeground = component.getForeground();
        if (currentForeground == null || currentForeground instanceof UIResource) {
            component.setForeground(foreground);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applySelection(child, background, foreground, false);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Component render(RenderItem<E> item) {
        if (myRender instanceof ReusableComponentItemRender reusable) {
            if (myReusedComponent == null) {
                myReusedComponent = (consulo.ui.Component) reusable.createComponent();
            }
            reusable.bind(myReusedComponent, item);
            return TargetAWT.to(myReusedComponent);
        }

        return TargetAWT.to(myRender.render(item));
    }
}
