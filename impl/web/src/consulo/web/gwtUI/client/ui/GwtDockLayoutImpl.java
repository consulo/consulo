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
package consulo.web.gwtUI.client.ui;

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.UIObject;
import consulo.web.gwtUI.client.UIConverter;
import consulo.web.gwtUI.client.WebSocketProxy;
import consulo.web.gwtUI.client.util.GwtUIUtil2;
import consulo.web.gwtUI.shared.UIComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class GwtDockLayoutImpl extends DockPanel implements InternalGwtComponent {
  public GwtDockLayoutImpl() {
    GwtUIUtil2.fill(this);
    setHorizontalAlignment(ALIGN_LEFT);
    setVerticalAlignment(ALIGN_TOP);
  }

  @Override
  public void updateState(@NotNull Map<String, String> map) {
    DefaultVariables.updateState(map, this);
  }

  @Override
  public void addChildren(WebSocketProxy proxy, UIComponent.Child child) {
    final Map<String, String> variables = child.getVariables();

    final String side = variables.get("side");
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

    final InternalGwtComponent widget = UIConverter.create(proxy, child.getComponent());
    GwtUIUtil2.fill((UIObject)widget);
    add(widget, direction);
  }
}
