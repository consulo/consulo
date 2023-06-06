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

import consulo.ui.Component;
import consulo.ui.MenuItem;
import consulo.ui.image.Image;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.SimpleComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebMenuItemImpl extends VaadinComponentDelegate<WebMenuItemImpl.Vaadin> implements MenuItem {
  public class Vaadin extends SimpleComponent implements FromVaadinComponentWrapper {
    private String myText = "";
    private Image myIcon;

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebMenuItemImpl.this;
    }
  }

  public WebMenuItemImpl(String text) {
    getVaadinComponent().myText = text;
  }

  @Nonnull
  @Override
  public String getText() {
    return toVaadinComponent().myText;
  }

  @Override
  public void setIcon(@Nullable Image icon) {
    toVaadinComponent().myIcon = icon;
  }

  @Nonnull
  @Override
  public WebMenuItemImpl.Vaadin createVaadinComponent() {
    return new Vaadin();
  }
}
