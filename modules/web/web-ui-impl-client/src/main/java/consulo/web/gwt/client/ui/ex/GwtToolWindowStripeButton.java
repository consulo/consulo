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
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.ui.image.ImageConverter;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class GwtToolWindowStripeButton extends SimplePanel {
  private boolean myActive;
  private Label myLabel;
  private boolean myVertical;

  public GwtToolWindowStripeButton() {
    setWidget(new HorizontalPanel());
    sinkEvents(Event.ONCLICK | Event.ONFOCUS);

    addDomHandler(event -> {
      if (myActive) {
        return;
      }

      getElement().getStyle().setBackgroundColor("lightGray");
    }, MouseOverEvent.getType());

    addDomHandler(event -> {
      if (myActive) {
        return;
      }

      getElement().getStyle().setBackgroundColor(null);
    }, MouseOutEvent.getType());

    addDomHandler(event -> {
      // toolWindowPanel.showOrHide(labelText);
    }, ClickEvent.getType());
  }

  public void build(String text, @Nullable MultiImageState imageState) {
    CellPanel w = (CellPanel)getWidget();

    CellPanel panel = myVertical ? new VerticalPanel() : new HorizontalPanel();

    List<Character> list = new ArrayList<>(text.length());
    for (char c : text.toCharArray()) {
      list.add(c);
    }

    if (myVertical) {
      Collections.reverse(list);
    }

    for (char c : list) {
      Label child = new Label(String.valueOf(c));
      if(myVertical) {
        child.getElement().getStyle().setProperty("transform", "rotate(-90deg)");
        child.getElement().getStyle().setProperty("lineHeight", "10px");
      }
      panel.add(child);
    }
    myLabel = GwtUIUtil.fillAndReturn(new InlineLabel(text));
    myLabel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

    if (myVertical) {
      w.add(panel);

      if (imageState != null) {
        Widget widget = ImageConverter.create(imageState);
        w.add(widget);
      }
    }
    else {
      if (imageState != null) {
        Widget widget = ImageConverter.create(imageState);
        w.add(widget);
      }

      w.add(panel);
    }
  }

  public void setVerticalText() {
    myVertical = true;
    setWidget(new VerticalPanel());
  }

  public void setActive(boolean value) {
    myActive = value;

    getElement().getStyle().setBackgroundColor(value ? "lightGray" : null);
  }

  public boolean isActive() {
    return myActive;
  }
}
