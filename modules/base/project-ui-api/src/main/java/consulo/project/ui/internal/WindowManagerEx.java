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
package consulo.project.ui.internal;

import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameState;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.AppIcon;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public abstract class WindowManagerEx extends WindowManager {
  public static final String ID = "WindowManager";

  public enum WindowShadowMode {
    NORMAL,
    SMALL,
    DISABLED
  }

  public static WindowManagerEx getInstanceEx() {
    return (WindowManagerEx)WindowManager.getInstance();
  }

  @Override
  public abstract IdeFrameEx getIdeFrame(@Nullable Project project);

  @Override
  public void requestUserAttention(@Nonnull IdeFrame frame, boolean critical) {
    Project project = frame.getProject();
    if (project != null) AppIcon.getInstance().requestAttention(project, critical);
  }

  @Nonnull
  @RequiredUIAccess
  public abstract IdeFrameEx allocateFrame(@Nonnull Project project, @Nullable IdeFrameState state);

  public abstract void releaseFrame(IdeFrameEx frame);

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

  @Nullable
  public abstract consulo.ui.Window getMostRecentFocusedWindow();

  @RequiredUIAccess
  public abstract IdeFrame findFrameFor(@Nullable Project project);

  /**
   * @return default layout for tool windows.
   */
  public abstract ToolWindowLayout getLayout();

  /**
   * Copies <code>layout</code> into internal default layout.
   */
  public abstract void setLayout(ToolWindowLayout layout);

  /**
   * This method is invoked by <code>IdeEventQueue</code> to notify window manager that
   * some window activity happens. <u><b>Do not invoke it in other places!!!<b></u>
   */
  public abstract void dispatchComponentEvent(ComponentEvent e);

  /**
   * @return union of bounds of all default screen devices. Note that <code>x</code> and/or <code>y</code>
   * coordinates can be negative. It depends on physical configuration of graphics devices.
   * For example, the left monitor has negative coordinates on Win32 platform with dual monitor support
   * (right monitor is the primer one) .
   */
  public abstract Rectangle getScreenBounds();

  /**
   * @return bounds for the screen device for the given project frame
   */
  public abstract Rectangle getScreenBounds(@Nonnull final Project project);

  public abstract void setWindowMask(Window window, Shape mask);

  public abstract void setWindowShadow(Window window, WindowShadowMode mode);

  public abstract void resetWindow(final Window window);

  /**
   * Either dispose the dialog immediately if project's frame has focus or just hide and dispose when frame gets focus or closes.
   *
   * @param dialog  to hide and dispose later
   * @param project the dialog has been shown for
   */
  public abstract void hideDialog(JDialog dialog, Project project);

  public abstract void adjustContainerWindow(Component c, Dimension oldSize, Dimension newSize);

  public abstract void disposeRootFrame();

  public boolean isFloatingMenuBarSupported() {
    return false;
  }
}
