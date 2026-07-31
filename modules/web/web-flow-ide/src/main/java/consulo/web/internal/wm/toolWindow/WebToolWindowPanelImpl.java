/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.internal.wm.toolWindow;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import consulo.web.internal.ui.vaadin.AuraUtility;
import consulo.logging.Logger;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.*;
import consulo.ide.impl.wm.impl.ToolWindowAnchorUtil;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.ThreeComponentSplitLayout;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaadin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.InitiableComponent;
import consulo.web.internal.ui.vaadin.VaadinSizeUtil;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2017-09-25
 */
public class WebToolWindowPanelImpl extends VaadinComponentDelegate<WebToolWindowPanelImpl.Vaadin> implements ToolWindowPanel {
    private static final Logger LOG = Logger.getInstance(WebToolWindowPanelImpl.class);

    public class Vaadin extends Div implements FromVaadinComponentWrapper, InitiableComponent, FlexComponent {
        private Div myTopDiv = new Div();
        private Div myCenterDiv = new Div();

        public Vaadin() {
            add(myTopDiv);
            myTopDiv.setWidthFull();
            add(myCenterDiv);
            myCenterDiv.addClassName(AuraUtility.Display.FLEX);
            myCenterDiv.setWidthFull();
            setFlexGrow(1, myCenterDiv);
        }

        @Override
        public @Nullable Component toUIComponent() {
            return WebToolWindowPanelImpl.this;
        }

        @Override
        public void init(String classPrefix) {
            myTopDiv.addClassName(classPrefix + "-top");
            myCenterDiv.addClassName(classPrefix + "-center");
        }
    }

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
        public final void run() {
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
            //getVaadinComponent().markAsDirtyRecursive();
        }
    }

    private final class UpdateButtonPositionCmd implements Runnable {
        private final String myId;

        private UpdateButtonPositionCmd(String id) {
            myId = id;
        }

        @Override
        public void run() {
            WebToolWindowStripeButtonImpl stripeButton = getButtonById(myId);
            if (stripeButton == null) {
                return;
            }

            WindowInfo info = stripeButton.getWindowInfo();
            ToolWindowAnchor anchor = info.getAnchor();

            if (ToolWindowAnchor.TOP == anchor) {
                myTopStripe.markAsDirtyRecursive();
            }
            else if (ToolWindowAnchor.LEFT == anchor) {
                myLeftStripe.markAsDirtyRecursive();
            }
            else if (ToolWindowAnchor.BOTTOM == anchor) {
                myBottomStripe.markAsDirtyRecursive();
            }
            else if (ToolWindowAnchor.RIGHT == anchor) {
                myRightStripe.markAsDirtyRecursive();
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
        public final void run() {
            ToolWindowAnchor anchor = myInfo.getAnchor();

            (myInfo.isSplit() ? myAnchor2Secondary : myAnchor2Primary).put(anchor, myDecorator);

            updateAnchorComponent(anchor, WindowInfoImpl.normalizeWeight(myInfo.getWeight()));
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
        public final void run() {
            ToolWindowAnchor anchor = myInfo.getAnchor();

            (myInfo.isSplit() ? myAnchor2Secondary : myAnchor2Primary).remove(anchor);

            updateAnchorComponent(anchor, WindowInfoImpl.normalizeWeight(myInfo.getWeight()));
        }
    }

    private final class SetEditorComponentCmd implements Runnable {
        private final Component myComponent;

        public SetEditorComponentCmd(Component component) {
            myComponent = component;
        }

        @Override
        @RequiredUIAccess
        public void run() {
            setDocumentComponent(myComponent);
            //myLayeredPane.validate();
            //myLayeredPane.repaint();
        }
    }

    private WebToolWindowStripeImpl myTopStripe = new WebToolWindowStripeImpl(WebToolWindowStripePosition.TOP);
    private WebToolWindowStripeImpl myBottomStripe = new WebToolWindowStripeImpl(WebToolWindowStripePosition.BOTTOM);
    private WebToolWindowStripeImpl myLeftStripe = new WebToolWindowStripeImpl(WebToolWindowStripePosition.LEFT);
    private WebToolWindowStripeImpl myRightStripe = new WebToolWindowStripeImpl(WebToolWindowStripePosition.RIGHT);

    private final Map<String, WebToolWindowStripeButtonImpl> myId2Button = new HashMap<>();
    private final Map<String, ToolWindowInternalDecorator> myId2Decorator = new HashMap<>();
    private final Map<ToolWindowInternalDecorator, WindowInfoImpl> myDecorator2Info = new HashMap<>();
    private final Map<WebToolWindowStripeButtonImpl, WindowInfoImpl> myButton2Info = new HashMap<>();

    private final Map<ToolWindowAnchor, ToolWindowInternalDecorator> myAnchor2Primary = new HashMap<>();
    private final Map<ToolWindowAnchor, ToolWindowInternalDecorator> myAnchor2Secondary = new HashMap<>();

    private ThreeComponentSplitLayout myHorizontalSplitter = ThreeComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);

    private ThreeComponentSplitLayout myVerticalSplitter = ThreeComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);

    private boolean myWidescreen;

    public WebToolWindowPanelImpl() {
        Vaadin vaadinComponent = getVaadinComponent();

        vaadinComponent.myTopDiv.add(TargetVaadin.to(myTopStripe));
        vaadinComponent.myCenterDiv.add(TargetVaadin.to(myLeftStripe));

        // same nesting as the awt panel - the outer splitter holds the stripes of its own orientation,
        // the inner one is placed in its center and ends up holding the editor
        ThreeComponentSplitLayout rootSplitter = myWidescreen ? myHorizontalSplitter : myVerticalSplitter;
        if (myWidescreen) {
            myHorizontalSplitter.setCenterComponent(myVerticalSplitter);
        }
        else {
            myVerticalSplitter.setCenterComponent(myHorizontalSplitter);
        }

        VaadinSizeUtil.setWidthFull(rootSplitter);
        com.vaadin.flow.component.Component splitter = TargetVaadin.to(rootSplitter);
        vaadinComponent.myCenterDiv.add(splitter);
        vaadinComponent.myCenterDiv.add(TargetVaadin.to(myRightStripe));

        // tttttttttttttttttttttttttttttttt
        // l                              r
        // l                              r
        // l            content           r
        // l                              r
        // l                              r
        //
        // the bottom stripe is not part of the panel - the tool window manager hands it to the status bar,
        // where it shares one row with the widgets, like the awt frame does

        splitter.addClassName("web-tool-window-content");
    }

    public WebToolWindowStripeImpl getBottomStripe() {
        return myBottomStripe;
    }

    
    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    /**
     * Rebuilds what sits at the anchor - a single decorator, or both of them inside a splitter when the
     * anchor holds a primary and a split window at once, like the awt panel does.
     */
    @RequiredUIAccess
    private void updateAnchorComponent(ToolWindowAnchor anchor, float weight) {
        WebToolWindowInternalDecorator primary = (WebToolWindowInternalDecorator) myAnchor2Primary.get(anchor);
        WebToolWindowInternalDecorator secondary = (WebToolWindowInternalDecorator) myAnchor2Secondary.get(anchor);

        consulo.ui.Component component;
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
    private void setComponent(consulo.ui.@Nullable Component component, ToolWindowAnchor anchor, float weight) {
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

    private @Nullable WebToolWindowStripeButtonImpl getButtonById(String id) {
        return myId2Button.get(id);
    }

    @Override
    @RequiredUIAccess
    public void addButton(ToolWindowStripeButton button, WindowInfo info, Comparator<ToolWindowStripeButton> comparator) {
        WindowInfoImpl copiedInfo = ((WindowInfoImpl) info).copy();
        myId2Button.put(copiedInfo.getId(), (WebToolWindowStripeButtonImpl) button);
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
            //return new RemoveSlidingComponentCmd(decorator, info, dirtyMode, finishCallBack);
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
            //return new AddSlidingComponentCmd((DesktopInternalDecorator)decorator, info, dirtyMode, finishCallBack);
        }
        else {
            throw new IllegalArgumentException("Unknown window type: " + info.getType());
        }
    }

    @Override
    @RequiredUIAccess
    public void updateButtonPosition(String id) {
        new UpdateButtonPositionCmd(id).run();
    }

    @Override
    @RequiredUIAccess
    public void setEditorComponent(Object component) {
        new SetEditorComponentCmd((Component) component).run();
    }
}
