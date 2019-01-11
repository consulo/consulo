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
package consulo.ui.web.internal;

import com.intellij.openapi.util.Disposer;
import consulo.ui.*;
import consulo.ui.shared.Size;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class VaadinWindowImpl extends com.vaadin.ui.Window implements Window, VaadinWrapper {
  private boolean myDisposed;
  private WGwtRootPanelImpl myRootPanel = new WGwtRootPanelImpl();

  public VaadinWindowImpl(boolean modal) {
    setModal(modal);
    setContent((com.vaadin.ui.Component)myRootPanel);
    addCloseListener(closeEvent -> getListenerDispatcher(Window.CloseListener.class).onClose());
  }

  @RequiredUIAccess
  @Override
  public void show() {
    if (myDisposed) {
      throw new IllegalArgumentException("Window already disposed");
    }

    VaadinUIAccessImpl uiAccess = (VaadinUIAccessImpl)UIAccess.current();

    uiAccess.getUI().addWindow(this);
  }

  @RequiredUIAccess
  @Override
  public void close() {
    super.close();

    myDisposed = true;

    Disposer.dispose(this);
  }

  @RequiredUIAccess
  @Override
  public void setTitle(@Nonnull String title) {
    setCaption(title);
  }

  @RequiredUIAccess
  @Override
  public void setContent(@Nonnull Component content) {
    myRootPanel.setCenterComponent(content);
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {
    myRootPanel.setMenuBar(menuBar);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    if (size.getWidth() != -1) {
      setWidth(size.getWidth(), Unit.PIXELS);
    }

    if (size.getHeight() != -1) {
      setHeight(size.getHeight(), Unit.PIXELS);
    }
  }
}
