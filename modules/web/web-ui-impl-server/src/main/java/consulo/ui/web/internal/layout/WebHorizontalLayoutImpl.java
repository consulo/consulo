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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinComponentContainer;
import consulo.ui.web.internal.border.WebBorderBuilder;
import consulo.web.gwt.shared.ui.state.layout.BaseLayoutState;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebHorizontalLayoutImpl extends VaadinComponentDelegate<WebHorizontalLayoutImpl.Vaadin> implements HorizontalLayout {
  public static class Vaadin extends VaadinComponentContainer {
    private final List<Component> myChildren = new LinkedList<>();

    @Override
    public void beforeClientResponse(boolean initial) {
      super.beforeClientResponse(initial);

      WebBorderBuilder.fill(toUIComponent(), getState().myBorderListState);
    }

    @Override
    public BaseLayoutState getState() {
      return (BaseLayoutState)super.getState();
    }

    @Override
    public void removeComponent(Component c) {
      myChildren.remove(c);
      super.removeComponent(c);
    }

    @Override
    public int getComponentCount() {
      return myChildren.size();
    }

    @Override
    public Iterator<Component> iterator() {
      return myChildren.iterator();
    }

    public void add(@Nonnull Component component) {
      addComponent(component);
      myChildren.add(component);
      markAsDirtyRecursive();
    }
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public HorizontalLayout add(@Nonnull consulo.ui.Component component) {
    getVaadinComponent().add(TargetVaddin.to(component));
    return this;
  }
}
