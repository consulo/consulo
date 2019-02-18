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

import com.vaadin.ui.Component;
import consulo.ui.web.internal.base.UIComponentWithVaadinComponent;
import consulo.ui.web.internal.base.VaadinComponentContainer;
import consulo.web.gwt.shared.ui.state.RootPanelState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebRootPaneImpl extends UIComponentWithVaadinComponent<WebRootPaneImpl.Vaadin> {
  public static class Vaadin extends VaadinComponentContainer {
    private Component myMenuBar;
    private Component myCenterComponent;

    public void setMenuBar(@Nullable Component menuBar) {
      if (myMenuBar != null) {
        removeComponent(myMenuBar);
      }
      myMenuBar = menuBar;
      if (menuBar != null) {
        addComponent(menuBar);
      }

      getState().menuBarExists = menuBar != null;
      markAsDirty();
    }

    private void setCenterComponent(@Nullable Component centerComponent) {
      if (myCenterComponent != null) {
        removeComponent(myCenterComponent);
      }
      myCenterComponent = centerComponent;
      if (centerComponent != null) {
        addComponent(myCenterComponent);
      }

      getState().contentExists = centerComponent != null;
      markAsDirty();
    }

    @Override
    public int getComponentCount() {
      int i = 0;
      if (myMenuBar != null) {
        i++;
      }
      if (myCenterComponent != null) {
        i++;
      }
      return i;
    }

    @Override
    public Iterator<Component> iterator() {
      List<Component> list = new ArrayList<>(getComponentCount());
      if (myMenuBar != null) {
        list.add(myMenuBar);
      }
      if (myCenterComponent != null) {
        list.add(myCenterComponent);
      }
      return list.iterator();
    }

    @Override
    public RootPanelState getState() {
      return (RootPanelState)super.getState();
    }
  }

  @Nonnull
  @Override
  public Vaadin create() {
    return new Vaadin();
  }

  public void setSizeFull() {
    getVaadinComponent().setSizeFull();
  }

  public void setCenterComponent(@Nullable consulo.ui.Component centerComponent) {
    getVaadinComponent().setCenterComponent(TargetVaddin.to(centerComponent));
  }

  public void setMenuBar(@Nullable consulo.ui.MenuBar menuBar) {
    getVaadinComponent().setMenuBar(TargetVaddin.to(menuBar));
  }
}
