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

import com.intellij.util.SmartList;
import consulo.ui.Component;
import consulo.ui.RequiredUIThread;
import consulo.ui.layout.VerticalLayout;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIEventFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WGwtVerticalLayoutImpl extends WGwtComponentImpl implements VerticalLayout {
  private List<WGwtComponentImpl> myComponents = new SmartList<WGwtComponentImpl>();

  @Override
  public void registerComponent(Map<String, WGwtComponentImpl> map) {
    super.registerComponent(map);
    for (WGwtComponentImpl component : myComponents) {
      component.registerComponent(map);
    }
  }

  @Override
  protected void initChildren(UIEventFactory factory, List<UIComponent.Child> children) {
    for (WGwtComponentImpl component : myComponents) {
      final UIComponent.Child child = factory.componentChild().as();

      final UIComponent uiComponent = component.convert(factory);
      child.setComponent(uiComponent);

      children.add(child);
    }
  }

  @NotNull
  @Override
  @RequiredUIThread
  public VerticalLayout add(@NotNull Component component) {
    myComponents.add((WGwtComponentImpl)component);
    return this;
  }
}
