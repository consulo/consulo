/*
 * Copyright 2013-2016 must-be.org
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

import consulo.ui.Component;
import consulo.ui.layout.DockLayout;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIEventFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtDockPanelImpl extends WGwtComponentImpl implements DockLayout {
  private Map<String, WGwtComponentImpl> myComponents = new HashMap<String, WGwtComponentImpl>();

  @Override
  public void registerComponent(Map<String, WGwtComponentImpl> map) {
    super.registerComponent(map);
    for (WGwtComponentImpl component : myComponents.values()) {
      component.registerComponent(map);
    }
  }

  @NotNull
  @Override
  public DockLayout top(@NotNull Component component) {
    myComponents.put("top", (WGwtComponentImpl)component);
    return this;
  }

  @NotNull
  @Override
  public DockLayout bottom(@NotNull Component component) {
    myComponents.put("bottom", (WGwtComponentImpl)component);
    return this;
  }

  @NotNull
  @Override
  public DockLayout center(@NotNull Component component) {
    myComponents.put("center", (WGwtComponentImpl)component);
    return this;
  }

  @NotNull
  @Override
  public DockLayout left(@NotNull Component component) {
    myComponents.put("left", (WGwtComponentImpl)component);
    return this;
  }

  @NotNull
  @Override
  public DockLayout right(@NotNull Component component) {
    myComponents.put("right", (WGwtComponentImpl)component);
    return this;
  }

  @Override
  protected void initChildren(UIEventFactory factory, List<UIComponent.Child> children) {
    for (Map.Entry<String, WGwtComponentImpl> entry : myComponents.entrySet()) {
      final UIComponent.Child child = factory.componentChild().as();

      final UIComponent uiComponent = entry.getValue().convert(factory);
      child.setComponent(uiComponent);

      Map<String, String> vars = new HashMap<String, String>();
      vars.put("side", entry.getKey());

      child.setVariables(vars);

      children.add(child);
    }
  }
}
