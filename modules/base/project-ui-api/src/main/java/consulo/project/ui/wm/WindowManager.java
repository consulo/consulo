/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.project.ui.wm;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.project.ui.wm.event.WindowManagerListener;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class WindowManager {
  /**
   * @return <code>true</code> is and only if current OS supports alpha mode for windows and
   * all native libraries were successfully loaded.
   */
  public abstract boolean isAlphaModeSupported();

  /**
   * Sets alpha (transparency) ratio for the specified <code>window</code>.
   * If alpha mode isn't supported by underlying windowing system then the method does nothing.
   * The method also does nothing if alpha mode isn't enabled for the specified <code>window</code>.
   *
   * @param window <code>window</code> which transparency should be changed.
   * @param ratio  ratio of transparency. <code>0</code> means absolutely non transparent window.
   *               <code>1</code> means absolutely transparent window.
   * @throws IllegalArgumentException if <code>window</code> is not displayable or not showing,
   *                                  or if <code>ration</code> isn't in <code>[0..1]</code> range.
   */
  public abstract void setAlphaModeRatio(Window window, float ratio);

  /**
   * @return <code>true</code> if specified <code>window</code> is currently is alpha mode.
   */
  public abstract boolean isAlphaModeEnabled(Window window);

  /**
   * Sets whether the alpha (transparent) mode is enabled for specified <code>window</code>.
   * If alpha mode isn't supported by underlying windowing system then the method does nothing.
   *
   * @param window window which mode to be set.
   * @param state  determines the new alpha mode.
   */
  public abstract void setAlphaModeEnabled(Window window, boolean state);

  public static WindowManager getInstance() {
    return ApplicationManager.getApplication().getComponent(WindowManager.class);
  }

  @Deprecated
  @DeprecationInfo("Desktop only")
  public void doNotSuggestAsParent(consulo.ui.Window window) {
  }

  /**
   * Gets first window (starting from the active one) that can be parent for other windows.
   * Note, that this method returns only subclasses of dialog or frame.
   *
   * @return <code>null</code> if there is no currently active window or there are any window
   * that can be parent.
   */
  @Nullable
  public abstract consulo.ui.Window suggestParentWindow(@Nullable Project project);

  /**
   * Get the status bar for the project's main frame
   */
  @Nullable
  public abstract StatusBar getStatusBar(Project project);

  /**
   * Get the status bar for the component, it may be either the main status bar or the status bar for an undocked window
   *
   * @param c a component
   * @return status bar
   * @deprecated use getStatusBar(Component, Project)
   */
  public abstract StatusBar getStatusBar(@Nonnull Component c);

  public StatusBar getStatusBar(@Nonnull Component c, @Nullable Project project) {
    return null;
  }

  public StatusBar getStatusBar(@Nonnull consulo.ui.Component c, @Nullable Project project) {
    return null;
  }

  @Deprecated
  @Nullable
  public JFrame getFrame(@Nullable Project project) {
    consulo.ui.Window window = getWindow(project);
    return (JFrame)TargetAWT.to(window);
  }

  public abstract IdeFrame getIdeFrame(@Nullable Project project);

  @Nullable
  public consulo.ui.Window getWindow(@Nullable Project project) {
    IdeFrame ideFrame = getIdeFrame(project);
    return ideFrame != null ? ideFrame.getWindow() : null;
  }

  /**
   * Tests whether the specified rectangle is inside of screen bounds. Method uses its own heuristic test.
   * Test passes if intersection of screen bounds and specified rectangle isn't empty and its height and
   * width are not less then some value. Note, that all parameters are in screen coordinate system.
   * The method properly works in multi-monitor configuration.
   */
  public abstract boolean isInsideScreenBounds(int x, int y, int width);

  /**
   * Tests whether the specified point is inside of screen bounds. Note, that
   * all parameters are in screen coordinate system.
   * The method properly works in multi-monitor configuration.
   */
  public abstract boolean isInsideScreenBounds(int x, int y);

  @Nonnull
  public abstract IdeFrame[] getAllProjectFrames();

  @Nullable
  @Deprecated
  public JFrame findVisibleFrame() {
    return (JFrame)TargetAWT.to(findVisibleWindow());
  }

  @Nullable
  public consulo.ui.Window findVisibleWindow() {
    IdeFrame frame = findVisibleIdeFrame();
    return frame == null ? null : frame.getWindow();
  }

  @Nullable
  public abstract IdeFrame findVisibleIdeFrame();

  public abstract void addListener(WindowManagerListener listener);

  public abstract void removeListener(WindowManagerListener listener);

  /**
   * @return <code>true</code> if full screen mode is supported in current OS.
   */
  public abstract boolean isFullScreenSupportedInCurrentOS();

  public abstract void requestUserAttention(@Nonnull IdeFrame frame, boolean critical);

  @Nullable
  public abstract consulo.ui.Window getMostRecentFocusedWindow();


  /**
   * @return focus owner of the specified window.
   * @throws IllegalArgumentException if <code>window</code> is <code>null</code>.
   */
  public abstract Component getFocusedComponent(@Nonnull Window window);

  /**
   * @param project may be <code>null</code> when no project is opened.
   * @return focused component for the project. If project isn't specified then
   * the method returns focused component in window which has no project.
   * If there is no focused component at all then the method returns <code>null</code>.
   */
  @Nullable
  public abstract Component getFocusedComponent(@Nullable Project project);
}
