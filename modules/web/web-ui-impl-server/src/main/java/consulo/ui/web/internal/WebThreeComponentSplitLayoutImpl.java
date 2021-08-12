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

import com.intellij.util.containers.ContainerUtil;
import com.vaadin.shared.Connector;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ThreeComponentSplitLayout;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinComponent;
import consulo.web.gwt.shared.ui.ex.state.ThreeComponentSplitLayoutState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebThreeComponentSplitLayoutImpl extends VaadinComponentDelegate<WebThreeComponentSplitLayoutImpl.Vaadin> implements ThreeComponentSplitLayout {
  public static class Vaadin extends VaadinComponent implements HasComponents {
    @Override
    public ThreeComponentSplitLayoutState getState() {
      return (ThreeComponentSplitLayoutState)super.getState();
    }

    @Override
    public Iterator<Component> iterator() {
      List<Component> components = new ArrayList<>(3);

      ThreeComponentSplitLayoutState state = (ThreeComponentSplitLayoutState)getState(false);

      ContainerUtil.addIfNotNull(components, (Component)state.myLeftComponent);
      ContainerUtil.addIfNotNull(components, (Component)state.myRightComponent);
      ContainerUtil.addIfNotNull(components, (Component)state.myCenterComponent);

      return components.iterator();
    }

    public void setFirstComponent(@Nullable Component component) {
      reset(getState().myLeftComponent);

      if (component != null) {
        component.setParent(this);
      }

      getState().myLeftComponent = component;
    }

    public void setSecondComponent(@Nullable Component component) {
      reset(getState().myRightComponent);

      if (component != null) {
        component.setParent(this);
      }

      getState().myRightComponent = component;
    }

    public void setCenterComponent(@Nullable Component component) {
      reset(getState().myCenterComponent);

      if (component != null) {
        component.setParent(this);
      }

      getState().myCenterComponent = component;
    }

    private void reset(@Nullable Connector connector) {
      if (connector != null) {
        Component component = (Component)connector;

        component.setParent(null);
      }
    }
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ThreeComponentSplitLayout setFirstComponent(@Nullable consulo.ui.Component component) {
    getVaadinComponent().setFirstComponent(TargetVaddin.to(component));
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ThreeComponentSplitLayout setCenterComponent(@Nullable consulo.ui.Component component) {
    getVaadinComponent().setCenterComponent(TargetVaddin.to(component));
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public ThreeComponentSplitLayout setSecondComponent(@Nullable consulo.ui.Component component) {
    getVaadinComponent().setSecondComponent(TargetVaddin.to(component));
    return this;
  }
}
