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
package consulo.ui.ex;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.WindowInfo;
import com.intellij.openapi.wm.impl.WindowInfoImpl;
import com.intellij.openapi.wm.impl.commands.FinalizableCommand;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Component;
import consulo.ui.Label;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import consulo.ui.internal.VaadinWrapper;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;
import consulo.web.wm.impl.WebToolWindowInternalDecorator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WGwtToolWindowPanel extends AbstractComponentContainer implements consulo.ui.Component, VaadinWrapper, ToolWindowPanel {
  private static final Logger LOGGER = Logger.getInstance(WGwtToolWindowPanel.class);

  private final class AddToolStripeButtonCmd extends FinalizableCommand {
    private final WGwtToolWindowStripeButton myButton;
    private final WindowInfoImpl myInfo;
    private final Comparator<ToolWindowStripeButton> myComparator;

    public AddToolStripeButtonCmd(final WGwtToolWindowStripeButton button, @NotNull WindowInfoImpl info, @NotNull Comparator<ToolWindowStripeButton> comparator, @NotNull Runnable finishCallBack) {
      super(finishCallBack);
      myButton = button;
      myInfo = info;
      myComparator = comparator;
    }

    @Override
    public final void run() {
      try {
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
          LOGGER.error("unknown anchor: " + anchor);
        }
        markAsDirtyRecursive();
      }
      finally {
        finish();
      }
    }
  }

  private final class UpdateButtonPositionCmd extends FinalizableCommand {
    private final String myId;

    private UpdateButtonPositionCmd(@NotNull String id, @NotNull Runnable finishCallBack) {
      super(finishCallBack);
      myId = id;
    }

    @Override
    public void run() {
      try {
        WGwtToolWindowStripeButton stripeButton = getButtonById(myId);
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
          LOGGER.error("unknown anchor: " + anchor);
        }
      }
      finally {
        finish();
      }
    }
  }

  private final class AddDockedComponentCmd extends FinalizableCommand {
    private final ToolWindowInternalDecorator myDecorator;
    private final WindowInfoImpl myInfo;
    private final boolean myDirtyMode;

    public AddDockedComponentCmd(@NotNull ToolWindowInternalDecorator decorator, @NotNull WindowInfoImpl info, final boolean dirtyMode, @NotNull Runnable finishCallBack) {
      super(finishCallBack);
      myDecorator = decorator;
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public final void run() {
      try {
        final ToolWindowAnchor anchor = myInfo.getAnchor();

        setComponent(myDecorator, anchor, WindowInfoImpl.normalizeWeigh(myInfo.getWeight()));
      }
      finally {
        finish();
      }
    }
  }

  private final class RemoveDockedComponentCmd extends FinalizableCommand {
    private final WindowInfoImpl myInfo;
    private final boolean myDirtyMode;

    public RemoveDockedComponentCmd(@NotNull WindowInfoImpl info, final boolean dirtyMode, @NotNull Runnable finishCallBack) {
      super(finishCallBack);
      myInfo = info;
      myDirtyMode = dirtyMode;
    }

    @Override
    public final void run() {
      try {
        setComponent(null, myInfo.getAnchor(), 0);
      }
      finally {
        finish();
      }
    }
  }

  private WGwtToolWindowStripe myTopStripe = new WGwtToolWindowStripe(DockLayoutState.Constraint.TOP);
  private WGwtToolWindowStripe myBottomStripe = new WGwtToolWindowStripe(DockLayoutState.Constraint.BOTTOM);
  private WGwtToolWindowStripe myLeftStripe = new WGwtToolWindowStripe(DockLayoutState.Constraint.LEFT);
  private WGwtToolWindowStripe myRightStripe = new WGwtToolWindowStripe(DockLayoutState.Constraint.RIGHT);

  private final Map<String, WGwtToolWindowStripeButton> myId2Button = new HashMap<>();
  private final HashMap<String, ToolWindowInternalDecorator> myId2Decorator = new HashMap<>();
  private final HashMap<ToolWindowInternalDecorator, WindowInfoImpl> myDecorator2Info = new HashMap<>();
  private final HashMap<WGwtToolWindowStripeButton, WindowInfoImpl> myButton2Info = new HashMap<>();

  private final List<Component> myChildren = new ArrayList<>();

  private WGwtThreeComponentSplitLayout myHorizontalSplitter = new WGwtThreeComponentSplitLayout();

  public WGwtToolWindowPanel() {
    add(myTopStripe);
    add(myBottomStripe);
    add(myLeftStripe);
    add(myRightStripe);

    myHorizontalSplitter.setCenterComponent(Label.create("Test"));

    add(myHorizontalSplitter);
  }

  private void add(Component component) {
    addComponent(component);
    myChildren.add(component);
  }

  private void setComponent(@Nullable ToolWindowInternalDecorator d, @NotNull ToolWindowAnchor anchor, final float weight) {
    WebToolWindowInternalDecorator decorator = (WebToolWindowInternalDecorator)d;

    consulo.ui.Component component = decorator == null ? null : decorator.getComponent();

    if (ToolWindowAnchor.TOP == anchor) {
      //myVerticalSplitter.setFirstComponent(component);
      //myVerticalSplitter.setFirstSize((int)(myLayeredPane.getHeight() * weight));
    }
    else if (ToolWindowAnchor.LEFT == anchor) {
      myHorizontalSplitter.setLeftComponent(component);
      //myHorizontalSplitter.setFirstSize((int)(myLayeredPane.getWidth() * weight));
    }
    else if (ToolWindowAnchor.BOTTOM == anchor) {
      //myVerticalSplitter.setLastComponent(component);
      //myVerticalSplitter.setLastSize((int)(myLayeredPane.getHeight() * weight));
    }
    else if (ToolWindowAnchor.RIGHT == anchor) {
      myHorizontalSplitter.setRightComponent(component);
      //myHorizontalSplitter.setLastSize((int)(myLayeredPane.getWidth() * weight));
    }
    else {
      //LOG.error("unknown anchor: " + anchor);
    }
  }

  @Nullable
  private WGwtToolWindowStripeButton getButtonById(final String id) {
    return myId2Button.get(id);
  }

  @Override
  public void replaceComponent(Component oldComponent, Component newComponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getComponentCount() {
    return myChildren.size();
  }

  @Override
  public Iterator<Component> iterator() {
    return myChildren.iterator();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {
  }

  @NotNull
  @Override
  public FinalizableCommand createAddButtonCmd(ToolWindowStripeButton button, @NotNull WindowInfoImpl info, @NotNull Comparator<ToolWindowStripeButton> comparator, @NotNull Runnable finishCallBack) {
    final WindowInfoImpl copiedInfo = info.copy();
    myId2Button.put(copiedInfo.getId(), (WGwtToolWindowStripeButton)button);
    return new AddToolStripeButtonCmd((WGwtToolWindowStripeButton)button, copiedInfo, comparator, finishCallBack);
  }

  @NotNull
  @Override
  public FinalizableCommand createRemoveButtonCmd(@NotNull String id, @NotNull Runnable finishCallBack) {
    return new FinalizableCommand(finishCallBack) {
      @Override
      public void run() {

      }
    };
  }

  @NotNull
  @Override
  public FinalizableCommand createRemoveDecoratorCmd(@NotNull String id, boolean dirtyMode, @NotNull Runnable finishCallBack) {
    final ToolWindowInternalDecorator decorator = getDecoratorById(id);
    final WindowInfoImpl info = getDecoratorInfoById(id);

    myDecorator2Info.remove(decorator);
    myId2Decorator.remove(id);

    WindowInfoImpl sideInfo = getDockedInfoAt(info.getAnchor(), !info.isSplit());

    if (info.isDocked()) {
      if (sideInfo == null) {
        return new RemoveDockedComponentCmd(info, dirtyMode, finishCallBack);
      }
      else {
        return new FinalizableCommand(finishCallBack) {
          @Override
          public void run() {

          }
        };
        //return new RemoveSplitAndDockedComponentCmd(info, dirtyMode, finishCallBack);
      }
    }
    else if (info.isSliding()) {
      return new FinalizableCommand(finishCallBack) {
        @Override
        public void run() {

        }
      };
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

  @NotNull
  @Override
  public FinalizableCommand createAddDecoratorCmd(@NotNull ToolWindowInternalDecorator decorator, @NotNull WindowInfoImpl info, boolean dirtyMode, @NotNull Runnable finishCallBack) {
    final WindowInfoImpl copiedInfo = info.copy();
    final String id = copiedInfo.getId();

    myDecorator2Info.put(decorator, copiedInfo);
    myId2Decorator.put(id, decorator);

    if (info.isDocked()) {
      WindowInfoImpl sideInfo = getDockedInfoAt(info.getAnchor(), !info.isSplit());
      if (sideInfo == null) {
        return new AddDockedComponentCmd(decorator, info, dirtyMode, finishCallBack);
      }
      else {
        //return new AddAndSplitDockedComponentCmd((DesktopInternalDecorator)decorator, info, dirtyMode, finishCallBack);
        return new FinalizableCommand(finishCallBack) {
          @Override
          public void run() {

          }
        };
      }
    }
    else if (info.isSliding()) {
      return new FinalizableCommand(finishCallBack) {
        @Override
        public void run() {

        }
      };
      //return new AddSlidingComponentCmd((DesktopInternalDecorator)decorator, info, dirtyMode, finishCallBack);
    }
    else {
      throw new IllegalArgumentException("Unknown window type: " + info.getType());
    }
  }

  private WindowInfoImpl getDockedInfoAt(@NotNull ToolWindowAnchor anchor, boolean side) {
    for (WindowInfoImpl info : myDecorator2Info.values()) {
      if (info.isVisible() && info.isDocked() && info.getAnchor() == anchor && side == info.isSplit()) {
        return info;
      }
    }

    return null;
  }

  @NotNull
  @Override
  public FinalizableCommand createUpdateButtonPositionCmd(@NotNull String id, @NotNull Runnable finishCallback) {
    return new UpdateButtonPositionCmd(id, finishCallback);
  }
}
