/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.awt.ui.impl.layout;

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.components.LeftHandScrollbarLayout;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.ScrollablePanel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.internal.ScrollableLayoutInternal;
import consulo.ui.internal.ScrollableLayoutViewportEvent;
import consulo.ui.layout.ScrollableLayoutOptions;
import consulo.ui.layout.VerticalScrollBarPosition;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
public class DesktopScrollableLayoutImpl extends SwingComponentDelegate<JScrollPane> implements ScrollableLayoutInternal {
    class MyJBScrollPane extends JBScrollPane implements FromSwingComponentWrapper {
        private java.awt.@Nullable Component myOverlay;

        MyJBScrollPane(java.awt.Component view) {
            super(view);
        }

        @Override
        public Component toUIComponent() {
            return DesktopScrollableLayoutImpl.this;
        }

        void setOverlay(java.awt.@Nullable Component overlay) {
            if (myOverlay != null) {
                remove(myOverlay);
            }
            myOverlay = overlay;
            if (overlay != null) {
                add(overlay, 0);
            }
            revalidate();
            repaint();
        }

        @Override
        public boolean isOptimizedDrawingEnabled() {
            return myOverlay == null && super.isOptimizedDrawingEnabled();
        }

        @Override
        public void doLayout() {
            super.doLayout();
            java.awt.Component overlay = myOverlay;
            if (overlay != null) {
                Rectangle viewportBounds = getViewport().getBounds();
                overlay.setBounds(viewportBounds.x, viewportBounds.y, viewportBounds.width, overlay.getPreferredSize().height);
            }
        }
    }

    static class WidthTrackingPanel extends ScrollablePanel {
        WidthTrackingPanel(java.awt.Component view) {
            super(new BorderLayout());
            setOpaque(false);
            add(view, BorderLayout.CENTER);
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
        }
    }

    private final @Nullable Component myComponent;
    private final ScrollableLayoutOptions myOptions;

    private final Set<java.awt.Component> myTracked = new HashSet<>();
    private final ComponentListener myTrackedListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            fireViewportChanged();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            fireViewportChanged();
        }
    };

    public DesktopScrollableLayoutImpl(@Nullable Component component, ScrollableLayoutOptions options) {
        myComponent = component;
        myOptions = options;
    }

    @Override
    protected JScrollPane createComponent() {
        java.awt.Component view = TargetAWT.to(myComponent);
        if (view != null && myOptions.getHorizontalScrollPolicy() == ScrollableLayoutOptions.ScrollPolicy.NEVER) {
            view = new WidthTrackingPanel(view);
        }

        MyJBScrollPane pane = new MyJBScrollPane(view);

        pane.setBorder(IdeBorderFactory.createEmptyBorder());
        pane.setViewportBorder(IdeBorderFactory.createEmptyBorder());

        if (myOptions.getVerticalScrollBarPosition() == VerticalScrollBarPosition.LEFT) {
            pane.setLayout(new LeftHandScrollbarLayout());
        }

        switch (myOptions.getHorizontalScrollPolicy()) {
            case ALWAYS:
                pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                break;
            case IF_NEEDED:
                pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                break;
            case NEVER:
                pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                break;
        }

        switch (myOptions.getVerticalScrollPolicy()) {
            case ALWAYS:
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                break;
            case IF_NEEDED:
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
                break;
            case NEVER:
                pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
                break;
        }

        pane.getViewport().addChangeListener(e -> fireViewportChanged());

        return pane;
    }

    private void fireViewportChanged() {
        getListenerDispatcher(ScrollableLayoutViewportEvent.class).onEvent(new ScrollableLayoutViewportEvent(this, getViewRect()));
    }

    @RequiredUIAccess
    @Override
    public Rectangle2D getViewRect() {
        Rectangle rect = toAWTComponent().getViewport().getViewRect();
        return new Rectangle2D(rect.x, rect.y, rect.width, rect.height);
    }

    @RequiredUIAccess
    @Override
    public void scrollTo(Point2D position) {
        JViewport viewport = toAWTComponent().getViewport();
        java.awt.Component view = viewport.getView();
        if (view == null) {
            return;
        }

        int maxX = Math.max(0, view.getWidth() - viewport.getWidth());
        int maxY = Math.max(0, view.getHeight() - viewport.getHeight());
        viewport.setViewPosition(new Point(Math.max(0, Math.min(position.x(), maxX)), Math.max(0, Math.min(position.y(), maxY))));
    }

    @RequiredUIAccess
    @Override
    public void setBoundsTracked(Component descendant, boolean tracked) {
        java.awt.Component component = TargetAWT.to(descendant);
        if (tracked) {
            if (myTracked.add(component)) {
                component.addComponentListener(myTrackedListener);
            }
        }
        else if (myTracked.remove(component)) {
            component.removeComponentListener(myTrackedListener);
        }
    }

    @RequiredUIAccess
    @Override
    public void setTopOverlay(@Nullable Component overlay) {
        ((MyJBScrollPane) toAWTComponent()).setOverlay(overlay == null ? null : TargetAWT.to(overlay));
    }

    @RequiredUIAccess
    @Override
    public @Nullable Rectangle2D getBounds(Component descendant) {
        java.awt.Component component = TargetAWT.to(descendant);
        java.awt.Component view = toAWTComponent().getViewport().getView();
        if (view == null || !myTracked.contains(component) || !SwingUtilities.isDescendingFrom(component, view)) {
            return null;
        }

        Rectangle rect = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), view);
        return new Rectangle2D(rect.x, rect.y, rect.width, rect.height);
    }

    @Override
    public void forEachChild(@RequiredUIAccess Consumer<Component> consumer) {
        Component component = myComponent;
        if (component != null) {
            consumer.accept(component);
        }
    }
}
