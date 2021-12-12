/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.ui.ex;

import com.vaadin.shared.communication.SharedState;
import com.vaadin.ui.Component;
import consulo.ui.ex.ToolWindowStripeButton;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinComponentContainer;
import consulo.web.gwt.shared.ui.ex.state.toolWindow.ToolWindowStripeState;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WebToolWindowStripeImpl extends VaadinComponentDelegate<WebToolWindowStripeImpl.Vaadin> {
  public static class Vaadin extends VaadinComponentContainer {
    private DockLayoutState.Constraint myConstraint = DockLayoutState.Constraint.CENTER;
    private final List<Component> myButtons = new ArrayList<>();

    @Override
    public ToolWindowStripeState getState() {
      return (ToolWindowStripeState)super.getState();
    }

    @Override
    protected ToolWindowStripeState getState(boolean markAsDirty) {
      return (ToolWindowStripeState)super.getState(markAsDirty);
    }

    @Override
    protected SharedState createState() {
      ToolWindowStripeState state = new ToolWindowStripeState();
      state.myConstraint = myConstraint;
      return state;
    }

    @Override
    public void replaceComponent(Component component, Component component1) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getComponentCount() {
      return myButtons.size();
    }

    @Override
    public Iterator<Component> iterator() {
      return myButtons.iterator();
    }
  }

  public WebToolWindowStripeImpl(DockLayoutState.Constraint constraint) {
    getVaadinComponent().myConstraint = constraint;
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  public void markAsDirtyRecursive() {
    getVaadinComponent().markAsDirtyRecursive();
  }

  public void addButton(ToolWindowStripeButton button, Comparator<ToolWindowStripeButton> comparator) {
    Vaadin vaadinComponent = getVaadinComponent();

    vaadinComponent.myButtons.add(TargetVaddin.to(button.getComponent()));

    vaadinComponent.myButtons.sort((o1, o2) -> {
      ToolWindowStripeButton v1 = (ToolWindowStripeButton)TargetVaddin.from(o1);
      ToolWindowStripeButton v2 = (ToolWindowStripeButton)TargetVaddin.from(o2);
      return comparator.compare(v1, v2);
    });

    vaadinComponent.addComponent(TargetVaddin.to(button.getComponent()));
  }
}
