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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class GwtToolWindowStripeButton extends HorizontalPanel {
  private boolean myActive;
  private DockLayoutState.Constraint myConstraint;

  public GwtToolWindowStripeButton(String labelText, DockLayoutState.Constraint constraint, GwtToolWindowPanel toolWindowPanel) {
    myConstraint = constraint;
    Label label = GwtUIUtil.fillAndReturn(new Label(labelText));
    label.setHorizontalAlignment(ALIGN_CENTER);
    add(label);

    // too dirty hack. most browsers may not support that
    getElement().getStyle().setProperty("writingMode", "vertical-rl");
    getElement().getStyle().setProperty("transform", "rotate(180deg)");

    sinkEvents(Event.ONCLICK);

    addDomHandler(event -> {
      toolWindowPanel.showOrHide(labelText);
    }, ClickEvent.getType());
  }

  public DockLayoutState.Constraint getPosition() {
    return myConstraint;
  }

  public void setActive(boolean value) {
    myActive = value;

    getElement().getStyle().setBackgroundColor(value ? "lightGray" : null);
  }

  public boolean isActive() {
    return myActive;
  }
}
