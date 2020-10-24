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
package consulo.web.gwt.client.ui.border;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.util.GwtStyleUtil;
import consulo.web.gwt.shared.ui.state.border.BorderListState;
import consulo.web.gwt.shared.ui.state.border.BorderPosition;

/**
 * @author VISTALL
 * @since 15-Sep-17
 */
public class GwtBorderSetter {
  public static void set(Widget widget, BorderListState borderListState) {
    Element element = widget.getElement();

    // reset
    for (BorderPosition border : BorderPosition.values()) {
      String prefix = prefix(border);
      element.getStyle().setProperty("border" + prefix + "Color", null);
      element.getStyle().setProperty("border" + prefix + "Style", null);
      element.getStyle().setProperty("border" + prefix + "Width", null);
    }

    for (BorderListState.BorderState border : borderListState.myBorders) {
      String prefix = prefix(border.myPosition);

      String color = null;
      String style = null;
      switch (border.myStyle) {
        case LINE:
          style = "solid";
          color = GwtStyleUtil.toString(border.myColor);
          break;
        case EMPTY:
          style = "solid";
          color = "transparent";
          break;
      }

      element.getStyle().setProperty("border" + prefix + "Color", color);
      element.getStyle().setProperty("border" + prefix + "Style", style);
      element.getStyle().setProperty("border" + prefix + "Width", border.myWidth + "px");
    }
  }

  private static String prefix(BorderPosition position) {
    String prefix = null;
    switch (position) {
      case TOP:
        prefix = "Top";
        break;
      case BOTTOM:
        prefix = "Bottom";
        break;
      case LEFT:
        prefix = "Left";
        break;
      case RIGHT:
        prefix = "Right";
        break;
    }
    return prefix;
  }
}
