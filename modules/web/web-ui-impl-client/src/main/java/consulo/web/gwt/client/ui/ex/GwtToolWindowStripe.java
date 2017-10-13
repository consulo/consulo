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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class GwtToolWindowStripe extends SimplePanel {
  private final Map<GwtToolWindowStripeButton, GwtInternalDecorator> myInternalDecorators = new HashMap<>();
  private final List<GwtToolWindowStripeButton> myButtons = new ArrayList<>();
  private DockLayoutState.Constraint myPosition;

  public GwtToolWindowStripe() {
  }

  public void setPosition(DockLayoutState.Constraint position) {
    myInternalDecorators.clear();
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

  public void addButton(GwtToolWindowStripeButton button) {
    GwtToolWindowStripeInner inner = (GwtToolWindowStripeInner)getWidget();

    myButtons.add(button);

    inner.add(button, false);

    myInternalDecorators.put(button, new GwtInternalDecorator(button));

    if (myPosition == DockLayoutState.Constraint.LEFT || myPosition == DockLayoutState.Constraint.RIGHT) {
      button.setWidth("22px");
      button.setVerticalText();
    }
    else if (myPosition == DockLayoutState.Constraint.TOP || myPosition == DockLayoutState.Constraint.BOTTOM) {
      button.setHeight("22px");
    }
  }
}
