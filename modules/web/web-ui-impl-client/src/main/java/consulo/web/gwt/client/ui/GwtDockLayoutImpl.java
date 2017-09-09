/*
 * Copyright 2013-2016 consulo.io
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

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.UIConverter;
import consulo.web.gwt.client.WebSocketProxy;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.UIComponent;
import consulo.web.gwt.shared.state.UIComponentState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class GwtDockLayoutImpl extends DockPanel implements InternalGwtComponentWithChildren<UIComponentState> {
  public GwtDockLayoutImpl() {
    GwtUIUtil.fill(this);
    setHorizontalAlignment(ALIGN_LEFT);
    setVerticalAlignment(ALIGN_TOP);
  }

  @Override
  public void updateState(@NotNull Map<String, Object> map) {
  }

  @Override
  public void addChildren(WebSocketProxy proxy, List<UIComponent.Child> children) {
    for (UIComponent.Child child : children) {
      final Map<String, Object> variables = child.getVariables();

      final Object side = variables.get("side");
      DockLayoutConstant direction = null;
      if (side.equals("top")) {
        direction = NORTH;
      }
      else if (side.equals("bottom")) {
        direction = SOUTH;
      }
      else if (side.equals("left")) {
        direction = WEST;
      }
      else if (side.equals("right")) {
        direction = EAST;
      }
      else if (side.equals("center")) {
        direction = CENTER;
      }

      final Widget widget = UIConverter.create(proxy, child.getComponent()).asWidget();
      widget.setWidth("100%");
      add(widget, direction);
    }
  }
}
