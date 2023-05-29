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
package consulo.web.internal.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.Icon;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.internal.ui.base.ComponentHolder;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.VaadinSizeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class WebWindowImpl extends VaadinComponentDelegate<WebWindowImpl.Vaadin> implements Window {
  public static class Vaadin extends Dialog implements ComponentHolder, FromVaadinComponentWrapper {
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
  private final WebRootPaneImpl myRootPanel = new WebRootPaneImpl();

  public WebWindowImpl(boolean modal, WindowOptions options) {
    Vaadin vaadinComponent = getVaadinComponent();

    vaadinComponent.setModal(modal);
    vaadinComponent.setResizable(options.isResizable());
    vaadinComponent.setCloseOnEsc(false);
    vaadinComponent.setCloseOnOutsideClick(false);
    if (options.isClosable()) {
      addCloseDialogButton(vaadinComponent);
    }

    VaadinSizeUtil.setWidthFull(myRootPanel.getComponent());
    vaadinComponent.add(TargetVaddin.to(myRootPanel.getComponent()));
    // TODO vaadinComponent.addCloseListener(closeEvent -> getListenerDispatcher(Window.CloseListener.class).onClose());

    WebFocusManagerImpl.register(toVaadinComponent());
  }

  private static void addCloseDialogButton(Dialog dialog) {
    Button closeButton = new Button(new Icon("lumo", "cross"), (e) -> dialog.close());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    dialog.getHeader().add(closeButton);
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

    toVaadinComponent().open();
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
    getVaadinComponent().setHeaderTitle(title);
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

  @Override
  public boolean isActive() {
    return true;
  }

  @Override
  public void dispose() {

  }

  @Nullable
  @Override
  public Window getParent() {
    return (Window)super.getParent();
  }
}