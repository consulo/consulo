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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.BusyObject;
import com.intellij.ui.content.ContentManager;
import consulo.annotation.DeprecationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ToolWindow extends BusyObject {
  Key<Boolean> SHOW_CONTENT_ICON = Key.create("ContentIcon");

  @Nonnull
  String getId();

  @Nonnull
  LocalizeValue getDisplayName();

  void setDisplayName(@Nonnull LocalizeValue displayName);

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
   * @return whether the tool window is visible or not. if not registered return false
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
  void setAnchor(@Nonnull ToolWindowAnchor anchor, @Nullable Runnable runnable);

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
  @Nonnull
  ToolWindowType getType();

  /**
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  void setType(@Nonnull ToolWindowType type, @Nullable Runnable runnable);

  /**
   * @return selected content title
   */
  @RequiredUIAccess
  @Deprecated
  String getTitle();

  /**
   * Sets new window title.
   */
  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Use getSelectedContent() for change")
  void setTitle(String title);

  /**
   * @return window stripe button text.
   */
  @Nonnull
  @RequiredUIAccess
  @Deprecated
  default String getStripeTitle() {
    return getDisplayName().getValue();
  }

  /**
   * Sets new window stripe button text.
   */
  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Use 'displayName' attribute")
  default void setStripeTitle(@Nonnull String title) {
    // unsupported
  }

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

  /**
   * Sets whether the tool window available or not. Term "available" means that tool window
   * can be shown and it has button on tool window bar.
   *
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  default void setAvailable(boolean available) {
    setAvailable(available, null);
  }

  @RequiredUIAccess
  void setContentUiType(@Nonnull ToolWindowContentUiType type, @Nullable Runnable runnable);

  void setDefaultContentUiType(@Nonnull ToolWindowContentUiType type);

  @Nonnull
  @RequiredUIAccess
  ToolWindowContentUiType getContentUiType();

  void installWatcher(@Nonnull ContentManager contentManager);

  @Nonnull
  ContentManager getContentManager();

  @Nullable
  default ContentManager getContentManagerIfCreated() {
    return getContentManager();
  }

  void setDefaultState(@Nullable ToolWindowAnchor anchor, @Nullable ToolWindowType type, @Nullable Rectangle2D floatingBounds);

  void setToHideOnEmptyContent(boolean hideOnEmpty);

  boolean isToHideOnEmptyContent();

  /**
   * @param show if <code>false</code> stripe button would be hidden
   */
  void setShowStripeButton(boolean show);

  boolean isShowStripeButton();

  boolean isDisposed();

  default void setTitleActions(@Nonnull AnAction... actions) {
  }

  default void setTabActions(@Nonnull AnAction... actions) {
  }

  default void setTabDoubleClickActions(@Nonnull AnAction... actions) {
  }

  @Nonnull
  default Component getUIComponent() {
    throw new AbstractMethodError();
  }

  @Nullable
  @RequiredUIAccess
  Image getIcon();

  /**
   * Sets new window icon.
   */
  @RequiredUIAccess
  void setIcon(@Nullable Image image);

  //TODO [VISTALL] awt & swing dependency

  // region AWT & Swing dependency

  @Nullable
  default javax.swing.JComponent getComponent() {
    throw new AbstractMethodError();
  }

  default void showContentPopup(java.awt.event.InputEvent inputEvent) {
  }
  //endregion
}
