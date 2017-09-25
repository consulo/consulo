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

import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.StaticPosition;
import consulo.ui.internal.VaadinWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WGwtToolWindowPanel extends AbstractComponentContainer implements consulo.ui.Component, VaadinWrapper {
  private final Map<StaticPosition, Component> myComponents = new LinkedHashMap<>();

  public WGwtToolWindowPanel() {
    for (StaticPosition position : StaticPosition.values()) {
      if(position == StaticPosition.CENTER) {
        add(position, new WGwtToolWindowCenterPanel());
      }
      else {
        add(position, new WGwtToolWindowStripe());
      }
    }
  }

  private void add(StaticPosition position, Component component) {
    myComponents.put(position, component);

    addComponent(component);
  }

  @Override
  public void replaceComponent(Component oldComponent, Component newComponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getComponentCount() {
    return myComponents.size();
  }

  @Override
  public Iterator<Component> iterator() {
    return myComponents.values().iterator();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {
  }
}
