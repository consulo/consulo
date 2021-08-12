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

import consulo.disposer.Disposer;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.web.internal.base.ComponentHolder;
import consulo.ui.web.internal.base.FromVaadinComponentWrapper;
import consulo.ui.web.internal.base.VaadinComponentDelegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebWindowImpl extends VaadinComponentDelegate<WebWindowImpl.Vaadin> implements Window {
  public static class Vaadin extends com.vaadin.ui.Window implements ComponentHolder, FromVaadinComponentWrapper {
    private Component myComponent;

    @Override
    public void setComponent(Component component) {
      myComponent = component;
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return myComponent;
    }
  }

  private boolean myDisposed;
  private WebRootPaneImpl myRootPanel = new WebRootPaneImpl();

  public WebWindowImpl(boolean modal, WindowOptions options) {
    Vaadin vaadinComponent = getVaadinComponent();

    vaadinComponent.setModal(modal);
    vaadinComponent.setResizable(options.isResizable());
    vaadinComponent.setClosable(options.isClosable());
    vaadinComponent.setContent(TargetVaddin.to(myRootPanel.getComponent()));
    vaadinComponent.addCloseListener(closeEvent -> getListenerDispatcher(Window.CloseListener.class).onClose());

    WebFocusManagerImpl.register(toVaadinComponent());
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Override
  public void show() {
    if (myDisposed) {
      throw new IllegalArgumentException("Window already disposed");
    }

    WebUIAccessImpl uiAccess = (WebUIAccessImpl)UIAccess.current();

    uiAccess.getUI().addWindow(getVaadinComponent());
  }

  @RequiredUIAccess
  @Override
  public void close() {
    getVaadinComponent().close();

    myDisposed = true;

    Disposer.dispose(this);
  }

  @RequiredUIAccess
  @Override
  public void setTitle(@Nonnull String title) {
    getVaadinComponent().setCaption(title);
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
  public Window getParent() {
    return (Window)super.getParent();
  }
}