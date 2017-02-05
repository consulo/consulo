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

import com.intellij.util.containers.hash.LinkedHashMap;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Tab;
import consulo.ui.TabbedLayout;
import consulo.web.gwtUI.shared.UIComponent;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtTabbedLayoutImpl extends WGwtBaseComponent implements TabbedLayout {
  private Map<Tab, WGwtBaseComponent> myTabs = new LinkedHashMap<Tab, WGwtBaseComponent>();
  private int mySelected = 0;

  @Override
  public void registerComponent(TLongObjectHashMap<WGwtBaseComponent> map) {
    super.registerComponent(map);
    for (WGwtBaseComponent component : myTabs.values()) {
      component.registerComponent(map);
    }
  }

  @Override
  public void visitChanges(List<UIComponent> components) {
    super.visitChanges(components);

    for (WGwtBaseComponent component : myTabs.values()) {
      component.visitChanges(components);
    }
  }

  @Override
  protected void getState(Map<String, Serializable> map) {
    super.getState(map);
    map.put("selected", mySelected);
  }

  @Override
  protected void initChildren(List<UIComponent.Child> children) {
    for (Map.Entry<Tab, WGwtBaseComponent> entry : myTabs.entrySet()) {
      // send tab
      UIComponent.Child child = new UIComponent.Child();
      child.setComponent(((WGwtTabImpl)entry.getKey()).getLayout().convert());
      children.add(child);

      // send tab content
      child = new UIComponent.Child();
      child.setComponent(entry.getValue().convert());
      children.add(child);
    }
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public TabbedLayout addTab(@NotNull Tab tab, @NotNull Component component) {
    myTabs.put(tab, (WGwtBaseComponent)component);
    markAsChanged();
    return this;
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public TabbedLayout addTab(@NotNull String tabName, @NotNull Component component) {
    Tab presentation = new WGwtTabImpl();
    presentation.append(tabName);
    return addTab(presentation, component);
  }
}
