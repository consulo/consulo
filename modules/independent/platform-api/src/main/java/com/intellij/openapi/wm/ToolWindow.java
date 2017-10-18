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
package com.intellij.openapi.wm;

import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.BusyObject;
import com.intellij.openapi.util.Key;
import com.intellij.ui.content.ContentManager;
import consulo.annotations.DeprecationInfo;
import consulo.ui.Component;
import consulo.ui.Rectangle2D;
import consulo.ui.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ToolWindow extends BusyObject {
  Key<Boolean> SHOW_CONTENT_ICON = Key.create("ContentIcon");

  @NotNull
  String getId();

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  boolean isActive();

  /**
   * @param runnable A command to execute right after the window gets activated.  The call is asynchronous since it may require animation.
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  void activate(@Nullable Runnable runnable);

  @RequiredUIAccess
  void activate(@Nullable Runnable runnable, boolean autoFocusContents);

  @RequiredUIAccess
  void activate(@Nullable Runnable runnable, boolean autoFocusContents, boolean forced);

  /**
   * @return whether the tool window is visible or not.
   * @throws IllegalStateException if tool window isn't installed.
   */
  boolean isVisible();

  /**
   * @param runnable A command to execute right after the window shows up.  The call is asynchronous since it may require animation.
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  void show(@Nullable Runnable runnable);

  /**
   * Hides tool window. If the window is active then the method deactivates it.
   * Does nothing if tool window isn't visible.
   *
   * @param runnable A command to execute right after the window hides.  The call is asynchronous since it may require animation.
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  void hide(@Nullable Runnable runnable);

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  ToolWindowAnchor getAnchor();

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  void setAnchor(@NotNull ToolWindowAnchor anchor, @Nullable Runnable runnable);

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  boolean isSplitMode();

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  void setSplitMode(boolean isSideTool, @Nullable Runnable runnable);

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  boolean isAutoHide();

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  void setAutoHide(boolean state);

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  @NotNull
  ToolWindowType getType();

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  void setType(@NotNull ToolWindowType type, @Nullable Runnable runnable);

  /**
   * @return window title. Returns <code>null</code> if window has no title.
   */
  @RequiredUIAccess
  String getTitle();

  /**
   * Sets new window title.
   */
  @RequiredUIAccess
  void setTitle(String title);

  /**
   * @return window stripe button text.
   */
  @NotNull
  @RequiredUIAccess
  String getStripeTitle();

  /**
   * Sets new window stripe button text.
   */
  @RequiredUIAccess
  void setStripeTitle(@NotNull String title);

  /**
   * @return whether the window is available or not.
   */
  boolean isAvailable();

  /**
   * Sets whether the tool window available or not. Term "available" means that tool window
   * can be shown and it has button on tool window bar.
   *
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  void setAvailable(boolean available, @Nullable Runnable runnable);

  @RequiredUIAccess
  void setContentUiType(@NotNull ToolWindowContentUiType type, @Nullable Runnable runnable);

  void setDefaultContentUiType(@NotNull ToolWindowContentUiType type);

  @NotNull
  @RequiredUIAccess
  ToolWindowContentUiType getContentUiType();

  void installWatcher(@NotNull ContentManager contentManager);

  ContentManager getContentManager();

  void setDefaultState(@Nullable ToolWindowAnchor anchor, @Nullable ToolWindowType type, @Nullable Rectangle2D floatingBounds);

  void setToHideOnEmptyContent(boolean hideOnEmpty);

  boolean isToHideOnEmptyContent();

  /**
   * @param show if <code>false</code> stripe button would be hidden
   */
  void setShowStripeButton(boolean show);

  boolean isShowStripeButton();

  boolean isDisposed();

  ActionCallback getActivation();

  @NotNull
  default Component getUIComponent() {
    throw new AbstractMethodError();
  }

  @Nullable
  @RequiredUIAccess
  Image getUIIcon();

  @RequiredUIAccess
  void setUIIcon(@Nullable Image image);

  //TODO [VISTALL] awt & swing dependency

  // region AWT & Swing dependency

  /**
   * @return window icon. Returns <code>null</code> if window has no icon.
   */
  @Nullable
  @RequiredUIAccess
  @DeprecationInfo("Use #getIconUI()")
  javax.swing.Icon getIcon();

  /**
   * Sets new window icon.
   */
  @RequiredUIAccess
  @DeprecationInfo("Use #setIconUI()")
  void setIcon(@Nullable javax.swing.Icon icon);

  @Nullable
  default javax.swing.JComponent getComponent() {
    throw new AbstractMethodError();
  }

  default void showContentPopup(java.awt.event.InputEvent inputEvent) {
  }
  //endregion
}
