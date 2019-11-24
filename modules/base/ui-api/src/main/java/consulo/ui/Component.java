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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.KeyListener;
import consulo.ui.shared.Size;
import consulo.ui.shared.border.BorderPosition;
import consulo.ui.shared.border.BorderStyle;
import consulo.ui.style.ColorKey;
import consulo.ui.style.ComponentColors;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EventListener;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public interface Component extends Disposable, UserDataHolder {

  @RequiredUIAccess
  default void addBorder(@Nonnull BorderPosition borderPosition) {
    addBorder(borderPosition, BorderStyle.LINE, ComponentColors.BORDER, 1);
  }

  @RequiredUIAccess
  default void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle) {
    addBorder(borderPosition, borderStyle, ComponentColors.BORDER, 1);
  }

  @RequiredUIAccess
  default void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, @Nullable ColorKey colorKey) {
    addBorder(borderPosition, borderStyle, colorKey, 1);
  }

  @RequiredUIAccess
  default void addBorders(@Nonnull BorderStyle borderStyle, @Nullable ColorKey colorKey, @Nonnegative int width) {
    for (BorderPosition position : BorderPosition.values()) {
      addBorder(position, borderStyle, colorKey, width);
    }
  }

  @RequiredUIAccess
  default void addDefaultBorders() {
    for (BorderPosition position : BorderPosition.values()) {
      addBorder(position);
    }
  }

  @RequiredUIAccess
  void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, @Nullable ColorKey colorKey, @Nonnegative int width);

  @RequiredUIAccess
  default void removeBorders() {
    for (BorderPosition position : BorderPosition.values()) {
      removeBorder(position);
    }
  }

  @RequiredUIAccess
  void removeBorder(@Nonnull BorderPosition borderPosition);

  boolean isVisible();

  @RequiredUIAccess
  void setVisible(boolean value);

  boolean isEnabled();

  @RequiredUIAccess
  void setEnabled(boolean value);

  @Nullable
  Component getParent();

  @RequiredUIAccess
  void setSize(@Nonnull Size size);

  @Nonnull
  default <T> Disposable addUserDataProvider(@Nonnull Key<T> key, @Nonnull Supplier<T> supplier) {
    return addUserDataProvider(k -> k == key ? supplier.get() : null);
  }

  @Nonnull
  Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function);

  /**
   * @return runner for unregister listener
   */
  @Nonnull
  <T extends EventListener> Disposable addListener(@Nonnull Class<T> eventClass, @RequiredUIAccess @Nonnull T listener);

  @Nonnull
  <T extends EventListener> T getListenerDispatcher(@Nonnull Class<T> eventClass);

  default Disposable addKeyListener(@Nonnull KeyListener keyListener) {
    return addListener(KeyListener.class, keyListener);
  }

  @Override
  default void dispose() {
  }
}
