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
package consulo.ui.web.internal.layout;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.Layout;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinComponentContainer;
import consulo.ui.web.internal.border.WebBorderBuilder;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebDockLayoutImpl extends VaadinComponentDelegate<WebDockLayoutImpl.Vaadin> implements DockLayout {
  protected static class Vaadin extends VaadinComponentContainer {
    private final Map<DockLayoutState.Constraint, com.vaadin.ui.Component> myChildren = new LinkedHashMap<>();

    @RequiredUIAccess
    public void removeAll() {
      for (com.vaadin.ui.Component child : new ArrayList<>(myChildren.values())) {
        removeComponent(child);
      }
      markAsDirty();
    }

    @Override
    public DockLayoutState getState() {
      return (DockLayoutState)super.getState();
    }

    void placeAt(@Nonnull Component uiComponent, DockLayoutState.Constraint constraint) {
      com.vaadin.ui.Component component = TargetVaddin.to(uiComponent);

      Component parentComponent = uiComponent.getParent();
      // remove from old parent
      if (parentComponent instanceof Layout) {
        ((Layout)parentComponent).remove(uiComponent);
      }

      com.vaadin.ui.Component oldComponent = myChildren.remove(constraint);
      if (oldComponent != null) {
        removeComponent(oldComponent);
      }

      myChildren.put(constraint, component);

      addComponent(component);

      getState().myConstraints = new ArrayList<>(myChildren.keySet());
    }

    @Override
    public void removeComponent(com.vaadin.ui.Component c) {
      DockLayoutState.Constraint constraint = null;

      for (Map.Entry<DockLayoutState.Constraint, com.vaadin.ui.Component> entry : myChildren.entrySet()) {
        if (entry.getValue() == c) {
          constraint = entry.getKey();
        }
      }

      if (constraint != null) {
        myChildren.remove(constraint);
      }

      super.removeComponent(c);
    }

    @Override
    public void beforeClientResponse(boolean initial) {
      super.beforeClientResponse(initial);
      WebBorderBuilder.fill(toUIComponent(), getState().myBorderListState);
    }

    @Override
    public int getComponentCount() {
      return myChildren.size();
    }

    @Override
    public Iterator<com.vaadin.ui.Component> iterator() {
      return myChildren.values().iterator();
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
    getVaadinComponent().removeAll();
  }

  @Override
  public void remove(@Nonnull Component component) {
    getVaadinComponent().removeComponent(TargetVaddin.to(component));
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout top(@Nonnull Component component) {
    getVaadinComponent().placeAt(component, DockLayoutState.Constraint.TOP);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout bottom(@Nonnull Component component) {
    getVaadinComponent().placeAt(component, DockLayoutState.Constraint.BOTTOM);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout center(@Nonnull Component component) {
    getVaadinComponent().placeAt(component, DockLayoutState.Constraint.CENTER);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout left(@Nonnull Component component) {
    getVaadinComponent().placeAt(component, DockLayoutState.Constraint.LEFT);
    return this;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public DockLayout right(@Nonnull Component component) {
    getVaadinComponent().placeAt(component, DockLayoutState.Constraint.RIGHT);
    return this;
  }
}
