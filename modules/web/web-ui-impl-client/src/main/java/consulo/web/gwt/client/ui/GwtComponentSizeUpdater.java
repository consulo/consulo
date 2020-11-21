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
package consulo.web.gwt.client.ui;

import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.AbstractComponentState;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class GwtComponentSizeUpdater {
  public static void updateForLayout(AbstractComponentConnector connector) {
    update(connector, "100%");
  }

  public static void updateForComponent(AbstractComponentConnector connector) {
    update(connector, null);
  }

  private static void update(AbstractComponentConnector connector, String defaultValue) {
    AbstractComponentState state = connector.getState();

    Widget widget = connector.getWidget();

    String width = state.width;
    if (width != null && width.length() > 0) {
      widget.setWidth(width);
    }
    else {
      widget.setWidth(defaultValue);
    }

    String height = state.height;
    if (height != null && height.length() > 0) {
      widget.setHeight(height);
    }
    else {
      widget.setHeight(defaultValue);
    }
  }
}
