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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The toasts floating over one window: a column in the bottom right corner of its area - the whole layered pane when it
 * has none - the newest at the bottom. Each toast is as wide as {@link #WIDTH} and as tall as its content at that
 * width. A toast sits in a holder which measures the column again whenever the toast changes - its toolbar, for one,
 * is filled after the toast is shown and takes width from the text.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
final class DesktopToastStack {
    private static final int WIDTH = 360;
    private static final int MARGIN = 8;

    private final JRootPane myRootPane;
    private final List<Toast> myToasts = new ArrayList<>();
    private final Map<Toast, JPanel> myHolders = new IdentityHashMap<>();

    private boolean myRelayouting;
    private boolean myRelayoutQueued;

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
        JPanel holder = new JPanel(new BorderLayout()) {
            @Override
            public void invalidate() {
                super.invalidate();
                queueRelayout();
            }
        };
        holder.setOpaque(false);
        holder.add(TargetAWT.to(toast), BorderLayout.CENTER);
        myToasts.add(toast);
        myHolders.put(toast, holder);
        myRootPane.getLayeredPane().add(holder, JLayeredPane.POPUP_LAYER);
        relayout();
    }

    void hide(Toast toast) {
        JPanel holder = myHolders.remove(toast);
        if (!myToasts.remove(toast) || holder == null) {
            return;
        }
        JLayeredPane layeredPane = myRootPane.getLayeredPane();
        layeredPane.remove(holder);
        layeredPane.repaint(holder.getBounds());
        relayout();
    }

    private void queueRelayout() {
        if (myRelayouting || myRelayoutQueued) {
            return;
        }
        myRelayoutQueued = true;
        SwingUtilities.invokeLater(() -> {
            myRelayoutQueued = false;
            relayout();
        });
    }

    private void relayout() {
        if (myRelayouting) {
            return;
        }
        myRelayouting = true;
        try {
            JLayeredPane layeredPane = myRootPane.getLayeredPane();
            Rectangle area = areaBounds(layeredPane);
            int width = JBUI.scale(WIDTH);
            int margin = JBUI.scale(MARGIN);
            int bottom = area.y + area.height - margin;
            int x = area.x + area.width - width - margin;

            for (int i = myToasts.size() - 1; i >= 0; i--) {
                JPanel holder = myHolders.get(myToasts.get(i));
                if (holder == null) {
                    continue;
                }
                int height = preferredHeight(holder, width);
                int y = bottom - height;
                holder.setBounds(x, y, width, height);
                holder.setVisible(y >= area.y);
                holder.validate();
                bottom = y;
            }
            layeredPane.repaint();
        }
        finally {
            myRelayouting = false;
        }
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
