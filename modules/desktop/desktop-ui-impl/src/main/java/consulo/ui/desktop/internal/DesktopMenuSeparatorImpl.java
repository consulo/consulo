/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ui.desktop.internal;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.MenuSeparator;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.font.Font;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EventListener;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
class DesktopMenuSeparatorImpl implements MenuSeparator {
  public static final DesktopMenuSeparatorImpl INSTANCE = new DesktopMenuSeparatorImpl();

  @Nonnull
  @Override
  public String getText() {
    return "";
  }

  @Override
  public void setIcon(@Nullable Image icon) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, ColorValue colorValue, int width) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void removeBorder(@Nonnull BorderPosition borderPosition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void setVisible(boolean value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void setEnabled(boolean value) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Component getParent() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Font getFont() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setFont(@Nonnull Font font) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T extends EventListener> T getListenerDispatcher(@Nonnull Class<T> eventClass) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T extends EventListener> Disposable addListener(@Nonnull Class<T> eventClass, @Nonnull T listener) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    throw new UnsupportedOperationException();
  }
}
