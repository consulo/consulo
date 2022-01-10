/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.ex;

import consulo.disposer.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowInfo;
import com.intellij.openapi.wm.impl.InternalDecoratorListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public interface ToolWindowInternalDecorator extends Disposable {
  /**
   * Catches all event from tool window and modifies decorator's appearance.
   */
  String HIDE_ACTIVE_WINDOW_ACTION_ID = "HideActiveWindow";
  String TOGGLE_PINNED_MODE_ACTION_ID = "TogglePinnedMode";
  String TOGGLE_DOCK_MODE_ACTION_ID = "ToggleDockMode";
  String TOGGLE_FLOATING_MODE_ACTION_ID = "ToggleFloatingMode";
  String TOGGLE_WINDOWED_MODE_ACTION_ID = "ToggleWindowedMode";
  String TOGGLE_SIDE_MODE_ACTION_ID = "ToggleSideMode";
  String TOGGLE_CONTENT_UI_TYPE_ACTION_ID = "ToggleContentUiTypeMode";

  @Nonnull
  WindowInfo getWindowInfo();

  void apply(@Nonnull WindowInfo windowInfo);

  @Nonnull
  ToolWindow getToolWindow();

  void addInternalDecoratorListener(InternalDecoratorListener l);

  void removeInternalDecoratorListener(InternalDecoratorListener l);

  void fireActivated();

  void fireHidden();

  void fireHiddenSide();

  @Nonnull
  ActionGroup createPopupGroup();

  boolean isFocused();

  boolean hasFocus();

  default void setTitleActions(AnAction... actions) {
  }

  default void setTabActions(AnAction... actions) {
  }

  default void setAdditionalGearActions(@Nullable ActionGroup gearActions) {
  }
}
