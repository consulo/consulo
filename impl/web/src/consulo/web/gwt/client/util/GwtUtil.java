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

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.service.FetchService;
import consulo.web.gwt.client.ui.WidgetWithUpdateUI;
import consulo.web.gwt.shared.GwtTransportService;
import consulo.web.gwt.shared.GwtTransportServiceAsync;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-May-16
 */
public class GwtUtil {
  private static final GwtTransportServiceAsync ourAsyncService = GWT.create(GwtTransportService.class);

  private static final Map<String, FetchService> ourServices = new HashMap<String, FetchService>();

  public static GwtTransportServiceAsync rpc() {
    return ourAsyncService;
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
      for (Widget child : (HasWidgets) widget) {
        updateUI(child);
      }
    }

    if (widget instanceof Grid) {
      Grid grid = (Grid)widget;
      for (int c = 0; c < grid.getColumnCount(); c++) {
        for (int r = 0; r < grid.getRowCount(); r++) {
          Widget temp = grid.getWidget(r, c);

          if (temp != null) {
            GwtUtil.updateUI(temp);
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

  public static void put(String key, FetchService fetchService) {
    ourServices.put(key, fetchService);
  }

  @SuppressWarnings("unchecked")
  public static <T> T get(String key) {
    return (T)ourServices.get(key);
  }
}
