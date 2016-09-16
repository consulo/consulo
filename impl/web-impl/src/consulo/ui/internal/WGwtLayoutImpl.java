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

import com.intellij.codeInspection.SmartHashMap;
import com.intellij.util.containers.hash.LinkedHashMap;
import consulo.ui.Component;
import consulo.web.gwtUI.shared.UIComponent;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class WGwtLayoutImpl<C> extends WGwtBaseComponent {
  private Map<C, WGwtBaseComponent> myComponents = new LinkedHashMap<C, WGwtBaseComponent>();

  @Override
  public void registerComponent(TLongObjectHashMap<WGwtBaseComponent> map) {
    super.registerComponent(map);

    for (WGwtBaseComponent component : myComponents.values()) {
      component.registerComponent(map);
    }
  }

  @Override
  protected void initChildren(List<UIComponent.Child> children) {
    for (Map.Entry<C, WGwtBaseComponent> entry : myComponents.entrySet()) {
      final UIComponent.Child child = new UIComponent.Child();

      final UIComponent uiComponent = entry.getValue().convert();
      child.setComponent(uiComponent);

      Map<String, Serializable> vars = new SmartHashMap<String, Serializable>();

      convertConstraint(vars, entry.getKey());

      if (!vars.isEmpty()) {
        child.setVariables(vars);
      }

      children.add(child);
    }
  }

  protected WGwtBaseComponent addChild(@NotNull WGwtBaseComponent component, C constraint) {
    final Component parentComponent = component.getParentComponent();
    if(parentComponent != null) {
      throw new IllegalArgumentException("We can't change child");
    }

    component.setParentComponent(this);
    return myComponents.put(constraint, component);
  }

  protected void convertConstraint(Map<String, Serializable> map, C constraint) {
  }

  @Override
  public void visitChanges(List<UIComponent> components) {
    super.visitChanges(components);

    for (WGwtBaseComponent component : myComponents.values()) {
      component.visitChanges(components);
    }
  }
}
