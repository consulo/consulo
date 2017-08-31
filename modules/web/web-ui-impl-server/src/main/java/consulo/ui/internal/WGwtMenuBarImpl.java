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

import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.web.gwt.shared.UIComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtMenuBarImpl extends WGwtBaseComponent implements MenuBar {
  private List<MenuItem> myMenuItems = new ArrayList<MenuItem>();

  @NotNull
  @Override
  public MenuBar add(@NotNull MenuItem menuItem) {
    myMenuItems.add(menuItem);
    return this;
  }

  @Override
  protected void initChildren(List<UIComponent.Child> children) {
    for (MenuItem menuItem : myMenuItems) {
      final UIComponent.Child child = new UIComponent.Child();

      final UIComponent uiComponent = ((WGwtBaseComponent)menuItem).convert();
      child.setComponent(uiComponent);

      children.add(child);
    }
  }
}
