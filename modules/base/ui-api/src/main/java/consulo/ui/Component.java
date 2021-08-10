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

import consulo.annotation.ApiType;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;
import consulo.ui.event.AttachListener;
import consulo.ui.event.ClickListener;
import consulo.ui.event.DetachListener;
import consulo.ui.event.KeyListener;
import consulo.ui.font.Font;
import consulo.ui.style.ComponentColors;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;

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
@ApiType
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
  default void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, @Nullable ColorValue colorKey) {
    addBorder(borderPosition, borderStyle, colorKey, 1);
  }

  @RequiredUIAccess
  default void addBorders(@Nonnull BorderStyle borderStyle, @Nullable ColorValue colorKey, @Nonnegative int width) {
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
  default void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, @Nonnegative int width) {
    addBorder(borderPosition, borderStyle, null, width);
  }

  @RequiredUIAccess
  void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, @Nullable ColorValue colorValue, @Nonnegative int width);

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

  @RequiredUIAccess
  @Nonnull
  default Component withVisible(boolean visible) {
    setVisible(visible);
    return this;
  }

  boolean isEnabled();

  @RequiredUIAccess
  void setEnabled(boolean value);

  @RequiredUIAccess
  default Component withEnabled(boolean enabled) {
    setEnabled(enabled);
    return this;
  }

  @Nullable
  Component getParent();

  @RequiredUIAccess
  void setSize(@Nonnull Size size);

  @Nonnull
  @RequiredUIAccess
  default Component withSize(@Nonnull Size size) {
    setSize(size);
    return this;
  }

  @Nonnull
  default <T> Disposable addUserDataProvider(@Nonnull Key<T> key, @Nonnull Supplier<T> supplier) {
    return addUserDataProvider(k -> k == key ? supplier.get() : null);
  }

  @Nonnull
  Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function);

  @Nonnull
  Font getFont();

  void setFont(@Nonnull Font font);

  @Nonnull
  @RequiredUIAccess
  default Component withFont(@Nonnull Font font) {
    setFont(font);
    return this;
  }

  @Nullable
  default ColorValue getForegroundColor() {
    throw new AbstractMethodError("not supported");
  }

  default void setForegroundColor(@Nullable ColorValue foreground) {
    throw new AbstractMethodError("not supported");
  }

  @Nonnull
  default Component withForegroundColor(@Nullable ColorValue foreground) {
    setForegroundColor(foreground);
    return this;
  }

  @Nullable
  default Cursor getCursor() {
    throw new AbstractMethodError("not supported");
  }

  default void setCursor(@Nullable Cursor cursor) {
    throw new AbstractMethodError("not supported");
  }

  @Nonnull
  default Component withCursor(@Nullable Cursor cursor) {
    setCursor(cursor);
    return this;
  }
  
  /**
   * @return runner for unregister listener
   */
  @Nonnull
  <T extends EventListener> Disposable addListener(@Nonnull Class<T> eventClass, @RequiredUIAccess @Nonnull T listener);

  @Nonnull
  <T extends EventListener> T getListenerDispatcher(@Nonnull Class<T> eventClass);

  @Nonnull
  default Disposable addKeyListener(@Nonnull KeyListener keyListener) {
    return addListener(KeyListener.class, keyListener);
  }

  @Nonnull
  default Disposable addAttachListener(@Nonnull AttachListener attachListener) {
    return addListener(AttachListener.class, attachListener);
  }

  @Nonnull
  default Disposable addDetachListener(@Nonnull DetachListener detachListener) {
    return addListener(DetachListener.class, detachListener);
  }

  @Nonnull
  default Disposable addClickListener(@RequiredUIAccess @Nonnull ClickListener clickListener) {
    return addListener(ClickListener.class, clickListener);
  }

  @Override
  default void dispose() {
  }
}
