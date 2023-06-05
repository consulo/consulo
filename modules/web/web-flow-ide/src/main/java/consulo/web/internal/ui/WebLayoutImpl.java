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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.Layout;
import consulo.util.lang.ObjectUtil;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import jakarta.annotation.Nonnull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 28/05/2023
 */
public abstract class WebLayoutImpl<C extends Component & HasComponents & FromVaadinComponentWrapper> extends VaadinComponentDelegate<C> implements Layout {
  @RequiredUIAccess
  @Override
  public void removeAll() {
    toVaadinComponent().removeAll();
  }

  @Override
  public void remove(@Nonnull consulo.ui.Component component) {
    if (component.getParent() == this) {
      toVaadinComponent().remove(TargetVaddin.to(component));
    }
  }

  @Override
  public void forEachChild(@Nonnull Consumer<consulo.ui.Component> consumer) {
    C c = toVaadinComponent();

    c.getChildren()
     .map(component -> ObjectUtil.tryCast(component, FromVaadinComponentWrapper.class))
     .filter(Objects::nonNull)
     .map(FromVaadinComponentWrapper::toUIComponent)
     .forEach(consumer::accept);
  }
}
