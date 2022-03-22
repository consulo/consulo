/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.actionSystem.impl;

import consulo.dataContext.DataManager;
import consulo.application.ui.UISettings;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.dataContext.DataContext;
import consulo.application.Application;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import consulo.application.ApplicationManager;
import consulo.application.impl.internal.LaterInvocator;
import consulo.ui.ex.awt.JBPopupMenu;
import com.intellij.openapi.util.Getter;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.component.messagebus.MessageBusConnection;
import consulo.ui.ex.awt.UIUtil;
import consulo.annotation.DeprecationInfo;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.toolWindow.ToolWindowInternalDecorator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.util.function.Supplier;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@Deprecated
@DeprecationInfo("desktop only")
public final class DesktopActionPopupMenuImpl implements ApplicationActivationListener, ActionPopupMenu {

  private final MyMenu myMenu;
  private final ActionManagerImpl myManager;
  private MessageBusConnection myConnection;

  private final Application myApp;
  private IdeFrame myFrame;
  @Nullable
  private Supplier<DataContext> myDataContextProvider;
  private boolean myIsToolWindowContextMenu;

  public DesktopActionPopupMenuImpl(String place, @Nonnull ActionGroup group, ActionManagerImpl actionManager, @Nullable PresentationFactory factory) {
    myManager = actionManager;
    myMenu = new MyMenu(place, group, factory);
    myApp = ApplicationManager.getApplication();
  }

  @Override
  public void show(consulo.ui.Component component, int x, int y) {
    myMenu.show(TargetAWT.to(component), x, y);
  }

  @Nonnull
  @Override
  public JPopupMenu getComponent() {
    return myMenu;
  }

  @Override
  @Nonnull
  public String getPlace() {
    return myMenu.myPlace;
  }

  @Nonnull
  @Override
  public ActionGroup getActionGroup() {
    return myMenu.myGroup;
  }

  @Override
  public void setTargetComponent(@Nonnull consulo.ui.Component component) {
    setTargetComponent((JComponent)TargetAWT.to(component));
  }

  @Override
  public void setTargetComponent(@Nonnull JComponent component) {
    myDataContextProvider = () -> DataManager.getInstance().getDataContext(component);
    myIsToolWindowContextMenu = ComponentUtil.getParentOfType(ToolWindowInternalDecorator.class, (Component)component) != null;
  }

  boolean isToolWindowContextMenu() {
    return myIsToolWindowContextMenu;
  }

  public void setDataContextProvider(@Nullable Getter<DataContext> dataContextProvider) {
    myDataContextProvider = dataContextProvider;
  }

  private class MyMenu extends JBPopupMenu {
    private final String myPlace;
    private final ActionGroup myGroup;
    private DataContext myContext;
    private final PresentationFactory myPresentationFactory;

    public MyMenu(String place, @Nonnull ActionGroup group, @Nullable PresentationFactory factory) {
      myPlace = place;
      myGroup = group;
      myPresentationFactory = factory != null ? factory : new MenuItemPresentationFactory();
      addPopupMenuListener(new MyPopupMenuListener());
    }

    @Override
    public void show(final Component component, int x, int y) {
      if (!component.isShowing()) {
        //noinspection HardCodedStringLiteral
        throw new IllegalArgumentException("component must be shown on the screen");
      }

      removeAll();

      // Fill menu. Only after filling menu has non zero size.

      int x2 = Math.max(0, Math.min(x, component.getWidth() - 1)); // fit x into [0, width-1]
      int y2 = Math.max(0, Math.min(y, component.getHeight() - 1)); // fit y into [0, height-1]

      myContext = myDataContextProvider != null ? myDataContextProvider.get() : DataManager.getInstance().getDataContext(component, x2, y2);
      Utils.fillMenu(myGroup, this, true, myPresentationFactory, myContext, myPlace, false, false, LaterInvocator.isInModalContext());
      if (getComponentCount() == 0) {
        return;
      }
      if (myApp != null) {
        if (myApp.isActive()) {
          Component frame = UIUtil.findUltimateParent(component);
          if (frame instanceof Window) {
            consulo.ui.Window uiWindow = TargetAWT.from((Window)frame);
            myFrame = uiWindow.getUserData(IdeFrame.KEY);
          }
          myConnection = myApp.getMessageBus().connect();
          myConnection.subscribe(ApplicationActivationListener.TOPIC, DesktopActionPopupMenuImpl.this);
        }
      }

      super.show(component, x, y);
    }

    @Override
    public void setVisible(boolean b) {
      super.setVisible(b);
      if (!b) setInvoker(null);
    }

    private class MyPopupMenuListener implements PopupMenuListener {
      @Override
      public void popupMenuCanceled(PopupMenuEvent e) {
        disposeMenu();
      }

      @Override
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        disposeMenu();
      }

      private void disposeMenu() {
        myManager.removeActionPopup(DesktopActionPopupMenuImpl.this);
        removeAll();
        if (myConnection != null) {
          myConnection.disconnect();
        }
      }

      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        removeAll();
        Utils.fillMenu(myGroup, MyMenu.this, !UISettings.getInstance().getDisableMnemonics(), myPresentationFactory, myContext, myPlace, false, LaterInvocator.isInModalContext(), false);
        myManager.addActionPopup(DesktopActionPopupMenuImpl.this);
      }
    }
  }

  @Override
  public void applicationDeactivated(IdeFrame ideFrame) {
    if (myFrame == ideFrame) {
      myMenu.setVisible(false);
    }
  }
}
