/*
 * Copyright 2013-2016 consulo.io
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

import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferHandlerAdapter;
import consulo.desktop.awt.ui.impl.clipboard.DesktopAWTTransferTarget;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.TransferHandler;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TabSelectEvent;
import consulo.ui.internal.TabSelectionUtil;
import consulo.ui.ex.awt.JBTabbedPane;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragSource;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2016-06-14
 */
public class DesktopTabbedLayoutImpl extends SwingComponentDelegate<JTabbedPane> implements TabbedLayout, DesktopAWTTransferTarget<Tab> {
    class MyTabbedPane extends JBTabbedPane implements FromSwingComponentWrapper {
        MyTabbedPane(int tabPlacement) {
            super(tabPlacement);
        }

        @Override
        public Component toUIComponent() {
            return DesktopTabbedLayoutImpl.this;
        }
    }

    private final List<DesktopTabImpl> myTabs = new ArrayList<>();

    private final StaticPosition myTabPosition;
    private final int myTabPlacement;

    private @Nullable Component myPrefixComponent;
    private @Nullable Component mySuffixComponent;

    private @Nullable TransferHandler<Tab> myTransferHandler;
    private @Nullable DesktopTabImpl myDraggedTab;
    private boolean myMovingTab;
    private boolean myDragGestureInstalled;

    /**
     * @throws IllegalArgumentException if {@code tabPosition} is {@link StaticPosition#CENTER}, which is not a side
     */
    public DesktopTabbedLayoutImpl(StaticPosition tabPosition) {
        myTabPosition = tabPosition;
        myTabPlacement = toTabPlacement(tabPosition);
    }

    /**
     * The placement is fixed for the lifetime of the layout, so the pane gets it when it is created.
     */
    @Override
    protected JTabbedPane createComponent() {
        JTabbedPane pane = new MyTabbedPane(myTabPlacement);
        pane.addChangeListener(event -> {
            if (myMovingTab) {
                return;
            }

            int index = pane.getSelectedIndex();
            if (index >= 0 && index < myTabs.size()) {
                getListenerDispatcher(TabSelectEvent.class)
                    .onEvent(new TabSelectEvent(this, myTabs.get(index), DesktopAWTInputDetails.currentEvent(pane)));
            }
        });
        return pane;
    }

    private static int toTabPlacement(StaticPosition tabPosition) {
        return switch (tabPosition) {
            case TOP -> SwingConstants.TOP;
            case BOTTOM -> SwingConstants.BOTTOM;
            case LEFT -> SwingConstants.LEFT;
            case RIGHT -> SwingConstants.RIGHT;
            case CENTER -> throw new IllegalArgumentException("CENTER is not a valid tab position");
        };
    }

    @Override
    public StaticPosition getTabPosition() {
        return myTabPosition;
    }

    @Override
    public Tab createTab() {
        return new DesktopTabImpl(this);
    }

    @Override
    public void addStyle(TabbedLayoutStyle style) {
        switch (style) {
            case NO_PADDING -> ((JBTabbedPane) toAWTComponent()).setTabComponentInsets(null);
        }
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(Tab tab, Component component) {
        return addTab(tab, component, myTabs.size());
    }

    @Override
    @RequiredUIAccess
    public Tab addTab(Tab tab, Component component, int index) {
        DesktopTabImpl desktopTab = (DesktopTabImpl) tab;

        desktopTab.setComponent(component);

        int position = Math.max(0, Math.min(index, myTabs.size()));

        JTabbedPane pane = toAWTComponent();
        pane.insertTab("", null, TargetAWT.to(component), null, position);
        pane.setTabComponentAt(position, desktopTab.getTabComponent());
        myTabs.add(position, desktopTab);

        desktopTab.update();

        applyEnabled(desktopTab);

        return tab;
    }

    @Override
    @RequiredUIAccess
    public void moveTab(Tab tab, int index) {
        DesktopTabImpl desktopTab = (DesktopTabImpl) tab;

        int from = myTabs.indexOf(desktopTab);
        int to = Math.max(0, Math.min(index, myTabs.size() - 1));
        if (from == -1 || from == to) {
            return;
        }

        JTabbedPane pane = toAWTComponent();
        boolean selected = pane.getSelectedIndex() == from;
        java.awt.Component content = pane.getComponentAt(from);

        myMovingTab = true;
        try {
            pane.removeTabAt(from);
            myTabs.remove(from);

            pane.insertTab("", null, content, null, to);
            pane.setTabComponentAt(to, desktopTab.getTabComponent());
            myTabs.add(to, desktopTab);

            pane.setEnabledAt(to, desktopTab.isEnabled());
            if (selected) {
                pane.setSelectedIndex(to);
            }
        }
        finally {
            myMovingTab = false;
        }
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<Tab> handler) {
        myTransferHandler = handler;

        JTabbedPane pane = toAWTComponent();
        pane.setTransferHandler(handler == null ? null : new DesktopAWTTransferHandlerAdapter<>(this, handler, this));

        if (handler != null && !myDragGestureInstalled) {
            myDragGestureInstalled = true;
            installDragGesture(pane);
        }
    }

    @Override
    public @Nullable TransferHandler<Tab> getTransferHandler() {
        return myTransferHandler;
    }

    private void installDragGesture(JTabbedPane pane) {
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(pane, DnDConstants.ACTION_COPY_OR_MOVE, event -> {
            if (!(pane.getTransferHandler() instanceof DesktopAWTTransferHandlerAdapter<?> adapter) || !adapter.isDragAndDropSupported()) {
                return;
            }

            Point origin = event.getDragOrigin();
            int index = pane.indexAtLocation(origin.x, origin.y);
            if (index < 0 || index >= myTabs.size()) {
                return;
            }

            DesktopTabImpl tab = myTabs.get(index);
            InputEvent trigger = event.getTriggerEvent();
            int action = event.getDragAction() == DnDConstants.ACTION_COPY ? javax.swing.TransferHandler.COPY : javax.swing.TransferHandler.MOVE;

            myDraggedTab = tab;
            try {
                adapter.exportAsDrag(pane, trigger, action);
            }
            finally {
                myDraggedTab = null;
            }
        });
    }

    @Override
    public List<Tab> getTransferItems() {
        DesktopTabImpl tab = myDraggedTab;
        return tab == null ? List.of() : List.of(tab);
    }

    @Override
    public @Nullable Drop<Tab> resolveDrop(javax.swing.TransferHandler.TransferSupport support) {
        if (myTabs.isEmpty()) {
            return null;
        }

        JTabbedPane pane = toAWTComponent();
        Point point = support.getDropLocation().getDropPoint();
        int index = pane.indexAtLocation(point.x, point.y);
        if (index == -1) {
            return new Drop<>(myTabs.get(myTabs.size() - 1), DragAndDropTransferHandler.DropPosition.BELOW);
        }

        Rectangle bounds = pane.getBoundsAt(index);
        boolean horizontal = myTabPlacement == SwingConstants.TOP || myTabPlacement == SwingConstants.BOTTOM;
        boolean before = horizontal ? point.x < bounds.getCenterX() : point.y < bounds.getCenterY();
        return new Drop<>(myTabs.get(index), before ? DragAndDropTransferHandler.DropPosition.ABOVE : DragAndDropTransferHandler.DropPosition.BELOW);
    }

    @Override
    public void removeTab(Tab tab) {
        DesktopTabImpl desktopTab = (DesktopTabImpl) tab;

        int index = myTabs.indexOf(desktopTab);
        if (index == -1) {
            return;
        }

        toAWTComponent().removeTabAt(index);
        myTabs.remove(index);
    }

    @Override
    public void forEachTab(@RequiredUIAccess BiConsumer<Tab, Component> consumer) {
        for (DesktopTabImpl tab : new ArrayList<>(myTabs)) {
            consumer.accept(tab, tab.getComponent());
        }
    }

    @Override
    public @Nullable Tab getSelectedTab() {
        int index = toAWTComponent().getSelectedIndex();
        return index >= 0 && index < myTabs.size() ? myTabs.get(index) : null;
    }

    int indexOf(DesktopTabImpl tab) {
        return myTabs.indexOf(tab);
    }

    void applyEnabled(DesktopTabImpl tab) {
        tab.updateEnabledLook();

        int index = myTabs.indexOf(tab);
        if (index == -1) {
            return;
        }

        JTabbedPane pane = toAWTComponent();
        boolean enabled = tab.isEnabled();
        if (!enabled && pane.getSelectedIndex() == index) {
            int target = TabSelectionUtil.findSelectionOnDisable(index, myTabs.size(), i -> myTabs.get(i).isEnabled());
            if (target != -1) {
                pane.setSelectedIndex(target);
            }
        }

        pane.setEnabledAt(index, enabled);
    }

    @Override
    public void setPrefixComponent(@Nullable Component prefixComponent) {
        myPrefixComponent = prefixComponent;
        JComponent pane = toAWTComponent();
        pane.putClientProperty("JTabbedPane.leadingComponent", prefixComponent == null ? null : TargetAWT.to(prefixComponent));
    }

    @Override
    public @Nullable Component getPrefixComponent() {
        return myPrefixComponent;
    }

    @Override
    public void setSuffixComponent(@Nullable Component suffixComponent) {
        mySuffixComponent = suffixComponent;
        JComponent pane = toAWTComponent();
        pane.putClientProperty("JTabbedPane.trailingComponent", suffixComponent == null ? null : TargetAWT.to(suffixComponent));
    }

    @Override
    public @Nullable Component getSuffixComponent() {
        return mySuffixComponent;
    }
}
