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

import com.vaadin.ui.AbstractLayout;
import consulo.ui.Component;
import consulo.ui.DockLayout;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtDockLayoutImpl extends AbstractLayout implements DockLayout {
  private final List<com.vaadin.ui.Component> myChildren = new LinkedList<>();

  @Override
  protected DockLayoutState getState() {
    return (DockLayoutState)super.getState();
  }

  private void add(@NotNull com.vaadin.ui.Component component, DockLayoutState.Constraint constraint) {
    myChildren.add(component);
    addComponent(component);
    getState().myConstraints.add(constraint);
  }

  @Override
  public void removeComponent(com.vaadin.ui.Component c) {
    int i = myChildren.indexOf(c);
    getState().myConstraints.remove(i);

    myChildren.remove(c);
    super.removeComponent(c);
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout top(@NotNull Component component) {
    add((com.vaadin.ui.Component)component, DockLayoutState.Constraint.TOP);
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout bottom(@NotNull Component component) {
    add((com.vaadin.ui.Component)component, DockLayoutState.Constraint.BOTTOM);
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout center(@NotNull Component component) {
    add((com.vaadin.ui.Component)component, DockLayoutState.Constraint.CENTER);
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout left(@NotNull Component component) {
    add((com.vaadin.ui.Component)component, DockLayoutState.Constraint.LEFT);
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public DockLayout right(@NotNull Component component) {
    add((com.vaadin.ui.Component)component, DockLayoutState.Constraint.RIGHT);
    return this;
  }

  @Override
  public void replaceComponent(com.vaadin.ui.Component removeComponent, com.vaadin.ui.Component newComponent) {
    removeComponent(removeComponent);
    addComponent(newComponent);
  }

  @Override
  public int getComponentCount() {
    return myChildren.size();
  }

  @Override
  public Iterator<com.vaadin.ui.Component> iterator() {
    return myChildren.iterator();
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
