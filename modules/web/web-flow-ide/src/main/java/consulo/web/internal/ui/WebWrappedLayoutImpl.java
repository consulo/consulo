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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.WrappedLayout;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.VaadinSingleComponentContainer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebWrappedLayoutImpl extends VaadinComponentDelegate<WebWrappedLayoutImpl.Vaadin> implements WrappedLayout {
  public class Vaadin extends VaadinSingleComponentContainer {
    @Nullable
    @Override
    public Component toUIComponent() {
      return WebWrappedLayoutImpl.this;
    }
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Override
  public void removeAll() {
    getVaadinComponent().setContent(null);
  }

  @Override
  public void remove(@Nonnull Component component) {
    getVaadinComponent().removeIfContent(component);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public WrappedLayout set(@Nullable Component component) {
    getVaadinComponent().setContent(component);
    return this;
  }
}
