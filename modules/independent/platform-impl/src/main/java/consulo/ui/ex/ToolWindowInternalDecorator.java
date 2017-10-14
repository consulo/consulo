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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowInfo;
import com.intellij.openapi.wm.impl.InternalDecoratorListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public interface ToolWindowInternalDecorator extends Disposable {
  @NotNull
  WindowInfo getWindowInfo();

  void apply(@NotNull WindowInfo windowInfo);

  @NotNull
  ToolWindow getToolWindow();

  void addInternalDecoratorListener(InternalDecoratorListener l);

  void removeInternalDecoratorListener(InternalDecoratorListener l);

  void fireActivated();

  void fireHidden();

  void fireHiddenSide();

  @NotNull
  ActionGroup createPopupGroup();

  boolean isFocused();

  boolean hasFocus();

  default void setTitleActions(AnAction... actions) {
  }

  default void setAdditionalGearActions(@Nullable ActionGroup gearActions) {
  }
}
