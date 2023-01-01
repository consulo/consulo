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

package consulo.project.ui.wm;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.dumb.DumbAware;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * Performs lazy initialization of a toolwindow.
 * Please implement {@link DumbAware} marker interface to indicate that the toolwindow content should be
 * available during indexing process.
 *
 * @author yole
 * @author Konstantin Bulenkov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ToolWindowFactory {
  @Nonnull
  String getId();

  @RequiredUIAccess
  void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow);

  @Nonnull
  ToolWindowAnchor getAnchor();

  @Nonnull
  Image getIcon();

  @Nonnull
  LocalizeValue getDisplayName();

  default boolean isSecondary() {
    return false;
  }

  default boolean canCloseContents() {
    return false;
  }

  /**
   * Perform additional initialisation routine here
   *
   * @param window Tool Window
   */
  default void init(ToolWindow window) {
  }

  /**
   * Tool Window saves its state on project close and restore on when project opens
   * In some cases, it is useful to postpone Tool Window activation until user explicitly activates it.
   * Example: Tool Window initialisation takes huge amount of time and makes project loading slower.
   *
   * @return {@code true} if Tool Window should not be activated on start even if was opened previously.
   * {@code false} otherwise.
   */
  default boolean isDoNotActivateOnStart() {
    return false;
  }

  /**
   * Check if toolwindow (and its stripe button) should be visible after startup.
   *
   * @see ToolWindow#isAvailable()
   */
  default boolean shouldBeAvailable(@Nonnull Project project) {
    return true;
  }

  default boolean isUnified() {
    return false;
  }

  /**
   * If return false - toolwindow will be unregistered
   */
  default boolean validate(@Nonnull Project project) {
    return true;
  }
}
