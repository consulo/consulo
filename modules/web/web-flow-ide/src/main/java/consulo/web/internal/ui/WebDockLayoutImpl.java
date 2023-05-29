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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.vaadin.VaadinSizeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.vaadin.addons.johannest.borderlayout.BorderLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 27/05/2023
 */
public class WebDockLayoutImpl extends WebLayoutImpl<WebDockLayoutImpl.Vaadin> implements DockLayout {

  public class Vaadin extends BorderLayout implements FromVaadinComponentWrapper {
    @Nullable
    @Override
    public consulo.ui.Component toUIComponent() {
      return WebDockLayoutImpl.this;
    }
  }

  private List<Component> myCenterComponents = new ArrayList<>();

  @RequiredUIAccess
  @Override
  public void removeAll() {
    super.removeAll();

    myCenterComponents.clear();
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
    return replace(component, BorderLayout.Constraint.NORTH);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout bottom(@Nonnull consulo.ui.Component component) {
    return replace(component, BorderLayout.Constraint.SOUTH);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout center(@Nonnull consulo.ui.Component component) {
    VaadinSizeUtil.setWidthFull(component);
    return replace(component, BorderLayout.Constraint.CENTER);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout left(@Nonnull consulo.ui.Component component) {
    return replace(component, BorderLayout.Constraint.WEST);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout right(@Nonnull consulo.ui.Component component) {
    return replace(component, BorderLayout.Constraint.EAST);
  }

  private DockLayout replace(consulo.ui.Component child, BorderLayout.Constraint constraint) {
    Component oldComponent = toVaadinComponent().getComponent(constraint);
    if (oldComponent != null && Objects.equals(toVaadinComponent(), oldComponent.getParent().orElse(null))) {
      toVaadinComponent().removeLayoutComponent(oldComponent);
    }

    if (child != null) {
      toVaadinComponent().addComponent(TargetVaddin.to(child), constraint);
    }

    return this;
  }
}
