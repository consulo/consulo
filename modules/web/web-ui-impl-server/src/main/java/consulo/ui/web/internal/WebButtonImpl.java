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
package consulo.ui.web.internal;

import consulo.ui.Button;
import consulo.ui.RequiredUIAccess;
import consulo.ui.web.internal.base.UIComponentWithVaadinComponent;
import consulo.ui.web.internal.base.VaadinComponent;
import consulo.web.gwt.shared.ui.state.button.ButtonRpc;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebButtonImpl extends UIComponentWithVaadinComponent<WebButtonImpl.Vaadin> implements Button {
  public static class Vaadin extends VaadinComponent {
    private final ButtonRpc myRpc = new ButtonRpc() {
      @Override
      public void onClick() {
        toUIComponent().getListenerDispatcher(ClickListener.class).onClick();
      }
    };

    public Vaadin() {
      registerRpc(myRpc);
    }

    public void setText(String text) {
      getState().caption = text;
      markAsDirty();
    }
  }

  public WebButtonImpl(String text) {
    setText(text);
  }

  @Nonnull
  @Override
  public String getText() {
    return getVaadinComponent().getState().caption;
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull String text) {
    getVaadinComponent().setText(text);
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }
}
