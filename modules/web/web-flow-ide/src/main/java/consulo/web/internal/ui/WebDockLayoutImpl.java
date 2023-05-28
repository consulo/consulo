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
import com.vaadin.flow.component.HasSize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.CompositeComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 27/05/2023
 */
public class WebDockLayoutImpl extends VaadinComponentDelegate<WebDockLayoutImpl.Vaadin> implements DockLayout {

  public class Vaadin extends CompositeComponent implements FromVaadinComponentWrapper {
    @Nullable
    @Override
    public consulo.ui.Component toUIComponent() {
      return WebDockLayoutImpl.this;
    }
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout top(@Nonnull consulo.ui.Component component) {
    Component vaadinComponent = TargetVaddin.to(component);
    toVaadinComponent().add(vaadinComponent);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout bottom(@Nonnull consulo.ui.Component component) {
    toVaadinComponent().add(TargetVaddin.to(component));
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout center(@Nonnull consulo.ui.Component component) {
    Component vaadinComponent = TargetVaddin.to(component);
    ((HasSize)vaadinComponent).setSizeFull();
    
    toVaadinComponent().add(vaadinComponent);
    //toVaadinComponent().setAlignSelf(FlexComponent.Alignment.CENTER, vaadinComponent);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout left(@Nonnull consulo.ui.Component component) {
    toVaadinComponent().add(TargetVaddin.to(component));
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout right(@Nonnull consulo.ui.Component component) {
    toVaadinComponent().add(TargetVaddin.to(component));
    return this;
  }
}
