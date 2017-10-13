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
package consulo.ui.ex;

import com.vaadin.shared.communication.SharedState;
import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Component;
import consulo.web.gwt.shared.ui.ex.state.toolWindow.ToolWindowStripeState;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class WGwtToolWindowStripe extends AbstractComponentContainer {
  private final List<Component> myButtons = new ArrayList<>();

  private final DockLayoutState.Constraint myConstraint;

  public WGwtToolWindowStripe(DockLayoutState.Constraint constraint) {
    myConstraint = constraint;
  }

  public void addButton(WGwtToolWindowStripeButton button, Comparator<ToolWindowStripeButton> comparator) {
    myButtons.add(button);

    myButtons.sort((o1, o2) -> comparator.compare((ToolWindowStripeButton)o1, (ToolWindowStripeButton)o2));

    addComponent(button);
  }

  @Override
  protected ToolWindowStripeState getState() {
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
