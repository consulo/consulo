/*
 * Copyright 2013-2016 consulo.io
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
import consulo.ui.RequiredUIAccess;
import consulo.ui.internal.VaadinWrapper;
import consulo.ui.layout.Layout;
import consulo.ui.shared.Size;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.VaadinComponentContainer;
import consulo.ui.web.internal.border.WGwtBorderBuilder;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtDockLayoutImpl extends VaadinComponentContainer<WebDockLayoutImpl> implements VaadinWrapper {
  private final Map<DockLayoutState.Constraint, com.vaadin.ui.Component> myChildren = new LinkedHashMap<>();

  public WGwtDockLayoutImpl(WebDockLayoutImpl component) {
    super(component);
  }

  @RequiredUIAccess
  public void removeAll() {
    for (com.vaadin.ui.Component child : new ArrayList<>(myChildren.values())) {
      removeComponent(child);
    }
    markAsDirty();
  }

  @Override
  protected DockLayoutState getState() {
    return (DockLayoutState)super.getState();
  }

  void placeAt(@Nonnull Component uiComponent, DockLayoutState.Constraint constraint) {
    com.vaadin.ui.Component component = TargetVaddin.to(uiComponent);

    Component parentComponent = uiComponent.getParentComponent();
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
    WGwtBorderBuilder.fill(this, getState().myBorderListState);
  }

  @Override
  public void replaceComponent(com.vaadin.ui.Component removeComponent, com.vaadin.ui.Component newComponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getComponentCount() {
    return myChildren.size();
  }

  @Override
  public Iterator<com.vaadin.ui.Component> iterator() {
    return myChildren.values().iterator();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
  }
}
