/*
 * Copyright 2013-2021 consulo.io
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
package consulo.awt.impl;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.FocusableComponent;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.font.Font;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EventListener;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 16/08/2021
 */
class TempComponentWrapper implements Component, ToSwingComponentWrapper, FocusableComponent {
  private final java.awt.Component myComponent;

  TempComponentWrapper(java.awt.Component component) {
    myComponent = component;
  }

  @RequiredUIAccess
  @Override
  public void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, @Nullable ColorValue colorValue, @Nonnegative int width) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void removeBorder(@Nonnull BorderPosition borderPosition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isVisible() {
    return myComponent.isVisible();
  }

  @RequiredUIAccess
  @Override
  public void setVisible(boolean value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEnabled() {
    return myComponent.isEnabled();
  }

  @RequiredUIAccess
  @Override
  public void setEnabled(boolean value) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Component getParent() {
    throw new UnsupportedOperationException();
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

  @Nonnull
  @Override
  public java.awt.Component toAWTComponent() {
    return myComponent;
  }

  @Override
  public boolean hasFocus() {
    return myComponent.hasFocus();
  }
}
