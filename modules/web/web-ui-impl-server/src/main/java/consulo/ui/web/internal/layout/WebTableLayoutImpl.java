/*
 * Copyright 2013-2020 consulo.io
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
import consulo.ui.layout.TableLayout;
import consulo.ui.StaticPosition;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinComponentContainer;
import consulo.web.gwt.shared.ui.state.layout.TableLayoutState;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 2020-08-25
 */
public class WebTableLayoutImpl extends VaadinComponentDelegate<WebTableLayoutImpl.Vaadin> implements TableLayout {

  public static class Vaadin extends VaadinComponentContainer {
    private final Map<Component, TableLayoutState.TableCell> myChildren = new LinkedHashMap<>();

    public void add(@Nonnull Component component, TableLayoutState.TableCell cell) {
      addComponent(component);

      myChildren.put(component, cell);

      markAsDirtyRecursive();
    }

    @Override
    public void beforeClientResponse(boolean initial) {
      super.beforeClientResponse(initial);

      getState().myConstraints.clear();

      for (TableLayoutState.TableCell tableCell : myChildren.values()) {
        getState().myConstraints.add(tableCell);
      }
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
      return myChildren.keySet().iterator();
    }

    @Override
    public TableLayoutState getState() {
      return (TableLayoutState)super.getState();
    }
  }

  public WebTableLayoutImpl(StaticPosition fillOption) {
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public TableLayout add(@Nonnull consulo.ui.Component component, @Nonnull TableCell tableCell) {
    TableLayoutState.TableCell cellState = new TableLayoutState.TableCell(tableCell.getRow(), tableCell.getColumn(), tableCell.isFill());
    toVaadinComponent().add(TargetVaddin.to(component), cellState);
    return this;
  }
}
