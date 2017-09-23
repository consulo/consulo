/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui;

import com.intellij.openapi.Disposable;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.style.ColorKey;
import consulo.ui.style.ComponentColors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface Component extends Disposable {
  @RequiredUIAccess
  default void addBorder(@NotNull BorderPosition borderPosition) {
    addBorder(borderPosition, BorderStyle.LINE, ComponentColors.BORDER, 1);
  }

  @RequiredUIAccess
  default void addBorder(@NotNull BorderPosition borderPosition, @NotNull BorderStyle borderStyle) {
    addBorder(borderPosition, borderStyle, ComponentColors.BORDER, 1);
  }

  @RequiredUIAccess
  default void addBorder(@NotNull BorderPosition borderPosition, @NotNull BorderStyle borderStyle, ColorKey colorKey) {
    addBorder(borderPosition, borderStyle, colorKey, 1);
  }

  @RequiredUIAccess
  default void addDefaultBorders() {
    for (BorderPosition position : BorderPosition.values()) {
      addBorder(position);
    }
  }

  @RequiredUIAccess
  void addBorder(@NotNull BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width);

  @RequiredUIAccess
  void removeBorder(@NotNull BorderPosition borderPosition);

  boolean isVisible();

  @RequiredUIAccess
  void setVisible(boolean value);

  boolean isEnabled();

  @RequiredUIAccess
  void setEnabled(boolean value);

  @Nullable
  Component getParentComponent();

  @RequiredUIAccess
  void setSize(@NotNull Size size);

  /**
   * @return runner for unregister listener
   */
  @NotNull
  <T extends EventListener> Runnable addListener(@NotNull Class<T> eventClass, @RequiredUIAccess @NotNull T listener);

  @NotNull
  <T extends EventListener> T getListenerDispatcher(@NotNull Class<T> eventClass);

  @Override
  default void dispose() {
  }
}
