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

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.ui.image.ImageConverter;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
public class GwtToolWindowStripeButton extends SimplePanel {
  private boolean mySelected;
  private boolean myFocus;
  private Boolean myVerticalValue;

  private Runnable myClickListener;

  public GwtToolWindowStripeButton() {
    sinkEvents(Event.ONCLICK | Event.ONFOCUS);

    addDomHandler(event -> {
      myFocus = true;
      updateBackgroundColor();
    }, MouseOverEvent.getType());

    addDomHandler(event -> {
      myFocus = false;
      updateBackgroundColor();
    }, MouseOutEvent.getType());

    addDomHandler(event -> {
      if (myClickListener != null) {
        myClickListener.run();
      }
    }, ClickEvent.getType());

    doLayout();
  }

  public void doLayout() {
    HorizontalPanel widget = new HorizontalPanel();
    widget.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    widget.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    widget.setHeight("100%");

    setWidget(widget);

    if (isVertical()) {
      setVerticalText();
    }
  }

  private void updateBackgroundColor() {
    if (mySelected) {
      getElement().getStyle().setBackgroundColor("rgba(85,85,85, 0.33333334)");
      return;
    }

    if (myFocus) {
      getElement().getStyle().setBackgroundColor("rgba(85,85,85, 0.15686275)");
      return;
    }

    getElement().getStyle().setBackgroundColor(null);
  }

  public void setClickListener(@Nullable Runnable clickListener) {
    myClickListener = clickListener;
  }

  public void build(String text, @Nullable MultiImageState imageState) {
    CellPanel cell = (CellPanel)getWidget();

    CellPanel panel = isVertical() ? new VerticalPanel() : new HorizontalPanel();

    List<Character> list = new ArrayList<>(text.length());
    for (char c : text.toCharArray()) {
      list.add(c);
    }

    if (isVertical()) {
      Collections.reverse(list);
    }

    for (char c : list) {
      HTML child = new HTML(c == ' ' ? "&nbsp;" : String.valueOf(c));
      if (isVertical()) {
        child.getElement().getStyle().setProperty("transform", "rotate(-90deg)");
        child.getElement().getStyle().setProperty("lineHeight", "0.6em");
      }
      panel.add(child);
    }

    getElement().getStyle().clearPaddingLeft();
    getElement().getStyle().clearPaddingRight();
    getElement().getStyle().clearPaddingTop();
    getElement().getStyle().clearPaddingBottom();

    if (isVertical()) {
      getElement().getStyle().setPaddingTop(10, Style.Unit.PX);
      getElement().getStyle().setPaddingBottom(10, Style.Unit.PX);

      cell.add(panel);

      if (imageState != null) {
        panel.getElement().getStyle().setMarginBottom(5, Style.Unit.PX);

        Widget widget = ImageConverter.create(imageState);
        cell.add(widget);
      }
    }
    else {
      getElement().getStyle().setPaddingLeft(10, Style.Unit.PX);
      getElement().getStyle().setPaddingRight(10, Style.Unit.PX);

      if (imageState != null) {
        panel.getElement().getStyle().setMarginLeft(5, Style.Unit.PX);

        Widget widget = ImageConverter.create(imageState);
        cell.add(widget);
      }

      cell.add(panel);
    }
  }

  public void setVerticalText() {
    myVerticalValue = Boolean.TRUE;
    VerticalPanel panel = new VerticalPanel();
    panel.setWidth("100%");
    panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    setWidget(panel);
  }

  public boolean isVertical() {
    return myVerticalValue == Boolean.TRUE;
  }

  public void setSelected(boolean value) {
    mySelected = value;

    updateBackgroundColor();
  }

  public boolean isSelected() {
    return mySelected;
  }
}
