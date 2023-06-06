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
package consulo.web.internal.servlet;

import com.vaadin.flow.component.UI;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Size;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.color.ColorValue;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.web.internal.ui.WebRootPaneImpl;
import consulo.web.internal.ui.base.TargetVaddin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.EventListener;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
class UIWindowOverRouterLayout extends UserDataHolderBase implements Window {
  private final WebRootPaneImpl myRootPanel = new WebRootPaneImpl();

  private Font myFont = FontManager.get().createFont("?", 12);

  private boolean myDisposed;

  @RequiredUIAccess
  public UIWindowOverRouterLayout(VaadinRootLayout routerLayout) {
    routerLayout.add(TargetVaddin.to(myRootPanel.getComponent()));
  }

  @Override
  public void dispose() {
    myDisposed = true;
  }

  @RequiredUIAccess
  @Override
  public void addBorder(@Nonnull BorderPosition borderPosition, BorderStyle borderStyle, ColorValue colorValue, int width) {
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
    return null;
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {

  }

  @Nonnull
  @Override
  public Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function) {
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

  @Nonnull
  @Override
  public Font getFont() {
    return myFont;
  }

  @Override
  public void setFont(@Nonnull Font font) {
    myFont = font;
  }

  @RequiredUIAccess
  @Override
  public void setTitle(@Nonnull String title) {
    UI.getCurrent().getPage().setTitle(title);
  }

  @RequiredUIAccess
  @Override
  public void setContent(@Nonnull Component content) {
    if (myDisposed) {
      throw new IllegalArgumentException("Already disposed");
    }

    myRootPanel.setCenterComponent(content);
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {
    myRootPanel.setMenuBar(menuBar);
  }

  @RequiredUIAccess
  @Override
  public void show() {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void close() {
    Disposer.dispose(this);
  }

  @Override
  public boolean isActive() {
    return true;
  }
}
