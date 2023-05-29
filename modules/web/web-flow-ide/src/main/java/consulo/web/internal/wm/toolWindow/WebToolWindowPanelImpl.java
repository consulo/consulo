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
import com.vaadin.flow.theme.lumo.LumoUtility;
import consulo.annotation.DeprecationInfo;
import consulo.logging.Logger;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.*;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.ThreeComponentSplitLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.InitiableComponent;
import consulo.web.internal.ui.vaadin.VaadinSizeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WebToolWindowPanelImpl extends VaadinComponentDelegate<WebToolWindowPanelImpl.Vaadin> implements ToolWindowPanel {
  private static final Logger LOG = Logger.getInstance(WebToolWindowPanelImpl.class);

  public class Vaadin extends Div implements FromVaadinComponentWrapper, InitiableComponent {
    private Div myTopDiv = new Div();
    private Div myCenterDiv = new Div();
    private Div myBottomDiv = new Div();

    public Vaadin() {
      add(myTopDiv);
      myTopDiv.setWidthFull();
      add(myCenterDiv);
      myCenterDiv.addClassName(LumoUtility.Display.FLEX);
      myCenterDiv.setSizeFull();
      add(myBottomDiv);
      myBottomDiv.setWidthFull();
    }

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebToolWindowPanelImpl.this;
    }

    @Override
    public void init(String classPrefix) {
      myTopDiv.addClassName(classPrefix + "-top");
      myCenterDiv.addClassName(classPrefix + "-center");
      myBottomDiv.addClassName(classPrefix + "-bottom");
    }
  }

  private final class AddToolStripeButtonCmd implements Runnable {
    private final ToolWindowStripeButton myButton;
    private final WindowInfoImpl myInfo;
    private final Comparator<ToolWindowStripeButton> myComparator;

    public AddToolStripeButtonCmd(final ToolWindowStripeButton button,
                                  @Nonnull WindowInfoImpl info,
                                  @Nonnull Comparator<ToolWindowStripeButton> comparator) {
      myButton = button;
      myInfo = info;
      myComparator = comparator;
    }

    @Override
    public final void run() {
      final ToolWindowAnchor anchor = myInfo.getAnchor();
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

    private UpdateButtonPositionCmd(@Nonnull String id) {
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

    public AddDockedComponentCmd(@Nonnull ToolWindowInternalDecorator decorator, @Nonnull WindowInfoImpl info, final boolean dirtyMode) {
      myDecorator = decorator;
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public final void run() {
      final ToolWindowAnchor anchor = myInfo.getAnchor();

      setComponent(myDecorator, anchor, WindowInfoImpl.normalizeWeigh(myInfo.getWeight()));
    }
  }

  private final class RemoveDockedComponentCmd implements Runnable {
    private final WindowInfoImpl myInfo;
    private final boolean myDirtyMode;

    public RemoveDockedComponentCmd(@Nonnull WindowInfoImpl info, final boolean dirtyMode) {
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public final void run() {
      setComponent(null, myInfo.getAnchor(), 0);
    }
  }

  private final class SetEditorComponentCmd implements Runnable {
    private final Component myComponent;

    public SetEditorComponentCmd(Component component) {
      myComponent = component;
    }

    @Override
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


  private ThreeComponentSplitLayout myHorizontalSplitter = ThreeComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
  @Deprecated
  @DeprecationInfo("Unsupported for now")
  private ThreeComponentSplitLayout myVerticalSplitter = ThreeComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);

  private boolean myWidescreen;

  public WebToolWindowPanelImpl() {
    Vaadin vaadinComponent = getVaadinComponent();

    vaadinComponent.myTopDiv.add(TargetVaddin.to(myTopStripe));
    vaadinComponent.myCenterDiv.add(TargetVaddin.to(myLeftStripe));
    VaadinSizeUtil.setWidthFull(myHorizontalSplitter);
    com.vaadin.flow.component.Component splitter = TargetVaddin.to(myHorizontalSplitter);
    vaadinComponent.myCenterDiv.add(splitter);
    vaadinComponent.myCenterDiv.add(TargetVaddin.to(myRightStripe));
    vaadinComponent.myBottomDiv.add(TargetVaddin.to(myBottomStripe));

    // tttttttttttttttttttttttttttttttt
    // l                              r
    // l                              r
    // l            content           r
    // l                              r
    // l                              r
    // bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb

    splitter.addClassName("web-tool-window-content");
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  private void setComponent(@Nullable ToolWindowInternalDecorator d, @Nonnull ToolWindowAnchor anchor, final float weight) {
    WebToolWindowInternalDecorator decorator = (WebToolWindowInternalDecorator)d;

    consulo.ui.Component component = decorator == null ? null : decorator.getComponent();

    if (ToolWindowAnchor.TOP == anchor) {
      //myVerticalSplitter.setFirstComponent(component);
      //myVerticalSplitter.setFirstSize((int)(myLayeredPane.getHeight() * weight));
    }
    else if (ToolWindowAnchor.LEFT == anchor) {
      myHorizontalSplitter.setFirstComponent(component);
      //myHorizontalSplitter.setFirstSize((int)(myLayeredPane.getWidth() * weight));
    }
    else if (ToolWindowAnchor.BOTTOM == anchor) {
      //myVerticalSplitter.setLastComponent(component);
      //myVerticalSplitter.setLastSize((int)(myLayeredPane.getHeight() * weight));
    }
    else if (ToolWindowAnchor.RIGHT == anchor) {
      myHorizontalSplitter.setSecondComponent(component);
      //myHorizontalSplitter.setLastSize((int)(myLayeredPane.getWidth() * weight));
    }
    else {
      //LOG.error("unknown anchor: " + anchor);
    }
  }

  private void setDocumentComponent(Component component) {
    (myWidescreen ? myVerticalSplitter : myHorizontalSplitter).setCenterComponent(component);
  }

  @Nullable
  private WebToolWindowStripeButtonImpl getButtonById(final String id) {
    return myId2Button.get(id);
  }

  @RequiredUIAccess
  @Override
  public void addButton(ToolWindowStripeButton button, @Nonnull WindowInfo info, @Nonnull Comparator<ToolWindowStripeButton> comparator) {
    final WindowInfoImpl copiedInfo = ((WindowInfoImpl)info).copy();
    myId2Button.put(copiedInfo.getId(), (WebToolWindowStripeButtonImpl)button);
    new AddToolStripeButtonCmd((WebToolWindowStripeButtonImpl)button, copiedInfo, comparator).run();
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public void removeButton(@Nonnull String id) {
    // todo
  }

  @RequiredUIAccess
  @Override
  public void removeDecorator(@Nonnull String id, boolean dirtyMode) {
    final ToolWindowInternalDecorator decorator = getDecoratorById(id);
    final WindowInfoImpl info = getDecoratorInfoById(id);

    myDecorator2Info.remove(decorator);
    myId2Decorator.remove(id);

    WindowInfoImpl sideInfo = getDockedInfoAt(info.getAnchor(), !info.isSplit());

    if (info.isDocked()) {
      if (sideInfo == null) {
        new RemoveDockedComponentCmd(info, dirtyMode).run();
      }
      else {

        //return new RemoveSplitAndDockedComponentCmd(info, dirtyMode, finishCallBack);
      }
    }
    else if (info.isSliding()) {
      //return new RemoveSlidingComponentCmd(decorator, info, dirtyMode, finishCallBack);
    }
    else {
      throw new IllegalArgumentException("Unknown window type");
    }
  }

  private WindowInfoImpl getDecoratorInfoById(final String id) {
    return myDecorator2Info.get(myId2Decorator.get(id));
  }

  private ToolWindowInternalDecorator getDecoratorById(final String id) {
    return myId2Decorator.get(id);
  }

  @RequiredUIAccess
  @Override
  public void addDecorator(@Nonnull ToolWindowInternalDecorator decorator, @Nonnull WindowInfo info, boolean dirtyMode) {
    final WindowInfoImpl copiedInfo = ((WindowInfoImpl)info).copy();
    final String id = copiedInfo.getId();

    myDecorator2Info.put(decorator, copiedInfo);
    myId2Decorator.put(id, decorator);

    if (info.isDocked()) {
      WindowInfoImpl sideInfo = getDockedInfoAt(info.getAnchor(), !info.isSplit());
      if (sideInfo == null) {
        new AddDockedComponentCmd(decorator, (WindowInfoImpl)info, dirtyMode).run();
      }
      else {
        //return new AddAndSplitDockedComponentCmd((DesktopInternalDecorator)decorator, info, dirtyMode, finishCallBack);
      }
    }
    else if (info.isSliding()) {

      //return new AddSlidingComponentCmd((DesktopInternalDecorator)decorator, info, dirtyMode, finishCallBack);
    }
    else {
      throw new IllegalArgumentException("Unknown window type: " + info.getType());
    }
  }

  private WindowInfoImpl getDockedInfoAt(@Nonnull ToolWindowAnchor anchor, boolean side) {
    for (WindowInfoImpl info : myDecorator2Info.values()) {
      if (info.isVisible() && info.isDocked() && info.getAnchor() == anchor && side == info.isSplit()) {
        return info;
      }
    }

    return null;
  }

  @RequiredUIAccess
  @Override
  public void updateButtonPosition(@Nonnull String id) {
    new UpdateButtonPositionCmd(id).run();
  }

  @RequiredUIAccess
  @Override
  public void setEditorComponent(Object component) {
    new SetEditorComponentCmd((Component)component).run();
  }
}
