/*
 * Copyright 2013-2020 consulo.io
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

import com.vaadin.flow.component.HasSize;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.CompositeComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-05-10
 */
public class WebScrollLayoutImpl extends VaadinComponentDelegate<WebScrollLayoutImpl.Vaadin> implements ScrollableLayout {
  public class Vaadin extends CompositeComponent implements FromVaadinComponentWrapper {

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebScrollLayoutImpl.this;
    }
  }

  public WebScrollLayoutImpl(Component component) {
    HasSize content = (HasSize)TargetVaddin.to(component);
    content.setSizeFull();
    getVaadinComponent().add((com.vaadin.flow.component.Component)content);
  }

  @Override
  public void remove(@Nonnull Component component) {
    getVaadinComponent().remove(TargetVaddin.to(component));
  }

  @RequiredUIAccess
  @Override
  public void removeAll() {
    getVaadinComponent().removeAll();
  }

  @Nonnull
  @Override
  public WebScrollLayoutImpl.Vaadin createVaadinComponent() {
    return new Vaadin();
  }
}