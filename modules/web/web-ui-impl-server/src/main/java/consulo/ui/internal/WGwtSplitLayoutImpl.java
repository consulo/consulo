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
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import consulo.ui.SplitLayout;
import consulo.web.gwt.shared.ui.state.layout.SplitLayoutState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public class WGwtSplitLayoutImpl extends AbstractComponentContainer implements SplitLayout, VaadinWrapper {
  private Component myFirstComponent;
  private Component mySecondComponent;

  @Override
  public void setProportion(int percent) {
    getState().myProportion = percent;
    markAsDirty();
  }

  @Override
  protected SplitLayoutState getState() {
    return (SplitLayoutState)super.getState();
  }

  @RequiredUIAccess
  @Override
  public SplitLayout setFirstComponent(@NotNull Component component) {
    Component old = myFirstComponent;
    if (old != null) {
      removeComponent((com.vaadin.ui.Component)old);
    }

    myFirstComponent = component;
    addComponent((com.vaadin.ui.Component)component);
    return this;
  }

  @RequiredUIAccess
  @Override
  public SplitLayout setSecondComponent(@NotNull Component component) {
    Component old = mySecondComponent;
    if (old != null) {
      removeComponent((com.vaadin.ui.Component)old);
    }

    mySecondComponent = component;
    addComponent((com.vaadin.ui.Component)component);
    return this;
  }

  @Override
  public void replaceComponent(com.vaadin.ui.Component oldComponent, com.vaadin.ui.Component newComponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getComponentCount() {
    int i = 0;
    if (myFirstComponent != null) {
      i++;
    }
    if (mySecondComponent != null) {
      i++;
    }
    return i;
  }

  @Override
  public Iterator<com.vaadin.ui.Component> iterator() {
    List<com.vaadin.ui.Component> list = new ArrayList<>(getComponentCount());
    if (myFirstComponent != null) {
      list.add((com.vaadin.ui.Component)myFirstComponent);
    }
    if (mySecondComponent != null) {
      list.add((com.vaadin.ui.Component)mySecondComponent);
    }
    return list.iterator();
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
