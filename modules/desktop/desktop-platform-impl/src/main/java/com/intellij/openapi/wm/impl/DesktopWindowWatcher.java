/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.FocusWatcher;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.util.containers.ContainerUtil;
import consulo.awt.TargetAWT;
import consulo.logging.Logger;
import consulo.wm.util.IdeFrameUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class DesktopWindowWatcher implements PropertyChangeListener {
  private static final Logger LOG = Logger.getInstance(DesktopWindowWatcher.class);

  @NonNls
  protected static final String FOCUSED_WINDOW_PROPERTY = "focusedWindow";

  private final Object myLock = new Object();

  private final Map<consulo.ui.Window, WindowInfo> myWindow2Info = ContainerUtil.createWeakMap();
  private final Application myApplication;
  /**
   * Currenly focused window (window which has focused component). Can be <code>null</code> if there is no focused
   * window at all.
   */
  private consulo.ui.Window myFocusedWindow;
  /**
   * Contains last focused window for each project.
   */
  private final Set<consulo.ui.Window> myFocusedWindows = new HashSet<>();

  public DesktopWindowWatcher(Application application) {
    myApplication = application;
  }

  /**
   * This method should get notifications abount changes of focused window.
   * Only <code>focusedWindow</code> property is acceptable.
   *
   * @throws IllegalArgumentException if property name isn't <code>focusedWindow</code>.
   */
  @Override
  public final void propertyChange(final PropertyChangeEvent e) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: propertyChange(" + e + ")");
    }
    if (!FOCUSED_WINDOW_PROPERTY.equals(e.getPropertyName())) {
      throw new IllegalArgumentException("unknown property name: " + e.getPropertyName());
    }
    synchronized (myLock) {
      final consulo.ui.Window window = TargetAWT.from((Window)e.getNewValue());
      if (window == null || myApplication.isDisposed()) {
        return;
      }
      if (!myWindow2Info.containsKey(window)) {
        myWindow2Info.put(window, new WindowInfo(window, true));
      }
      myFocusedWindow = window;
      final Project project = DataManager.getInstance().getDataContext(myFocusedWindow).getData(CommonDataKeys.PROJECT);
      for (Iterator<consulo.ui.Window> i = myFocusedWindows.iterator(); i.hasNext(); ) {
        final consulo.ui.Window w = i.next();
        final DataContext dataContext = DataManager.getInstance().getDataContext(TargetAWT.to(w));
        if (project == dataContext.getData(CommonDataKeys.PROJECT)) {
          i.remove();
        }
      }
      myFocusedWindows.add(myFocusedWindow);
      // Set new root frame
      final IdeFrame frame = IdeFrameUtil.findRootIdeFrame(window);

      if (frame != null) {
        JOptionPane.setRootFrame((Frame)TargetAWT.to(frame.getWindow()));
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("exit: propertyChange()");
    }
  }

  final void dispatchComponentEvent(final ComponentEvent e) {
    final int id = e.getID();
    if (WindowEvent.WINDOW_CLOSED == id || (ComponentEvent.COMPONENT_HIDDEN == id && e.getSource() instanceof Window)) {
      dispatchHiddenOrClosed(TargetAWT.from((Window)e.getSource()));
    }
    // Clear obsolete reference on root frame
    if (WindowEvent.WINDOW_CLOSED == id) {
      final Window window = (Window)e.getSource();

      if (JOptionPane.getRootFrame() == window) {
        JOptionPane.setRootFrame(null);
      }
    }
  }

  private void dispatchHiddenOrClosed(consulo.ui.Window window) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: dispatchClosed(" + window + ")");
    }
    synchronized (myLock) {
      final WindowInfo info = myWindow2Info.get(window);
      if (info != null) {
        final FocusWatcher focusWatcher = info.myFocusWatcherRef.get();
        if (focusWatcher != null) {
          focusWatcher.deinstall(TargetAWT.to(window));
        }
        myWindow2Info.remove(window);
      }
    }
    // Now, we have to recalculate focused window if currently focused
    // window is being closed.
    if (myFocusedWindow == window) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("currently active window should be closed");
      }
      myFocusedWindow = myFocusedWindow.getParent();
      if (LOG.isDebugEnabled()) {
        LOG.debug("new active window is " + myFocusedWindow);
      }
    }
    for (Iterator<consulo.ui.Window> i = myFocusedWindows.iterator(); i.hasNext(); ) {
      final consulo.ui.Window activeWindow = i.next();
      if (activeWindow == window) {
        final consulo.ui.Window newActiveWindow = activeWindow.getParent();
        i.remove();
        if (newActiveWindow != null) {
          myFocusedWindows.add(newActiveWindow);
        }
        break;
      }
    }
    // Remove invalid infos for garbage collected windows
    for (Iterator i = myWindow2Info.values().iterator(); i.hasNext(); ) {
      final WindowInfo info = (WindowInfo)i.next();
      if (info.myFocusWatcherRef.get() == null) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("remove collected info");
        }
        i.remove();
      }
    }
  }

  public final consulo.ui.Window getFocusedWindow() {
    synchronized (myLock) {
      return myFocusedWindow;
    }
  }

  @Nullable
  public final Component getFocusedComponent(@Nullable final Project project) {
    synchronized (myLock) {
      final Window window = getFocusedWindowForProject(project);
      if (window == null) {
        return null;
      }
      return getFocusedComponent(window);
    }
  }


  public final Component getFocusedComponent(@Nonnull final Window window) {
    synchronized (myLock) {
      final WindowInfo info = myWindow2Info.get(TargetAWT.from(window));
      if (info == null) { // it means that we don't manage this window, so just return standard focus owner
        // return window.getFocusOwner();
        // TODO[vova,anton] usage of getMostRecentFocusOwner is experimental. But it seems suitable here.
        return window.getMostRecentFocusOwner();
      }
      final FocusWatcher focusWatcher = info.myFocusWatcherRef.get();
      if (focusWatcher != null) {
        final Component focusedComponent = focusWatcher.getFocusedComponent();
        if (focusedComponent != null && focusedComponent.isShowing()) {
          return focusedComponent;
        }
        else {
          return null;
        }
      }
      else {
        // info isn't valid, i.e. window was garbage collected, so we need the remove invalid info
        // and return null
        myWindow2Info.remove(TargetAWT.from(window));
        return null;
      }
    }
  }

  @Nullable
  public FocusWatcher getFocusWatcherFor(Component c) {
    final Window window = SwingUtilities.getWindowAncestor(c);
    final WindowInfo info = myWindow2Info.get(TargetAWT.from(window));
    return info == null ? null : info.myFocusWatcherRef.get();
  }

  /**
   * @param project may be null (for example, if no projects are opened)
   */
  @Nullable
  public final consulo.ui.Window suggestParentWindow(@Nullable final Project project) {
    synchronized (myLock) {
      Window window = getFocusedWindowForProject(project);
      if (window == null) {
        if (project != null) {
          IdeFrame frameFor = WindowManagerEx.getInstanceEx().findFrameFor(project);
          return frameFor == null ? null : frameFor.getWindow();
        }
        else {
          return null;
        }
      }

      LOG.assertTrue(window.isDisplayable());
      LOG.assertTrue(window.isShowing());

      while (window != null) {
        // skip all windows until found forst dialog or frame
        if (!(window instanceof Dialog) && !(window instanceof Frame)) {
          window = window.getOwner();
          continue;
        }
        // skip not visible and disposed/not shown windows
        if (!window.isDisplayable() || !window.isShowing()) {
          window = window.getOwner();
          continue;
        }
        // skip windows that have not associated WindowInfo
        final WindowInfo info = myWindow2Info.get(TargetAWT.from(window));
        if (info == null) {
          window = window.getOwner();
          continue;
        }
        if (info.mySuggestAsParent) {
          return TargetAWT.from(window);
        }
        else {
          window = window.getOwner();
        }
      }
      return null;
    }
  }

  public final void doNotSuggestAsParent(final consulo.ui.Window window) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: doNotSuggestAsParent(" + window + ")");
    }
    synchronized (myLock) {
      final WindowInfo info = myWindow2Info.get(window);
      if (info == null) {
        myWindow2Info.put(window, new WindowInfo(window, false));
      }
      else {
        info.mySuggestAsParent = false;
      }
    }
  }

  /**
   * @return active window for specified <code>project</code>. There is only one window
   * for project can be at any point of time.
   */
  @Nullable
  private Window getFocusedWindowForProject(@Nullable final Project project) {
    //todo[anton,vova]: it is possible that returned wnd is not contained in myFocusedWindows; investigate
    outer:
    for (consulo.ui.Window window : myFocusedWindows) {
      Window awtWindow = TargetAWT.to(window);

      while (!awtWindow.isDisplayable() || !awtWindow.isShowing()) { // if window isn't visible then gets its first visible ancestor
        awtWindow = awtWindow.getOwner();
        if (awtWindow == null) {
          continue outer;
        }
      }
      final DataContext dataContext = DataManager.getInstance().getDataContext(awtWindow);
      if (project == dataContext.getData(CommonDataKeys.PROJECT)) {
        return awtWindow;
      }
    }
    return null;
  }

  private static final class WindowInfo {
    public final WeakReference<FocusWatcher> myFocusWatcherRef;
    public boolean mySuggestAsParent;

    public WindowInfo(final consulo.ui.Window window, final boolean suggestAsParent) {
      final FocusWatcher focusWatcher = new FocusWatcher();
      focusWatcher.install(TargetAWT.to(window));
      myFocusWatcherRef = new WeakReference<>(focusWatcher);
      mySuggestAsParent = suggestAsParent;
    }
  }
}
