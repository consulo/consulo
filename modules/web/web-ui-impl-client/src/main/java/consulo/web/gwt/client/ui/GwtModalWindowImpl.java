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

import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.PopupPanel;
import consulo.web.gwt.client.WebSocketProxy;
import consulo.web.gwt.client.util.GwtUIUtil2;
import consulo.web.gwtUI.shared.UIComponent;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class GwtModalWindowImpl extends PopupPanel implements InternalGwtComponentWithChildren {
  private Grid myGrid = GwtUIUtil2.fillAndReturn(new Grid(2, 1));

  public GwtModalWindowImpl() {
    super(false, true);
    setWidget(myGrid);
    setGlassEnabled(true);
  }

  @Override
  public void addChildren(WebSocketProxy proxy, List<UIComponent.Child> children) {
    GwtWindowImpl.handleComponents(proxy, myGrid, children);
  }

  @Override
  public void updateState(@NotNull Map<String, Serializable> map) {
    final boolean visible = DefaultVariables.parseBoolAsTrue(map, "visible");

    if (visible) {
      center();
    }
    else {
      hide();
    }
  }
}
