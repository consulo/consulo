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
package consulo.desktop.awt.ui.impl.layout;

import consulo.desktop.awt.ui.impl.DesktopLength;
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferHandlerAdapter;
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferTarget;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.TransferHandler;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.LayoutConstraint;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Drag and drop of children laid out by a {@link DesktopViewportLayoutManager}: the base of {@link DesktopTileLayoutImpl} and
 * {@link DesktopFlowLayoutImpl}.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public abstract class DesktopTileLayoutBase extends DesktopLayoutBase<JPanel, LayoutConstraint> implements DesktopAWTTransferTarget<Component> {
    private static final int VISIBLE_ROW_COUNT = 2;

    private class MyTilePanel extends JPanel implements Scrollable, FromSwingComponentWrapper {
        private final DesktopViewportLayoutManager myLayoutManager;

        MyTilePanel(DesktopViewportLayoutManager layoutManager) {
            super(layoutManager);
            myLayoutManager = layoutManager;
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return myLayoutManager.preferredViewportSize(this, VISIBLE_ROW_COUNT);
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return DesktopLength.lineHeight(this);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
        }

        @Override
        public Component toUIComponent() {
            return DesktopTileLayoutBase.this;
        }
    }

    private @Nullable TransferHandler<Component> myTransferHandler;
    private @Nullable Component myDraggedChild;
    private boolean myDragGestureInstalled;

    protected abstract DesktopViewportLayoutManager createLayoutManager();

    @Override
    protected JPanel createComponent() {
        return new MyTilePanel(createLayoutManager());
    }

    @Override
    public void remove(Component component) {
        JPanel panel = toAWTComponent();
        java.awt.Component awtComponent = TargetAWT.to(component);
        if (awtComponent.getParent() == panel) {
            panel.remove(awtComponent);
            panel.revalidate();
            panel.repaint();
        }
    }

    public void setTransferHandler(@Nullable TransferHandler<Component> handler) {
        myTransferHandler = handler;

        JPanel panel = toAWTComponent();
        panel.setTransferHandler(handler == null ? null : new DesktopAWTTransferHandlerAdapter<>(this, handler, this));

        if (handler != null && !myDragGestureInstalled) {
            myDragGestureInstalled = true;
            DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(panel, DnDConstants.ACTION_COPY_OR_MOVE, event -> {
                if (!(panel.getTransferHandler() instanceof DesktopAWTTransferHandlerAdapter<?> adapter) || !adapter.isDragAndDropSupported()) {
                    return;
                }

                Component child = childAt(event.getDragOrigin());
                if (child == null) {
                    return;
                }

                InputEvent trigger = event.getTriggerEvent();
                int action = event.getDragAction() == DnDConstants.ACTION_COPY ? javax.swing.TransferHandler.COPY : javax.swing.TransferHandler.MOVE;
                myDraggedChild = child;
                try {
                    adapter.exportAsDrag(panel, trigger, action);
                }
                finally {
                    myDraggedChild = null;
                }
            });
        }
    }

    public @Nullable TransferHandler<Component> getTransferHandler() {
        return myTransferHandler;
    }

    @Override
    public List<Component> getTransferItems() {
        Component child = myDraggedChild;
        return child == null ? List.of() : List.of(child);
    }

    @Override
    public @Nullable Drop<Component> resolveDrop(javax.swing.TransferHandler.TransferSupport support) {
        List<Component> children = children();
        if (children.isEmpty()) {
            return null;
        }

        Point point = support.getDropLocation().getDropPoint();
        Component child = childAt(point);
        if (child == null) {
            return new Drop<>(children.get(children.size() - 1), DragAndDropTransferHandler.DropPosition.BELOW);
        }

        java.awt.Component awtChild = TargetAWT.to(child);
        boolean before = point.x < awtChild.getX() + awtChild.getWidth() / 2;
        return new Drop<>(child, before ? DragAndDropTransferHandler.DropPosition.ABOVE : DragAndDropTransferHandler.DropPosition.BELOW);
    }

    private List<Component> children() {
        List<Component> children = new ArrayList<>();
        forEachChild(children::add);
        return children;
    }

    private @Nullable Component childAt(Point point) {
        for (Component child : children()) {
            java.awt.Component awtChild = TargetAWT.to(child);
            if (awtChild.isVisible() && awtChild.getBounds().contains(point)) {
                return child;
            }
        }
        return null;
    }
}
