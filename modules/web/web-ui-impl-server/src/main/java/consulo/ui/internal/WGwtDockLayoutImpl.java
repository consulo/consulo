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
package consulo.ui.internal;

import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.HasComponents;
import consulo.ui.*;
import consulo.ui.internal.border.WGwtBorderBuilder;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtDockLayoutImpl extends AbstractComponentContainer implements DockLayout, VaadinWrapper {
  private final Map<DockLayoutState.Constraint, com.vaadin.ui.Component> myChildren = new LinkedHashMap<>();

  @RequiredUIAccess
  @Override
  public void clear() {
    for (com.vaadin.ui.Component child : new ArrayList<>(myChildren.values())) {
      removeComponent(child);
    }
    markAsDirty();
  }

  @Override
  public void remove(@NotNull Component component) {
    removeComponent((com.vaadin.ui.Component)component);
  }

  @Override
  protected DockLayoutState getState() {
    return (DockLayoutState)super.getState();
  }

  private void placeAt(@NotNull com.vaadin.ui.Component component, DockLayoutState.Constraint constraint) {
    HasComponents parent = component.getParent();
    // remove from old parent
    if (parent instanceof Layout) {
      ((Layout)parent).remove((Component)component);
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

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout top(@NotNull Component component) {
    placeAt((com.vaadin.ui.Component)component, DockLayoutState.Constraint.TOP);
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout bottom(@NotNull Component component) {
    placeAt((com.vaadin.ui.Component)component, DockLayoutState.Constraint.BOTTOM);
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout center(@NotNull Component component) {
    placeAt((com.vaadin.ui.Component)component, DockLayoutState.Constraint.CENTER);
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout left(@NotNull Component component) {
    placeAt((com.vaadin.ui.Component)component, DockLayoutState.Constraint.LEFT);
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout right(@NotNull Component component) {
    placeAt((com.vaadin.ui.Component)component, DockLayoutState.Constraint.RIGHT);
    return this;
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

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {
  }
}
