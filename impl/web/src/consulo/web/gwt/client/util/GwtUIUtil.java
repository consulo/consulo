/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwt.client.util;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.ui.WidgetWithUpdateUI;

import java.util.List;

/**
 * @author VISTALL
 * @since 20-May-16
 */
public class GwtUIUtil {
  public static Widget loadingPanel() {
    VerticalPanel verticalPanel = fillAndReturn(new VerticalPanel());
    verticalPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    verticalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

    verticalPanel.add(new Label("Loading..."));

    return verticalPanel;
  }

  public static Widget getWidget(Element element) {
    EventListener listener = DOM.getEventListener(element);

    if (listener == null) {
      return null;
    }
    if (listener instanceof Widget) {
      return (Widget)listener;
    }
    return null;
  }

  public static Widget icon(List<String> icons) {
    FlowPanel panel = new FlowPanel();
    panel.setStyleName("imageWrapper");

    for (String icon : icons) {
      Image image = new Image("/icon?path=\"" + icon + "\"");
      image.setStyleName("overlayImage");

      panel.add(image);
    }
    return panel;
  }

  public static void updateUI(Widget widget) {
    if (widget instanceof WidgetWithUpdateUI) {
      ((WidgetWithUpdateUI)widget).updateUI();
    }

    if (widget instanceof HasWidgets) {
      for (Widget child : (HasWidgets)widget) {
        updateUI(child);
      }
    }

    if (widget instanceof Grid) {
      Grid grid = (Grid)widget;
      for (int c = 0; c < grid.getColumnCount(); c++) {
        for (int r = 0; r < grid.getRowCount(); r++) {
          Widget temp = grid.getWidget(r, c);

          if (temp != null) {
            updateUI(temp);
          }
        }
      }
    }
  }

  public static <T extends UIObject> T fillAndReturn(T object) {
    object.setWidth("100%");
    object.setHeight("100%");
    return object;
  }

  public static void fill(UIObject object) {
    object.setWidth("100%");
    object.setHeight("100%");
  }
}
