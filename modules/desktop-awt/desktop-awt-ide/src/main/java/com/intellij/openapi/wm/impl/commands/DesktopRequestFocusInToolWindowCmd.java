/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.commands;

import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.FocusWatcher;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.impl.DesktopFloatingDecorator;
import com.intellij.openapi.wm.impl.DesktopToolWindowImpl;
import com.intellij.openapi.wm.impl.DesktopWindowManagerImpl;
import com.intellij.openapi.wm.impl.DesktopWindowWatcher;
import com.intellij.util.Alarm;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * Requests focus for the specified tool window.
 *
 * @author Vladimir Kondratyev
 */
public final class DesktopRequestFocusInToolWindowCmd {
  private static final Logger LOG = Logger.getInstance(DesktopRequestFocusInToolWindowCmd.class);
  private final IdeFocusManager myFocusManager;
  private final DesktopToolWindowImpl myToolWindow;
  private final FocusWatcher myFocusWatcher;

  private final Project myProject;

  public DesktopRequestFocusInToolWindowCmd(IdeFocusManager focusManager, final DesktopToolWindowImpl toolWindow, final FocusWatcher focusWatcher, Project project) {
    myFocusManager = focusManager;
    myToolWindow = toolWindow;
    myFocusWatcher = focusWatcher;
    myProject = project;
  }

  private void bringOwnerToFront() {
    final Window owner = SwingUtilities.getWindowAncestor(myToolWindow.getComponent());
    //Toolwindow component shouldn't take focus back if new dialog or frame appears
    //Example: Ctrl+D on file history brings a diff dialog to front and then hides it by main frame by calling
    // toFront on toolwindow window
    Window activeFrame = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    if (activeFrame != null && activeFrame != owner) return;
    //if (owner == null) {
    //  System.out.println("owner = " + owner);
    //  return;
    //}
    // if owner is active window or it has active child window which isn't floating decorator then
    // don't bring owner window to font. If we will make toFront every time then it's possible
    // the following situation:
    // 1. user perform refactoring
    // 2. "Do not show preview" dialog is popping up.
    // 3. At that time "preview" tool window is being activated and modal "don't show..." dialog
    // isn't active.
    if (owner != null && owner.getFocusOwner() == null) {
      final Window activeWindow = getActiveWindow(owner.getOwnedWindows());
      if (activeWindow == null || activeWindow instanceof DesktopFloatingDecorator) {
        LOG.debug("owner.toFront()");
        //Thread.dumpStack();
        //System.out.println("------------------------------------------------------");
        owner.toFront();
      }
    }
  }

  public void requestFocus() {
    final Alarm checkerAlarm = new Alarm();
    Runnable checker = new Runnable() {
      final long startTime = System.currentTimeMillis();

      @Override
      public void run() {
        if (System.currentTimeMillis() - startTime > 10000) {
          LOG.debug(myToolWindow.getId(), " tool window - cannot wait for showing component");
          return;
        }
        Component c = getShowingComponentToRequestFocus();
        if (c != null) {
          final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
          if (owner != c) {
            myFocusManager.requestFocusInProject(c, myProject);
            bringOwnerToFront();
          }
          myFocusManager.doWhenFocusSettlesDown(() -> updateToolWindow(c));
        }
        else {
          checkerAlarm.addRequest(this, 100);
        }
      }
    };
    checkerAlarm.addRequest(checker, 0);
  }


  @Nullable
  private Component getShowingComponentToRequestFocus() {
    Container container = myToolWindow.getComponent();
    if (container == null || !container.isShowing()) {
      LOG.debug(myToolWindow.getId(), " tool window - parent container is hidden: ", container);
      return null;
    }
    FocusTraversalPolicy policy = container.getFocusTraversalPolicy();
    if (policy == null) {
      LOG.warn(myToolWindow.getId() + " tool window does not provide focus traversal policy");
      return null;
    }
    Component component = policy.getDefaultComponent(container);
    if (component == null || !component.isShowing()) {
      LOG.debug(myToolWindow.getId(), " tool window - default component is hidden: ", container);
      return null;
    }
    return component;
  }

  private void updateToolWindow(Component c) {
    if (c.isFocusOwner()) {
      myFocusWatcher.setFocusedComponentImpl(c);
      if (myToolWindow.isAvailable() && !myToolWindow.isActive()) {
        myToolWindow.activate(null, true, false);
      }
    }

    updateFocusedComponentForWatcher(c);
  }

  private static void updateFocusedComponentForWatcher(final Component c) {
    final DesktopWindowWatcher watcher = ((DesktopWindowManagerImpl)WindowManager.getInstance()).getWindowWatcher();
    final FocusWatcher focusWatcher = watcher.getFocusWatcherFor(c);
    if (focusWatcher != null && c.isFocusOwner()) {
      focusWatcher.setFocusedComponentImpl(c);
    }
  }

  /**
   * @return first active window from hierarchy with specified roots. Returns <code>null</code>
   * if there is no active window in the hierarchy.
   */
  private static Window getActiveWindow(final Window[] windows) {
    for (Window window : windows) {
      if (window.isShowing() && window.isActive()) {
        return window;
      }
      window = getActiveWindow(window.getOwnedWindows());
      if (window != null) {
        return window;
      }
    }
    return null;
  }
}