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
package consulo.ui.web.internal.layout;

import com.vaadin.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.layout.WrappedLayout;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.UIComponentWithVaadinComponent;
import consulo.ui.web.internal.base.VaadinComponentContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public class WebWrappedLayoutImpl extends UIComponentWithVaadinComponent<WebWrappedLayoutImpl.Vaadin> implements WrappedLayout {
  protected static class Vaadin extends VaadinComponentContainer<WebWrappedLayoutImpl> {
    private Component myComponent;

    Vaadin(WebWrappedLayoutImpl component) {
      super(component);
    }

    private void set(@Nullable Component component) {
      if (myComponent != null) {
        removeComponent(myComponent);
      }

      myComponent = component;

      if (component != null) {
        addComponent(component);
      }
    }

    private void removeIfEqual(Component component) {
      if (myComponent == component) {
        set(null);
      }
    }

    @Override
    public int getComponentCount() {
      return myComponent == null ? 0 : 1;
    }

    @Override
    public Iterator<Component> iterator() {
      return myComponent == null ? Collections.<Component>emptyList().iterator() : Arrays.asList(myComponent).iterator();
    }
  }

  public WebWrappedLayoutImpl() {
    myVaadinComponent = new Vaadin(this);
  }

  @RequiredUIAccess
  @Override
  public void removeAll() {
    myVaadinComponent.set(null);
  }

  @Override
  public void remove(@Nonnull consulo.ui.Component component) {
    myVaadinComponent.removeIfEqual(TargetVaddin.to(component));
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public WrappedLayout set(@Nullable consulo.ui.Component component) {
    myVaadinComponent.set(TargetVaddin.to(component));
    return this;
  }
}
