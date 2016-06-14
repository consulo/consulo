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
import consulo.ui.MenuBar;
import consulo.ui.Window;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIEventFactory;
import gnu.trove.TLongObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtWindowImpl extends WBaseGwtComponent implements Window {
  private WBaseGwtComponent myContent;
  private MenuBar myMenuBar;

  @Override
  public void setContent(@NotNull Component content) {
    if (myContent != null) {
      throw new IllegalArgumentException();
    }
    myContent = (WBaseGwtComponent)content;
  }

  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {
    myMenuBar = menuBar;
  }

  @Override
  protected void initChildren(UIEventFactory factory, List<UIComponent.Child> children) {
    // add menu bar always
    UIComponent.Child menuChild = factory.componentChild().as();
    if(myMenuBar != null) {
      menuChild.setComponent(((WBaseGwtComponent)myMenuBar).convert(factory));
    }
    children.add(menuChild);

    UIComponent.Child contentChild = factory.componentChild().as();
    if(myContent != null) {
      contentChild.setComponent(myContent.convert(factory));
    }
    children.add(contentChild);
  }

  @Override
  public void registerComponent(TLongObjectHashMap<WBaseGwtComponent> map) {
    super.registerComponent(map);

    if (myContent != null) {
      myContent.registerComponent(map);
    }
  }

  @Override
  public void visitChanges(List<UIComponent> components) {
    super.visitChanges(components);

    if (myContent != null) {
      myContent.visitChanges(components);
    }
  }
}
