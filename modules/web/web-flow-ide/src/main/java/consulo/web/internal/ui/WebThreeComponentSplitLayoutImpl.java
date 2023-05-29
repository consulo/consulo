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

import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ThreeComponentSplitLayout;
import consulo.web.internal.ui.base.FromVaadinComponentWrapper;
import consulo.web.internal.ui.base.TargetVaddin;
import consulo.web.internal.ui.base.VaadinComponentDelegate;
import consulo.web.internal.ui.vaadin.CompositeComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 29/05/2023
 */
public class WebThreeComponentSplitLayoutImpl extends VaadinComponentDelegate<WebThreeComponentSplitLayoutImpl.Vaadin> implements ThreeComponentSplitLayout {
  public class Vaadin extends CompositeComponent implements FromVaadinComponentWrapper {
    private SplitLayout myFirstLayout;
    private SplitLayout mySecondLayout;

    public Vaadin() {
      myFirstLayout = new SplitLayout(SplitLayout.Orientation.HORIZONTAL);
      mySecondLayout = new SplitLayout(SplitLayout.Orientation.HORIZONTAL);

      myFirstLayout.addToSecondary(mySecondLayout);

      myFirstLayout.setSizeFull();
      add(myFirstLayout);

      myFirstLayout.setSplitterPosition(10);
      mySecondLayout.setSplitterPosition(50);
    }

    public void addFirst(com.vaadin.flow.component.Component component) {
      myFirstLayout.addToPrimary(component);
    }

    public void addCenter(com.vaadin.flow.component.Component component) {
      mySecondLayout.addToPrimary(component);
    }

    public void addSecond(com.vaadin.flow.component.Component component) {
      mySecondLayout.addToSecondary(component);
    }

    @Nullable
    @Override
    public Component toUIComponent() {
      return WebThreeComponentSplitLayoutImpl.this;
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
  public ThreeComponentSplitLayout setFirstComponent(@Nullable Component component) {
    if (component != null) {
      com.vaadin.flow.component.Component vComponent = TargetVaddin.to(component);
      ((HasSize)vComponent).setSizeFull();
      toVaadinComponent().addFirst(vComponent);
    }
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ThreeComponentSplitLayout setCenterComponent(@Nullable Component component) {
    if (component != null) {
      com.vaadin.flow.component.Component vComponent = TargetVaddin.to(component);
      ((HasSize)vComponent).setSizeFull();
      toVaadinComponent().addCenter(vComponent);
    }
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ThreeComponentSplitLayout setSecondComponent(@Nullable Component component) {
    if (component != null) {
      com.vaadin.flow.component.Component vComponent = TargetVaddin.to(component);
      ((HasSize)vComponent).setSizeFull();
      toVaadinComponent().addSecond(vComponent);
    }
    return this;
  }
}
