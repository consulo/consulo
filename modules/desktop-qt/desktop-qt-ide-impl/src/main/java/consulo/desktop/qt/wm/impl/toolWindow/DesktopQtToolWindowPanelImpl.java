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
package consulo.desktop.qt.wm.impl.toolWindow;

import consulo.ide.impl.wm.impl.ToolWindowAnchorUtil;
import consulo.logging.Logger;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;
import consulo.ui.ex.toolWindow.ToolWindowPanel;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.ex.toolWindow.WindowInfo;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.ThreeComponentSplitLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtToolWindowPanelImpl implements ToolWindowPanel, PseudoComponent {
    private static final Logger LOG = Logger.getInstance(DesktopQtToolWindowPanelImpl.class);

    private final class AddToolStripeButtonCmd implements Runnable {
        private final ToolWindowStripeButton myButton;
        private final WindowInfoImpl myInfo;
        private final Comparator<ToolWindowStripeButton> myComparator;

        private AddToolStripeButtonCmd(
            ToolWindowStripeButton button,
            WindowInfoImpl info,
            Comparator<ToolWindowStripeButton> comparator
        ) {
            myButton = button;
            myInfo = info;
            myComparator = comparator;
        }

        @Override
        @RequiredUIAccess
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

    private final class AddDockedComponentCmd implements Runnable {
        private final ToolWindowInternalDecorator myDecorator;
        private final WindowInfoImpl myInfo;
        private final boolean myDirtyMode;

        private AddDockedComponentCmd(ToolWindowInternalDecorator decorator, WindowInfoImpl info, boolean dirtyMode) {
            myDecorator = decorator;
            myInfo = info;
            myDirtyMode = dirtyMode;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            ToolWindowAnchor anchor = myInfo.getAnchor();

            (myInfo.isSplit() ? myAnchor2Secondary : myAnchor2Primary).put(anchor, myDecorator);

            updateAnchorComponent(anchor, WindowInfoImpl.normalizeWeight(myInfo.getWeight()));
        }
    }

    private final class RemoveDockedComponentCmd implements Runnable {
        private final WindowInfoImpl myInfo;
        private final boolean myDirtyMode;

        private RemoveDockedComponentCmd(WindowInfoImpl info, boolean dirtyMode) {
            myInfo = info;
            myDirtyMode = dirtyMode;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            ToolWindowAnchor anchor = myInfo.getAnchor();

            (myInfo.isSplit() ? myAnchor2Secondary : myAnchor2Primary).remove(anchor);

            updateAnchorComponent(anchor, WindowInfoImpl.normalizeWeight(myInfo.getWeight()));
        }
    }

    private final class SetEditorComponentCmd implements Runnable {
        private final Component myComponent;

        private SetEditorComponentCmd(Component component) {
            myComponent = component;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            setDocumentComponent(myComponent);
        }
    }

    private final DesktopQtToolWindowStripeImpl myTopStripe = new DesktopQtToolWindowStripeImpl(DesktopQtToolWindowStripePosition.TOP);
    private final DesktopQtToolWindowStripeImpl myBottomStripe =
        new DesktopQtToolWindowStripeImpl(DesktopQtToolWindowStripePosition.BOTTOM);
    private final DesktopQtToolWindowStripeImpl myLeftStripe = new DesktopQtToolWindowStripeImpl(DesktopQtToolWindowStripePosition.LEFT);
    private final DesktopQtToolWindowStripeImpl myRightStripe = new DesktopQtToolWindowStripeImpl(DesktopQtToolWindowStripePosition.RIGHT);

    private final Map<String, DesktopQtToolWindowStripeButtonImpl> myId2Button = new HashMap<>();
    private final Map<String, ToolWindowInternalDecorator> myId2Decorator = new HashMap<>();
    private final Map<ToolWindowInternalDecorator, WindowInfoImpl> myDecorator2Info = new HashMap<>();

    private final Map<ToolWindowAnchor, ToolWindowInternalDecorator> myAnchor2Primary = new HashMap<>();
    private final Map<ToolWindowAnchor, ToolWindowInternalDecorator> myAnchor2Secondary = new HashMap<>();

    private final ThreeComponentSplitLayout myHorizontalSplitter = ThreeComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
    private final ThreeComponentSplitLayout myVerticalSplitter = ThreeComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);

    private final DockLayout myRoot = DockLayout.create(0);

    private boolean myWidescreen;

    @RequiredUIAccess
    public DesktopQtToolWindowPanelImpl() {
        // same nesting as the awt panel - the outer splitter holds the stripes of its own orientation,
        // the inner one is placed in its center and ends up holding the editor
        ThreeComponentSplitLayout rootSplitter = myWidescreen ? myHorizontalSplitter : myVerticalSplitter;
        if (myWidescreen) {
            myHorizontalSplitter.setCenterComponent(myVerticalSplitter);
        }
        else {
            myVerticalSplitter.setCenterComponent(myHorizontalSplitter);
        }

        // tttttttttttttttttttttttttttttttt
        // l                              r
        // l                              r
        // l            content           r
        // l                              r
        // l                              r
        //
        // the bottom stripe is not part of the panel - the tool window manager hands it to the status bar,
        // where it shares one row with the widgets, like the awt frame does
        myRoot.top(myTopStripe);
        myRoot.left(myLeftStripe);
        myRoot.center(rootSplitter);
        myRoot.right(myRightStripe);
    }

    @Override
    public Component getComponent() {
        return myRoot;
    }

    public DesktopQtToolWindowStripeImpl getBottomStripe() {
        return myBottomStripe;
    }

    /**
     * Rebuilds what sits at the anchor - a single decorator, or both of them inside a splitter when the
     * anchor holds a primary and a split window at once, like the awt panel does.
     */
    @RequiredUIAccess
    private void updateAnchorComponent(ToolWindowAnchor anchor, float weight) {
        DesktopQtToolWindowInternalDecorator primary = (DesktopQtToolWindowInternalDecorator) myAnchor2Primary.get(anchor);
        DesktopQtToolWindowInternalDecorator secondary = (DesktopQtToolWindowInternalDecorator) myAnchor2Secondary.get(anchor);

        Component component;
        if (primary != null && secondary != null) {
            TwoComponentSplitLayout splitter = TwoComponentSplitLayout.create(
                ToolWindowAnchorUtil.isSplitVertically(anchor) ? SplitLayoutPosition.VERTICAL : SplitLayoutPosition.HORIZONTAL
            );
            splitter.setFirstComponent(primary.getComponent());
            splitter.setSecondComponent(secondary.getComponent());
            splitter.setProportion(50);

            component = splitter;
        }
        else if (primary != null) {
            component = primary.getComponent();
        }
        else if (secondary != null) {
            component = secondary.getComponent();
        }
        else {
            component = null;
        }

        setComponent(component, anchor, weight);
    }

    @RequiredUIAccess
    private void setComponent(@Nullable Component component, ToolWindowAnchor anchor, float weight) {
        if (ToolWindowAnchor.TOP == anchor) {
            myVerticalSplitter.setFirstComponent(component);
        }
        else if (ToolWindowAnchor.LEFT == anchor) {
            myHorizontalSplitter.setFirstComponent(component);
        }
        else if (ToolWindowAnchor.BOTTOM == anchor) {
            myVerticalSplitter.setSecondComponent(component);
        }
        else if (ToolWindowAnchor.RIGHT == anchor) {
            myHorizontalSplitter.setSecondComponent(component);
        }
        else {
            LOG.error("unknown anchor: " + anchor);
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

    @Override
    @RequiredUIAccess
    public void removeDecorator(String id, boolean dirtyMode) {
        ToolWindowInternalDecorator decorator = getDecoratorById(id);
        WindowInfoImpl info = getDecoratorInfoById(id);

        myDecorator2Info.remove(decorator);
        myId2Decorator.remove(id);

        if (info.isDocked()) {
            new RemoveDockedComponentCmd(info, dirtyMode).run();
        }
        else if (info.isSliding()) {
            // todo
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

    @Override
    @RequiredUIAccess
    public void addDecorator(ToolWindowInternalDecorator decorator, WindowInfo info, boolean dirtyMode) {
        WindowInfoImpl copiedInfo = ((WindowInfoImpl) info).copy();
        String id = copiedInfo.getId();

        myDecorator2Info.put(decorator, copiedInfo);
        myId2Decorator.put(id, decorator);

        if (info.isDocked()) {
            new AddDockedComponentCmd(decorator, copiedInfo, dirtyMode).run();
        }
        else if (info.isSliding()) {
            // todo
        }
        else {
            throw new IllegalArgumentException("Unknown window type: " + info.getType());
        }
    }

    @Override
    @RequiredUIAccess
    public void updateButtonPosition(String id) {
        DesktopQtToolWindowStripeButtonImpl stripeButton = getButtonById(id);
        if (stripeButton == null) {
            return;
        }

        // a qt layout re-arranges itself, so only the presentation of the button is left to refresh here
        stripeButton.updatePresentation();
    }

    @Override
    @RequiredUIAccess
    public void setEditorComponent(Object component) {
        new SetEditorComponentCmd((Component) component).run();
    }
}
