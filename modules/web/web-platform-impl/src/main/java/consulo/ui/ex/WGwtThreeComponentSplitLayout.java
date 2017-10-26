/*
 * Copyright 2013-2017 consulo.io
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
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.HasComponents;
import consulo.ui.Component;
import consulo.ui.Layout;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.internal.VaadinWrapper;
import consulo.web.gwt.shared.ui.ex.state.ThreeComponentSplitLayoutState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 19-Oct-17
 */
public class WGwtThreeComponentSplitLayout extends AbstractComponent implements HasComponents, Layout, VaadinWrapper {
  public void setLeftComponent(@Nullable Component component) {
    com.vaadin.ui.Component vaadin = (com.vaadin.ui.Component)component;

    reset(getState().myLeftComponent);

    if (vaadin != null) {
      vaadin.setParent(this);
    }

    getState().myLeftComponent = vaadin;
  }

  public void setRightComponent(@Nullable Component component) {
    com.vaadin.ui.Component vaadin = (com.vaadin.ui.Component)component;

    reset(getState().myRightComponent);

    if (vaadin != null) {
      vaadin.setParent(this);
    }

    getState().myRightComponent = vaadin;
  }

  public void setCenterComponent(@Nullable Component component) {
    com.vaadin.ui.Component vaadin = (com.vaadin.ui.Component)component;

    reset(getState().myCenterComponent);

    if (vaadin != null) {
      vaadin.setParent(this);
    }

    getState().myCenterComponent = vaadin;
  }

  private void reset(@Nullable Connector connector) {
    if (connector != null) {
      com.vaadin.ui.Component component = (com.vaadin.ui.Component)connector;

      component.setParent(null);
    }
  }

  @Override
  protected ThreeComponentSplitLayoutState getState() {
    return (ThreeComponentSplitLayoutState)super.getState();
  }

  @Override
  public Iterator<com.vaadin.ui.Component> iterator() {
    List<com.vaadin.ui.Component> components = new ArrayList<>(3);

    ThreeComponentSplitLayoutState state = (ThreeComponentSplitLayoutState)getState(false);

    ContainerUtil.addIfNotNull(components, (com.vaadin.ui.Component)state.myLeftComponent);
    ContainerUtil.addIfNotNull(components, (com.vaadin.ui.Component)state.myRightComponent);
    ContainerUtil.addIfNotNull(components, (com.vaadin.ui.Component)state.myCenterComponent);

    return components.iterator();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {
  }
}
