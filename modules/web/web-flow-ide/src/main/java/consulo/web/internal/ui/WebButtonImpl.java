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
package consulo.web.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebButtonImpl extends VaadinComponentDelegate<WebButtonImpl.Vaadin> implements Button {
  public class Vaadin extends com.vaadin.flow.component.button.Button implements FromVaadinComponentWrapper {
    @Nullable
    @Override
    public Component toUIComponent() {
      return WebButtonImpl.this;
    }
  }

  private LocalizeValue myTextValue = LocalizeValue.empty();

  public WebButtonImpl(LocalizeValue text) {
    Vaadin component = toVaadinComponent();

    component.addClickListener(event -> {
      getListenerDispatcher(ClickEvent.class).onEvent(new ClickEvent(this));
    });

    myTextValue = text;
    component.setText(text.get());
  }

  @Nonnull
  @Override
  public LocalizeValue getText() {
    return myTextValue;
  }
                                                                                                     
  @RequiredUIAccess
  @Override
  public void setText(@Nonnull LocalizeValue text) {
    myTextValue = text;
    toVaadinComponent().setText(text.get());
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Override
  public void setIcon(@Nullable Image image) {
   // TODO  toVaadinComponent().setIcon(image);
  }

  @Nullable
  @Override
  public Image getIcon() {
    // TODO
    return null;
    //return toVaadinComponent().myImage;
  }
}
