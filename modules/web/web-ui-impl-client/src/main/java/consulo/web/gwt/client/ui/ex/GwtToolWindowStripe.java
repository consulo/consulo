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
package consulo.web.gwt.client.ui.ex;

import com.google.gwt.user.client.ui.SimplePanel;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class GwtToolWindowStripe extends SimplePanel {
  private final List<GwtToolWindowStripeButton> myButtons = new ArrayList<>();
  private DockLayoutState.Constraint myPosition;

  private GwtToolWindowPanel myToolWindowPanel;

  public GwtToolWindowStripe() {
  }

  public void assign(DockLayoutState.Constraint position, GwtToolWindowPanel toolWindowPanel) {
    myToolWindowPanel = toolWindowPanel;
    myButtons.clear();
    myPosition = position;

    switch (position) {
      case TOP:
      case BOTTOM:
        setWidget(new GwtToolWindowStripeInner(false));
        break;
      case LEFT:
      case RIGHT:
        setWidget(new GwtToolWindowStripeInner(true));
        break;
      default:
        throw new UnsupportedOperationException();
    }
  }

  public void removeAll() {
    myButtons.clear();

    getWidget().removeAll();

    myToolWindowPanel.doLayout();
  }

  @Override
  public GwtToolWindowStripeInner getWidget() {
    return (GwtToolWindowStripeInner)super.getWidget();
  }

  public void addButton(GwtToolWindowStripeButton button) {
    GwtToolWindowStripeInner inner = getWidget();

    myButtons.add(button);

    inner.add(button, false);


    if (myPosition == DockLayoutState.Constraint.LEFT || myPosition == DockLayoutState.Constraint.RIGHT) {
      button.setWidth("22px");
      button.setVerticalText();
    }
    else if (myPosition == DockLayoutState.Constraint.TOP || myPosition == DockLayoutState.Constraint.BOTTOM) {
      button.setHeight("22px");
    }

    myToolWindowPanel.doLayout();
  }

  public DockLayoutState.Constraint getPosition() {
    return myPosition;
  }

  public boolean canShow() {
    return !myButtons.isEmpty();
  }
}
