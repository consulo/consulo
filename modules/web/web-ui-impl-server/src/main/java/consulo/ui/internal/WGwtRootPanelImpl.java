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
package consulo.ui.internal;

import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public class WGwtRootPanelImpl extends AbstractComponentContainer implements consulo.ui.Component, VaadinWrapper {
  private Component myMenuBar;
  private Component myCenterComponent;

  public void setMenuBar(consulo.ui.Component menuBar) {
    myMenuBar = (Component)menuBar;
    addComponent(myMenuBar);
  }

  public void setCenterComponent(Component centerComponent) {
    myCenterComponent = centerComponent;
    addComponent(myCenterComponent);
  }

  @Override
  public void replaceComponent(Component oldComponent, Component newComponent) {
    throw new UnsupportedOperationException();
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
  public Iterator<com.vaadin.ui.Component> iterator() {
    List<Component> list = new ArrayList<>(getComponentCount());
    if (myMenuBar != null) {
      list.add(myMenuBar);
    }
    if (myCenterComponent != null) {
      list.add(myCenterComponent);
    }
    return list.iterator();
  }

  @Nullable
  @Override
  public consulo.ui.Component getParentComponent() {
    return (consulo.ui.Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }
}
