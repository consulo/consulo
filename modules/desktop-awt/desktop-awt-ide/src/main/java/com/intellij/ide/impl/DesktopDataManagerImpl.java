/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.impl;

import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ProhibitAWTEvents;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.awt.impl.FromSwingWindowWrapper;
import consulo.ide.base.BaseDataManager;
import consulo.logging.Logger;
import consulo.ui.ex.ToolWindowFloatingDecorator;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

@Singleton
public class DesktopDataManagerImpl extends BaseDataManager {
  private static final Logger LOG = Logger.getInstance(DesktopDataManagerImpl.class);

  public static class MyDataContext extends BaseDataContext<DesktopDataManagerImpl, Component> implements DataContextWithEventCount {
    private int myEventCount;

    public MyDataContext(DesktopDataManagerImpl dataManager, Component component) {
      super(dataManager, component);
      myEventCount = -1;
    }

    @Override
    public void setEventCount(int eventCount, Object caller) {
      assert caller instanceof IdeKeyEventDispatcher : "This method might be accessible from " + IdeKeyEventDispatcher.class.getName() + " only";
      clearCacheData();
      myEventCount = eventCount;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getData(@Nonnull Key<T> dataId) {
      int currentEventCount = IdeEventQueue.getInstance().getEventCount();
      if (myEventCount != -1 && myEventCount != currentEventCount) {
        LOG.error("cannot share data context between Swing events; initial event count = " + myEventCount + "; current event count = " + currentEventCount);
        return doGetData(dataId);
      }

      return super.getData(dataId);
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    protected  <T> T doGetData(@Nonnull Key<T> dataId) {
      Component component = getComponent();
      if (PlatformDataKeys.IS_MODAL_CONTEXT == dataId) {
        if (component == null) {
          return null;
        }
        return (T)(Boolean)IdeKeyEventDispatcher.isModalContext(component);
      }
      if (PlatformDataKeys.CONTEXT_COMPONENT == dataId) {
        return (T)component;
      }
      if (PlatformDataKeys.MODALITY_STATE == dataId) {
        return (T)(component != null ? ModalityState.stateForComponent(component) : ModalityState.NON_MODAL);
      }

      Object data = calcData(dataId, component);
      if (CommonDataKeys.EDITOR == dataId || CommonDataKeys.HOST_EDITOR == dataId) {
        return (T)validateEditor((Editor)data);
      }
      return (T)data;
    }

    protected Object calcData(@Nonnull Key<?> dataId, Component component) {
      return getDataManager().getData(dataId, component);
    }
  }

  @Inject
  public DesktopDataManagerImpl(Provider<WindowManager> windowManagerProvider) {
    super(windowManagerProvider);
  }

  private WindowManagerEx windowManager() {
    return (WindowManagerEx)myWindowManager.get();
  }

  @Nullable
  private <T> T getData(@Nonnull Key<T> dataId, final Component focusedComponent) {
    try (AccessToken ignored = ProhibitAWTEvents.start("getData")) {
      for (Component c = focusedComponent; c != null; c = c.getParent()) {
        final DataProvider dataProvider = getDataProviderEx(c);
        if (dataProvider == null) continue;
        T data = getDataFromProvider(dataProvider, dataId, null);
        if (data != null) return data;
      }
    }
    return null;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public DataProvider getDataProviderEx(java.awt.Component component) {
    DataProvider dataProvider = null;
    if (component instanceof DataProvider) {
      dataProvider = (DataProvider)component;
    }
    else if (component instanceof TypeSafeDataProvider) {
      dataProvider = new TypeSafeDataProviderAdapter((TypeSafeDataProvider)component);
    }
    else if (component instanceof JComponent) {
      dataProvider = getDataProvider((JComponent)component);
    }
    // special case for desktop impl. Later removed since we don't want use AWT
    else if (component instanceof FromSwingComponentWrapper) {
      consulo.ui.Component uiComponent = ((FromSwingComponentWrapper)component).toUIComponent();
      return uiComponent::getUserData;
    }
    // special case for desktop impl. Later removed since we don't want use AWT
    else if (component instanceof FromSwingWindowWrapper) {
      consulo.ui.Window uiWindow = ((FromSwingWindowWrapper)component).toUIWindow();
      return uiWindow::getUserData;
    }

    return dataProvider;
  }

  @Nonnull
  @Override
  public AsyncDataContext createAsyncDataContext(@Nonnull DataContext dataContext) {
    return new DesktopAsyncDataContext(this, dataContext);
  }

  @Override
  public DataContext getDataContext(@Nullable Component component) {
    return new MyDataContext(this, component);
  }

  @Override
  public DataContext getDataContext(@Nonnull Component component, int x, int y) {
    if (x < 0 || x >= component.getWidth() || y < 0 || y >= component.getHeight()) {
      throw new IllegalArgumentException("wrong point: x=" + x + "; y=" + y);
    }

    // Point inside JTabbedPane has special meaning. If point is inside tab bounds then
    // we construct DataContext by the component which corresponds to the (x, y) tab.
    if (component instanceof JTabbedPane) {
      JTabbedPane tabbedPane = (JTabbedPane)component;
      int index = tabbedPane.getUI().tabForCoordinate(tabbedPane, x, y);
      return getDataContext(index != -1 ? tabbedPane.getComponentAt(index) : tabbedPane);
    }
    else {
      return getDataContext(component);
    }
  }

  @Override
  public DataContext getDataContextTest(Component component) {
    DataContext dataContext = getDataContext(component);

    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    Component focusedComponent = windowManager().getFocusedComponent(project);
    if (focusedComponent != null) {
      dataContext = getDataContext(focusedComponent);
    }
    return dataContext;
  }

  @Override
  @Nonnull
  public DataContext getDataContext() {
    return getDataContext(getFocusedComponent());
  }

  @Nullable
  private Component getFocusedComponent() {
    Window activeWindow = TargetAWT.to(windowManager().getMostRecentFocusedWindow());
    if (activeWindow == null) {
      activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
      if (activeWindow == null) {
        activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        if (activeWindow == null) return null;
      }
    }

    // In case we have an active floating toolwindow and some component in another window focused,
    // we want this other component to receive key events.
    // Walking up the window ownership hierarchy from the floating toolwindow would have led us to the main IdeFrame
    // whereas we want to be able to type in other frames as well.
    if (activeWindow instanceof ToolWindowFloatingDecorator) {
      IdeFocusManager ideFocusManager = IdeFocusManager.findInstanceByComponent(activeWindow);
      IdeFrame lastFocusedFrame = ideFocusManager.getLastFocusedFrame();
      JComponent frameComponent = lastFocusedFrame != null ? lastFocusedFrame.getComponent() : null;
      Window lastFocusedWindow = frameComponent != null ? SwingUtilities.getWindowAncestor(frameComponent) : null;
      boolean toolWindowIsNotFocused = windowManager().getFocusedComponent(activeWindow) == null;
      if (toolWindowIsNotFocused && lastFocusedWindow != null) {
        activeWindow = lastFocusedWindow;
      }
    }

    // try to find first parent window that has focus
    Window window = activeWindow;
    Component focusedComponent = null;
    while (window != null) {
      focusedComponent = windowManager().getFocusedComponent(window);
      if (focusedComponent != null) {
        break;
      }
      window = window.getOwner();
    }
    if (focusedComponent == null) {
      focusedComponent = activeWindow;
    }

    return focusedComponent;
  }

  @Override
  @Nullable
  // FIXME [VISTALL] hack until not all UI code will return consulo.ui.Component
  protected <T> T getData(@Nonnull Key<T> dataId, final consulo.ui.Component focusedComponent) {
    return getData(dataId, TargetAWT.to(focusedComponent));
  }

  @Nullable
  public static Editor validateEditor(Editor editor) {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (focusOwner instanceof JComponent) {
      final JComponent jComponent = (JComponent)focusOwner;
      if (jComponent.getClientProperty("AuxEditorComponent") != null) return null; // Hack for EditorSearchComponent
    }

    return editor;
  }
}
