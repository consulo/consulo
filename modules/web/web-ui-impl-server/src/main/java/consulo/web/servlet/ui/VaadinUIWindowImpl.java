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
package consulo.web.servlet.ui;

import com.intellij.openapi.util.Key;
import com.vaadin.server.Page;
import com.vaadin.ui.UI;
import consulo.ui.*;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.internal.WGwtRootPanelImpl;
import consulo.ui.style.ColorKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EventListener;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
class VaadinUIWindowImpl implements Window {
  private final UI myUI;
  private WGwtRootPanelImpl myRootPanel = new WGwtRootPanelImpl();

  private boolean myDisposed;

  public VaadinUIWindowImpl(UI ui) {
    myUI = ui;
    myUI.setSizeFull();
    myUI.setContent(myRootPanel);
  }

  @Override
  public void dispose() {
    myDisposed = true;
  }

  @RequiredUIAccess
  @Override
  public void addBorder(@NotNull BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void removeBorder(@NotNull BorderPosition borderPosition) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isVisible() {
    return myUI.isVisible();
  }

  @RequiredUIAccess
  @Override
  public void setVisible(boolean value) {
    myUI.setVisible(value);
  }

  @Override
  public boolean isEnabled() {
    return myUI.isEnabled();
  }

  @RequiredUIAccess
  @Override
  public void setEnabled(boolean value) {
    myUI.setEnabled(value);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)myUI.getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }

  @NotNull
  @Override
  public <T> Runnable addUserDataProvider(@NotNull Key<T> key, @NotNull Supplier<T> supplier) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public <T extends EventListener> T getListenerDispatcher(@NotNull Class<T> eventClass) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public <T extends EventListener> Runnable addListener(@NotNull Class<T> eventClass, @NotNull T listener) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void setTitle(@NotNull String title) {
    Page.getCurrent().setTitle(title);
  }

  @RequiredUIAccess
  @Override
  public void setContent(@NotNull Component content) {
    if (myDisposed) {
      throw new IllegalArgumentException("Already disposed");
    }

    myRootPanel.setCenterComponent((com.vaadin.ui.Component)content);
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {
    myRootPanel.setMenuBar(menuBar);
  }

  @Override
  public void setResizable(boolean value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setClosable(boolean value) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void show() {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void close() {
    myUI.close();
  }

  @Nullable
  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    throw new UnsupportedOperationException();
  }
}
