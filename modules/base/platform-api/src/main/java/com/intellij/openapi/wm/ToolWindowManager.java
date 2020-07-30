/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.wm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * If you want to register a toolwindow, which will be enabled during the dumb mode, please use {@link ToolWindowManager}'s
 * registration methods which have 'canWorkInDumMode' parameter.
 */
public abstract class ToolWindowManager {

  public abstract boolean canShowNotification(@Nonnull String toolWindowId);

  public static ToolWindowManager getInstance(@Nonnull Project project) {
    return project.getComponent(ToolWindowManager.class);
  }

  @Nonnull
  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Use extension ToolWindowFactory")
  public abstract ToolWindow registerToolWindow(@Nonnull String id, boolean canCloseContent, @Nonnull ToolWindowAnchor anchor);

  @Nonnull
  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Use extension ToolWindowFactory")
  public abstract ToolWindow registerToolWindow(@Nonnull String id, boolean canCloseContent, @Nonnull ToolWindowAnchor anchor, boolean secondary);

  @Nonnull
  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Use extension ToolWindowFactory")
  public abstract ToolWindow registerToolWindow(@Nonnull String id, boolean canCloseContent, @Nonnull ToolWindowAnchor anchor, Disposable parentDisposable, boolean canWorkInDumbMode);

  @Nonnull
  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Use extension ToolWindowFactory")
  public abstract ToolWindow registerToolWindow(@Nonnull String id,
                                                boolean canCloseContent,
                                                @Nonnull ToolWindowAnchor anchor,
                                                Disposable parentDisposable,
                                                boolean canWorkInDumbMode,
                                                boolean secondary);

  @Nonnull
  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Use extension ToolWindowFactory")
  public ToolWindow registerToolWindow(@Nonnull final String id, final boolean canCloseContent, @Nonnull final ToolWindowAnchor anchor, final Disposable parentDisposable) {
    return registerToolWindow(id, canCloseContent, anchor, parentDisposable, false);
  }

  /**
   * does nothing if tool window with specified isn't registered.
   */
  @RequiredUIAccess
  public abstract void unregisterToolWindow(@Nonnull String id);

  /**
   */
  public abstract void activateEditorComponent();

  /**
   * @return <code>true</code> if and only if editor component is active.
   */
  public abstract boolean isEditorComponentActive();

  /**
   * @return array of <code>id</code>s of all registered tool windows.
   */
  @Nonnull
  public abstract String[] getToolWindowIds();

  /**
   * @return <code>ID</code> of currently active tool window or <code>null</code> if there is no active
   * tool window.
   */
  @Nullable
  @RequiredUIAccess
  public abstract String getActiveToolWindowId();

  /**
   * @return registered tool window with specified <code>id</code>. If there is no registered
   * tool window with specified <code>id</code> then the method returns <code>null</code>.
   */
  public abstract ToolWindow getToolWindow(String id);

  /**
   * Puts specified runnable to the tail of current command queue.
   */
  public abstract void invokeLater(@Nonnull Runnable runnable);

  /**
   * Utility method for quick access to the focus manager
   */
  @Nonnull
  public abstract IdeFocusManager getFocusManager();

  public abstract void notifyByBalloon(@Nonnull final String toolWindowId, @Nonnull final MessageType type, @Nonnull final String htmlBody);

  @Nullable
  public abstract Balloon getToolWindowBalloon(String id);

  public abstract boolean isMaximized(@Nonnull ToolWindow wnd);

  public abstract void setMaximized(@Nonnull ToolWindow wnd, boolean maximized);

  @Nonnull
  public Image getLocationIcon(@Nonnull String toolWindowId, @Nonnull Image fallbackImage) {
    return fallbackImage;
  }

  // region AWT & Swing dependency

  /**
   * Register specified tool window into IDE window system.
   *
   * @param id        <code>id</code> of tool window to be registered.
   * @param component <code>component</code> which represents tool window content.
   *                  May be null. Content can be further added via content manager for this tool window (See {@link ToolWindow#getContentManager()})
   * @param anchor    the default anchor for first registration. It uses only first time the
   *                  tool window with the specified <code>id</code> is being registered into the window system.
   *                  After the first registration window's anchor is stored in project file
   *                  and <code>anchor</code> is ignored.
   * @return tool window
   * @throws IllegalArgumentException if the same window is already installed or one
   *                                  of the parameters is <code>null</code>.
   * @deprecated {@link ToolWindowManager#registerToolWindow(String, boolean, ToolWindowAnchor)}
   */
  @Deprecated
  @Nonnull
  public abstract ToolWindow registerToolWindow(@Nonnull String id, @Nonnull javax.swing.JComponent component, @Nonnull ToolWindowAnchor anchor);

  /**
   * @deprecated {@link ToolWindowManager#registerToolWindow(String, boolean, ToolWindowAnchor)}
   */
  @Deprecated
  @Nonnull
  @RequiredUIAccess
  public ToolWindow registerToolWindow(@Nonnull String id, @Nonnull javax.swing.JComponent component, @Nonnull ToolWindowAnchor anchor, @Nonnull Disposable parentDisposable) {
    return registerToolWindow(id, component, anchor, parentDisposable, false, false);
  }

  /**
   * @deprecated {@link ToolWindowManager#registerToolWindow(String, boolean, ToolWindowAnchor)}
   */
  @Deprecated
  @Nonnull
  @RequiredUIAccess
  public ToolWindow registerToolWindow(@Nonnull String id, @Nonnull javax.swing.JComponent component, @Nonnull ToolWindowAnchor anchor, Disposable parentDisposable, boolean canWorkInDumbMode) {
    return registerToolWindow(id, component, anchor, parentDisposable, canWorkInDumbMode, false);
  }

  /**
   * @deprecated {@link ToolWindowManager#registerToolWindow(String, boolean, ToolWindowAnchor)}
   */
  @Deprecated
  @Nonnull
  @RequiredUIAccess
  public abstract ToolWindow registerToolWindow(@Nonnull String id,
                                                @Nonnull javax.swing.JComponent component,
                                                @Nonnull ToolWindowAnchor anchor,
                                                Disposable parentDisposable,
                                                boolean canWorkInDumbMode,
                                                boolean canCloseContents);

  @Deprecated
  public void notifyByBalloon(@Nonnull final String toolWindowId,
                              @Nonnull final MessageType type,
                              @Nonnull final String htmlBody,
                              @Nullable final Image icon,
                              @Nullable javax.swing.event.HyperlinkListener listener) {
    throw new AbstractMethodError("AWT & Swing dependency");
  }

  // endregion
}
