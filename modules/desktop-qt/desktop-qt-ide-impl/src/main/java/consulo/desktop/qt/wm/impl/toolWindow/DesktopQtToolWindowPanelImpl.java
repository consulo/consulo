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

import consulo.desktop.qt.ui.impl.layout.DesktopQtThreeComponentSplitLayoutImpl;
import consulo.ide.impl.wm.impl.ToolWindowAnchorUtil;
import consulo.ide.impl.wm.impl.UnifiedToolWindowSplitters;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.impl.internal.wm.ToolWindowBase;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.Space;
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
import java.util.List;
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

            getAnchorDecorators(myInfo).put(anchor, myDecorator);

            updateAnchorComponent(anchor);
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

            getAnchorDecorators(myInfo).remove(anchor);

            updateAnchorComponent(anchor);
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
    private final Map<ToolWindowAnchor, ToolWindowInternalDecorator> myAnchor2Sliding = new HashMap<>();

    private final DockLayout myRoot = DockLayout.create(Space.NONE);

    private final UnifiedToolWindowSplitters mySplitters;

    @RequiredUIAccess
    public DesktopQtToolWindowPanelImpl(Project project) {
        mySplitters = new UnifiedToolWindowSplitters(project, project, this::setRootSplitter);

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
        myRoot.center(mySplitters.getRootSplitter());
        myRoot.right(myRightStripe);
    }

    @RequiredUIAccess
    private void setRootSplitter(ThreeComponentSplitLayout rootSplitter) {
        myRoot.center(rootSplitter);
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
    private void updateAnchorComponent(ToolWindowAnchor anchor) {
        DesktopQtToolWindowInternalDecorator primary = (DesktopQtToolWindowInternalDecorator) myAnchor2Primary.get(anchor);
        DesktopQtToolWindowInternalDecorator secondary = (DesktopQtToolWindowInternalDecorator) myAnchor2Secondary.get(anchor);
        DesktopQtToolWindowInternalDecorator sliding = (DesktopQtToolWindowInternalDecorator) myAnchor2Sliding.get(anchor);

        DesktopQtToolWindowInternalDecorator placed = sliding != null ? sliding : primary != null ? primary : secondary;
        float weight = placed == null ? 0 : WindowInfoImpl.normalizeWeight(myDecorator2Info.get(placed).getWeight());

        Component component;
        if (sliding != null) {
            component = sliding.getComponent();
        }
        else if (primary != null && secondary != null) {
            ToolWindowBase toolWindow = (ToolWindowBase) primary.getToolWindow();
            TwoComponentSplitLayout splitter = TwoComponentSplitLayout.create(
                ToolWindowAnchorUtil.isSplitVertically(toolWindow.getToolWindowManager().getProject(), anchor)
                    ? SplitLayoutPosition.VERTICAL
                    : SplitLayoutPosition.HORIZONTAL
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
        ThreeComponentSplitLayout layout = anchor.isHorizontal()
            ? mySplitters.getVerticalSplitter()
            : mySplitters.getHorizontalSplitter();
        if (component != null && layout instanceof DesktopQtThreeComponentSplitLayoutImpl splitter) {
            if (ToolWindowAnchor.TOP == anchor || ToolWindowAnchor.LEFT == anchor) {
                splitter.setFirstWeight(weight);
            }
            else {
                splitter.setSecondWeight(weight);
            }
        }

        mySplitters.setComponent(anchor, component);
    }

    private Map<ToolWindowAnchor, ToolWindowInternalDecorator> getAnchorDecorators(WindowInfo info) {
        if (info.isSliding()) {
            return myAnchor2Sliding;
        }
        return info.isSplit() ? myAnchor2Secondary : myAnchor2Primary;
    }

    @RequiredUIAccess
    private void setDocumentComponent(Component component) {
        mySplitters.setDocumentComponent(component);
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
        DesktopQtToolWindowStripeButtonImpl button = myId2Button.remove(id);
        if (button == null) {
            return;
        }

        for (DesktopQtToolWindowStripeImpl stripe : List.of(myTopStripe, myLeftStripe, myBottomStripe, myRightStripe)) {
            stripe.removeButton(button);
        }
    }

    @Override
    @RequiredUIAccess
    public void removeDecorator(String id, boolean dirtyMode) {
        ToolWindowInternalDecorator decorator = getDecoratorById(id);
        WindowInfoImpl info = getDecoratorInfoById(id);

        myDecorator2Info.remove(decorator);
        myId2Decorator.remove(id);

        if (info.isDocked() || info.isSliding()) {
            new RemoveDockedComponentCmd(info, dirtyMode).run();
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

        if (info.isDocked() || info.isSliding()) {
            new AddDockedComponentCmd(decorator, copiedInfo, dirtyMode).run();
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
