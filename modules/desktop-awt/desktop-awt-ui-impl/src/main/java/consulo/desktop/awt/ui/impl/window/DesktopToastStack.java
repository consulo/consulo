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
package consulo.desktop.awt.ui.impl.window;

import consulo.ui.Toast;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.internal.ToastInternal;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The toasts floating over one window: a column in the bottom right corner of its area - the whole layered pane when it
 * has none - the newest at the bottom. Each toast is as wide as {@link #WIDTH} and as tall as its content at that
 * width.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
final class DesktopToastStack {
    private static final int WIDTH = 360;
    private static final int MARGIN = 8;

    private final JRootPane myRootPane;
    private final List<Toast> myToasts = new ArrayList<>();

    private final ComponentListener myAreaListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            relayout();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            relayout();
        }
    };
    private final HierarchyBoundsListener myAreaHierarchyListener = new HierarchyBoundsAdapter() {
        @Override
        public void ancestorMoved(HierarchyEvent e) {
            relayout();
        }

        @Override
        public void ancestorResized(HierarchyEvent e) {
            relayout();
        }
    };

    private java.awt.@Nullable Component myArea;

    DesktopToastStack(JRootPane rootPane) {
        myRootPane = rootPane;
        rootPane.getLayeredPane().addComponentListener(myAreaListener);
    }

    void setArea(java.awt.@Nullable Component area) {
        if (myArea == area) {
            return;
        }
        if (myArea != null) {
            myArea.removeComponentListener(myAreaListener);
            myArea.removeHierarchyBoundsListener(myAreaHierarchyListener);
        }
        myArea = area;
        if (area != null) {
            area.addComponentListener(myAreaListener);
            area.addHierarchyBoundsListener(myAreaHierarchyListener);
        }
        relayout();
    }

    void show(Toast toast) {
        if (myToasts.contains(toast)) {
            return;
        }
        if (toast instanceof ToastInternal internal) {
            internal.setShadow(true);
        }
        myToasts.add(toast);
        myRootPane.getLayeredPane().add(TargetAWT.to(toast), JLayeredPane.POPUP_LAYER);
        relayout();
    }

    void hide(Toast toast) {
        if (!myToasts.remove(toast)) {
            return;
        }
        java.awt.Component component = TargetAWT.to(toast);
        JLayeredPane layeredPane = myRootPane.getLayeredPane();
        layeredPane.remove(component);
        layeredPane.repaint(component.getBounds());
        relayout();
    }

    private void relayout() {
        JLayeredPane layeredPane = myRootPane.getLayeredPane();
        Rectangle area = areaBounds(layeredPane);
        int width = JBUI.scale(WIDTH);
        int margin = JBUI.scale(MARGIN);
        int bottom = area.y + area.height - margin;
        int x = area.x + area.width - width - margin;

        for (int i = myToasts.size() - 1; i >= 0; i--) {
            java.awt.Component component = TargetAWT.to(myToasts.get(i));
            int height = preferredHeight(component, width);
            int y = bottom - height;
            component.setBounds(x, y, width, height);
            component.setVisible(y >= area.y);
            bottom = y;
        }
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    private Rectangle areaBounds(JLayeredPane layeredPane) {
        java.awt.Component area = myArea;
        if (area != null && area.isShowing() && area.getParent() != null && SwingUtilities.isDescendingFrom(area, layeredPane)) {
            return SwingUtilities.convertRectangle(area.getParent(), area.getBounds(), layeredPane);
        }
        return new Rectangle(0, 0, layeredPane.getWidth(), layeredPane.getHeight());
    }

    /**
     * Text which wraps only knows its height once it was laid out at a width.
     */
    private static int preferredHeight(java.awt.Component component, int width) {
        component.setSize(width, Short.MAX_VALUE);
        component.validate();
        return component.getPreferredSize().height;
    }
}
