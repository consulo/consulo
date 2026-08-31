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

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ContextMenuEvent;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

/**
 * A swing cell renderer is a rubber stamp - the row is painted but never added to the hierarchy, so components
 * inside it never receive a mouse event of their own. Finding what the pointer is over therefore means stamping the
 * row again, laying it out the way the paint pass would, and hit testing that throwaway copy.
 *
 * @author VISTALL
 * @since 2026-08-23
 */
public final class DesktopListRenderInteraction {
    /**
     * A component inside a row which carries a click listener.
     *
     * @param component the component to send the click to
     * @param rowRoot   whether it is the row itself rather than something nested in it. A click on the row means the
     *                  row was activated, which the list still has to select and scroll to; a click on something
     *                  nested takes the event away from the list.
     */
    public record ClickTarget(Component component, boolean rowRoot) {
    }

    private DesktopListRenderInteraction() {
    }

    public static <E> @Nullable ClickTarget findTarget(
        JList<E> list,
        ListCellRenderer<E> renderer,
        Point point,
        Class<? extends ComponentEvent<?>> eventClass
    ) {
        int index = list.locationToIndex(point);
        if (index < 0) {
            return null;
        }

        Rectangle cell = list.getCellBounds(index, index);
        if (cell == null || !cell.contains(point)) {
            return null;
        }

        java.awt.Component row = stampRow(list, renderer, index, cell);
        if (row == null) {
            return null;
        }

        java.awt.Component hit = SwingUtilities.getDeepestComponentAt(row, point.x - cell.x, point.y - cell.y);

        for (java.awt.Component each = hit; each != null; each = each.getParent()) {
            Component uiComponent = each instanceof FromSwingComponentWrapper wrapper ? wrapper.toUIComponent() : null;

            if (uiComponent instanceof SwingComponentDelegate<?> delegate && delegate.hasListeners(eventClass)) {
                return new ClickTarget(uiComponent, each == row);
            }

            if (each == row) {
                break;
            }
        }

        return null;
    }

    public static void click(ClickTarget target, JList<?> list, MouseEvent event) {
        target.component().getListenerDispatcher(ClickEvent.class)
            .onEvent(new ClickEvent(anchorOf(list, target), DesktopAWTInputDetails.convert(list, event)));
    }

    public static void contextMenu(ClickTarget target, JList<?> list, MouseEvent event) {
        target.component().getListenerDispatcher(ContextMenuEvent.class)
            .onEvent(new ContextMenuEvent(anchorOf(list, target), DesktopAWTInputDetails.convert(list, event)));
    }

    /**
     * The listener belongs to the row, but the row is a stamp - it has no place on screen for anything to be put
     * next to. What the event carries is therefore the list, which the pointer position is measured against too, so
     * a menu asked for by the row opens where the pointer is.
     */
    private static Component anchorOf(JList<?> list, ClickTarget target) {
        return list instanceof FromSwingComponentWrapper wrapper ? wrapper.toUIComponent() : target.component();
    }

    private static <E> java.awt.@Nullable Component stampRow(JList<E> list, ListCellRenderer<E> renderer, int index, Rectangle cell) {
        java.awt.Component row = renderer.getListCellRendererComponent(
            list,
            list.getModel().getElementAt(index),
            index,
            list.isSelectedIndex(index),
            list.hasFocus()
        );
        if (row == null) {
            return null;
        }

        row.setBounds(0, 0, cell.width, cell.height);
        layoutRecursively(row);
        return row;
    }

    private static void layoutRecursively(java.awt.Component component) {
        component.doLayout();

        if (component instanceof Container container) {
            for (java.awt.Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }
}
