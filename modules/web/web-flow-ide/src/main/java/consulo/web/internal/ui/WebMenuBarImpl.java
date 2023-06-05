/*
 * Copyright 2013-2023 consulo.io
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
import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.SimpleComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/05/2023
 *
 * TODO stub
 */
public class WebMenuBarImpl extends VaadinComponentDelegate<WebMenuBarImpl.Vaadin> implements MenuBar {
  public class Vaadin extends SimpleComponent implements FromVaadinComponentWrapper {

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebMenuBarImpl.this;
    }
  }

  public WebMenuBarImpl() {

  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Override
  public void clear() {
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public MenuBar add(@Nonnull MenuItem menuItem) {
    return this;
  }
}
