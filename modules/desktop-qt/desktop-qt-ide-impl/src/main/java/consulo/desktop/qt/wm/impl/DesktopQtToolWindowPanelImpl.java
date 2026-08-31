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
package consulo.desktop.qt.wm.impl;

import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;
import consulo.ui.ex.toolWindow.ToolWindowPanel;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.ex.toolWindow.WindowInfo;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.ThreeComponentSplitLayout;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtToolWindowPanelImpl implements ToolWindowPanel {
    private static final Logger LOG = Logger.getInstance(DesktopQtToolWindowPanelImpl.class);

    private final class AddToolStripeButtonCmd implements Runnable {
        private final ToolWindowStripeButton myButton;
        private final WindowInfoImpl myInfo;
        private final Comparator<ToolWindowStripeButton> myComparator;

        public AddToolStripeButtonCmd(
            ToolWindowStripeButton button,
            WindowInfoImpl info,
            Comparator<ToolWindowStripeButton> comparator
        ) {
            myButton = button;
            myInfo = info;
            myComparator = comparator;
        }

        @Override
        public void run() {
            ToolWindowAnchor anchor = myInfo.getAnchor();
            if (ToolWindowAnchor.TOP == anchor) {
                myTopStripe.addButton(myButton, myComparator);
            }
            else if (ToolWindowAnchor.LEFT == anchor) {
                myLeftStripe.addButton(myButton, myComparator);
            }
            else if (ToolWindowAnchor.BOTTOM == anchor) {
                myBottomStripe.addButton(myButton, myComparator);
            }
            else if (ToolWindowAnchor.RIGHT == anchor) {
                myRightStripe.addButton(myButton, myComparator);
            }
            else {
                LOG.error("unknown anchor: " + anchor);
            }
        }
    }

    private final class UpdateButtonPositionCmd implements Runnable {
        private final String myId;

        private UpdateButtonPositionCmd(String id) {
            myId = id;
        }

        @Override
        public void run() {
            DesktopQtToolWindowStripeButtonImpl stripeButton = getButtonById(myId);
            if (stripeButton == null) {
                return;
            }

            WindowInfo info = stripeButton.getWindowInfo();
            ToolWindowAnchor anchor = info.getAnchor();

            if (ToolWindowAnchor.TOP == anchor) {
            }
            else if (ToolWindowAnchor.LEFT == anchor) {
            }
            else if (ToolWindowAnchor.BOTTOM == anchor) {
            }
            else if (ToolWindowAnchor.RIGHT == anchor) {
            }
            else {
                LOG.error("unknown anchor: " + anchor);
            }
        }
    }

    private final class AddDockedComponentCmd implements Runnable {
        private final ToolWindowInternalDecorator myDecorator;
        private final WindowInfoImpl myInfo;
        private final boolean myDirtyMode;

        public AddDockedComponentCmd(ToolWindowInternalDecorator decorator, WindowInfoImpl info, boolean dirtyMode) {
            myDecorator = decorator;
            myInfo = info;
            myDirtyMode = dirtyMode;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            ToolWindowAnchor anchor = myInfo.getAnchor();

            setComponent(myDecorator, anchor, WindowInfoImpl.normalizeWeight(myInfo.getWeight()));
        }
    }

    private final class RemoveDockedComponentCmd implements Runnable {
        private final WindowInfoImpl myInfo;
        private final boolean myDirtyMode;

        public RemoveDockedComponentCmd(WindowInfoImpl info, boolean dirtyMode) {
            myInfo = info;
            myDirtyMode = dirtyMode;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            setComponent(null, myInfo.getAnchor(), 0);
        }
    }

    private final DesktopToolWindowStripeImpl myTopStripe = new DesktopToolWindowStripeImpl(DesktopToolWindowStripeImpl.Position.TOP);
    private final DesktopToolWindowStripeImpl myBottomStripe = new DesktopToolWindowStripeImpl(DesktopToolWindowStripeImpl.Position.BOTTOM);
    private final DesktopToolWindowStripeImpl myLeftStripe = new DesktopToolWindowStripeImpl(DesktopToolWindowStripeImpl.Position.LEFT);
    private final DesktopToolWindowStripeImpl myRightStripe = new DesktopToolWindowStripeImpl(DesktopToolWindowStripeImpl.Position.RIGHT);

    private final Map<String, DesktopQtToolWindowStripeButtonImpl> myId2Button = new HashMap<>();
    private final Map<String, ToolWindowInternalDecorator> myId2Decorator = new HashMap<>();
    private final Map<ToolWindowInternalDecorator, WindowInfoImpl> myDecorator2Info = new HashMap<>();
    private final Map<DesktopQtToolWindowStripeButtonImpl, WindowInfoImpl> myButton2Info = new HashMap<>();

    private final ThreeComponentSplitLayout myHorizontalSplitter = ThreeComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
    @Deprecated
    @DeprecationInfo("Unsupported for now")
    private final ThreeComponentSplitLayout myVerticalSplitter = ThreeComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);

    private boolean myWidescreen;

    private final DockLayout myRoot = DockLayout.create();

    @RequiredUIAccess
    public DesktopQtToolWindowPanelImpl() {
        myRoot.top(myTopStripe);
        myRoot.bottom(myBottomStripe);
        myRoot.left(myLeftStripe);
        myRoot.right(myRightStripe);
        myRoot.center(myHorizontalSplitter);
    }

    public DockLayout getComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private void setComponent(@Nullable ToolWindowInternalDecorator d, ToolWindowAnchor anchor, float weight) {
        DesktopQtToolWindowInternalDecorator decorator = (DesktopQtToolWindowInternalDecorator) d;

        Component component = decorator == null ? null : decorator.getComponent();

        if (ToolWindowAnchor.TOP == anchor) {
        }
        else if (ToolWindowAnchor.LEFT == anchor) {
            myHorizontalSplitter.setFirstComponent(component);
        }
        else if (ToolWindowAnchor.BOTTOM == anchor) {
        }
        else if (ToolWindowAnchor.RIGHT == anchor) {
            myHorizontalSplitter.setSecondComponent(component);
        }
    }

    @RequiredUIAccess
    private void setDocumentComponent(Component component) {
        (myWidescreen ? myVerticalSplitter : myHorizontalSplitter).setCenterComponent(component);
    }

    private @Nullable DesktopQtToolWindowStripeButtonImpl getButtonById(String id) {
        return myId2Button.get(id);
    }

    @Override
    @RequiredUIAccess
    public void addButton(ToolWindowStripeButton button, WindowInfo info, Comparator<ToolWindowStripeButton> comparator) {
        WindowInfoImpl copiedInfo = ((WindowInfoImpl) info).copy();
        myId2Button.put(copiedInfo.getId(), (DesktopQtToolWindowStripeButtonImpl) button);
        new AddToolStripeButtonCmd(button, copiedInfo, comparator).run();
    }

    @Override
    @RequiredUIAccess
    public void removeButton(String id) {
        // todo
    }

    @RequiredUIAccess
    @Override
    public void removeDecorator(String id, boolean dirtyMode) {
        ToolWindowInternalDecorator decorator = getDecoratorById(id);
        WindowInfoImpl info = getDecoratorInfoById(id);

        myDecorator2Info.remove(decorator);
        myId2Decorator.remove(id);

        WindowInfoImpl sideInfo = getDockedInfoAt(info.getAnchor(), !info.isSplit());

        if (info.isDocked()) {
            if (sideInfo == null) {
                new RemoveDockedComponentCmd(info, dirtyMode).run();
            }
        }
        else if (info.isSliding()) {
        }
        else {
            throw new IllegalArgumentException("Unknown window type");
        }
    }

    private WindowInfoImpl getDecoratorInfoById(String id) {
        return myDecorator2Info.get(myId2Decorator.get(id));
    }

    private ToolWindowInternalDecorator getDecoratorById(String id) {
        return myId2Decorator.get(id);
    }

    @RequiredUIAccess
    @Override
    public void addDecorator(ToolWindowInternalDecorator decorator, WindowInfo info, boolean dirtyMode) {
        WindowInfoImpl copiedInfo = ((WindowInfoImpl) info).copy();
        String id = copiedInfo.getId();

        myDecorator2Info.put(decorator, copiedInfo);
        myId2Decorator.put(id, decorator);

        if (info.isDocked()) {
            WindowInfoImpl sideInfo = getDockedInfoAt(info.getAnchor(), !info.isSplit());
            if (sideInfo == null) {
                new AddDockedComponentCmd(decorator, (WindowInfoImpl) info, dirtyMode).run();
            }
        }
        else if (info.isSliding()) {
        }
        else {
            throw new IllegalArgumentException("Unknown window type: " + info.getType());
        }
    }

    private WindowInfoImpl getDockedInfoAt(ToolWindowAnchor anchor, boolean side) {
        for (WindowInfoImpl info : myDecorator2Info.values()) {
            if (info.isVisible() && info.isDocked() && info.getAnchor() == anchor && side == info.isSplit()) {
                return info;
            }
        }

        return null;
    }

    @RequiredUIAccess
    @Override
    public void updateButtonPosition(String id) {
        new UpdateButtonPositionCmd(id).run();
    }

    @RequiredUIAccess
    @Override
    public void setEditorComponent(Object component) {
        setDocumentComponent((Component) component);
    }
}
