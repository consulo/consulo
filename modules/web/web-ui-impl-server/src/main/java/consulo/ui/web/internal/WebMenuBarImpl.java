/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.web.internal;

import com.vaadin.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.MenuItem;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinComponentContainer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebMenuBarImpl extends VaadinComponentDelegate<WebMenuBarImpl.Vaadin> implements MenuBar {
  public static class Vaadin extends VaadinComponentContainer {
    private List<Component> myMenuItems = new ArrayList<>();

    public void clear() {
      for (Component menuItem : myMenuItems) {
        removeComponent(menuItem);
      }
      myMenuItems.clear();
    }

    public void add(@Nonnull MenuItem menuItem) {
      myMenuItems.add(TargetVaddin.to(menuItem));

      addComponent(TargetVaddin.to(menuItem));
    }

    @Override
    public void removeComponent(Component c) {
      super.removeComponent(c);
      myMenuItems.remove(c);
    }

    @Override
    public int getComponentCount() {
      return myMenuItems.size();
    }

    @Override
    public Iterator<Component> iterator() {
      return myMenuItems.iterator();
    }
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @Override
  public void clear() {
    getVaadinComponent().clear();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public MenuBar add(@Nonnull MenuItem menuItem) {
    getVaadinComponent().add(menuItem);
    return this;
  }
}
