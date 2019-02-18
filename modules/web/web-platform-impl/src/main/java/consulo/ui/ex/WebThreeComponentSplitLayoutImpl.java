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
package consulo.ui.ex;

import com.intellij.util.containers.ContainerUtil;
import com.vaadin.shared.Connector;
import com.vaadin.ui.Component;
import com.vaadin.ui.HasComponents;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.UIComponentWithVaadinComponent;
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
public class WebThreeComponentSplitLayoutImpl extends UIComponentWithVaadinComponent<WebThreeComponentSplitLayoutImpl.Vaadin> {
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

    public void setLeftComponent(@Nullable Component component) {
      reset(getState().myLeftComponent);

      if (component != null) {
        component.setParent(this);
      }

      getState().myLeftComponent = component;
    }

    public void setRightComponent(@Nullable Component component) {
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
  public Vaadin create() {
    return new Vaadin();
  }

  public void setLeftComponent(@Nullable consulo.ui.Component component) {
    getVaadinComponent().setLeftComponent(TargetVaddin.to(component));
  }

  public void setRightComponent(@Nullable consulo.ui.Component component) {
    getVaadinComponent().setRightComponent(TargetVaddin.to(component));
  }

  public void setCenterComponent(@Nullable consulo.ui.Component component) {
    getVaadinComponent().setCenterComponent(TargetVaddin.to(component));
  }
}
