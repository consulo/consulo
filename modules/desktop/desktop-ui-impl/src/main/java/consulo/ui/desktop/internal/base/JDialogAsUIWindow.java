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
package consulo.ui.desktop.internal.base;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Key;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Window;
import consulo.ui.impl.UIDataObject;
import consulo.ui.shared.Size;
import consulo.ui.shared.border.BorderPosition;
import consulo.ui.shared.border.BorderStyle;
import consulo.ui.style.ColorKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.EventListener;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-02-15
 */
public class JDialogAsUIWindow extends JDialog implements Window {
  private UIDataObject myUIDataObject = new UIDataObject();

  public JDialogAsUIWindow() {
  }

  public JDialogAsUIWindow(Window owner, String title) {
    super((java.awt.Window)owner, title);
  }

  @Override
  public void dispose() {
    super.dispose();

    myUIDataObject = null;
  }

  @RequiredUIAccess
  @Override
  public void showAsync() {
    SwingUtilities.invokeLater(() -> setVisible(true));
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

  @Override
  public void setClosable(boolean value) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void addBorder(@Nonnull BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void removeBorder(@Nonnull BorderPosition borderPosition) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Window getParentComponent() {
    return (Window)getParent();
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
    return myUIDataObject.getUserData(key);
  }
}
