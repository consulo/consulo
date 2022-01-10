/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.desktop.internal.window;

import consulo.awt.TargetAWT;
import consulo.awt.impl.ToSwingWindowWrapper;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Size;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.desktop.internal.DesktopFontImpl;
import consulo.ui.font.Font;
import consulo.ui.impl.UIDataObject;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EventListener;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
public abstract class WindowOverAWTWindow implements Window, ToSwingWindowWrapper {
  protected final java.awt.Window myWindow;
  private final UIDataObject myUIDataObject = new UIDataObject();

  public WindowOverAWTWindow(java.awt.Window window) {
    myWindow = window;
    myWindow.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        myUIDataObject.dispose();
      }
    });
  }

  @Override
  public void dispose() {
    myWindow.dispose();
  }

  @Nonnull
  @Override
  public java.awt.Window toAWTWindow() {
    return myWindow;
  }

  @RequiredUIAccess
  @Override
  public void show() {
    setVisible(true);
  }

  @RequiredUIAccess
  @Override
  public void close() {
    setVisible(false);
  }

  @RequiredUIAccess
  @Override
  public void setContent(@Nonnull Component content) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {
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
    return myWindow.isVisible();
  }

  @Nonnull
  @Override
  public Font getFont() {
    return new DesktopFontImpl(myWindow.getFont());
  }

  @Override
  public void setFont(@Nonnull Font font) {
    myWindow.setFont(TargetAWT.to(font));
  }

  @RequiredUIAccess
  @Override
  public void setVisible(boolean value) {
    SwingUtilities.invokeLater(() -> myWindow.setVisible(value));
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void setEnabled(boolean value) {

  }

  @Nullable
  @Override
  public Window getParent() {
    return TargetAWT.from((java.awt.Window)myWindow.getParent());
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function) {
    return myUIDataObject.addUserDataProvider(function);
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    myUIDataObject.putUserData(key, value);
  }

  @Nonnull
  @Override
  public <T extends EventListener> T getListenerDispatcher(@Nonnull Class<T> eventClass) {
    return myUIDataObject.getDispatcher(eventClass);
  }

  @Nonnull
  @Override
  public <T extends EventListener> Disposable addListener(@Nonnull Class<T> eventClass, @Nonnull T listener) {
    return myUIDataObject.addListener(eventClass, listener);
  }

  @Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    return myUIDataObject.getUserData(key);
  }
}
