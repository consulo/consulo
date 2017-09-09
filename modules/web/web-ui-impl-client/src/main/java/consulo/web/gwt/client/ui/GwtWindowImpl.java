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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Grid;
import consulo.web.gwt.client.UIConverter;
import consulo.web.gwt.client.WebSocketProxy;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.UIComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class GwtWindowImpl extends Grid implements InternalGwtComponentWithChildren {
  public GwtWindowImpl() {
    super(2, 1);
    GwtUIUtil.fill(this);
  }

  @Override
  public void updateState(@NotNull Map<String, Object> map) {

  }

  @Override
  public void addChildren(WebSocketProxy proxy, List<UIComponent.Child> children) {
    handleComponents(proxy, this, children);
  }

  public static void handleComponents(WebSocketProxy proxy, Grid grid, List<UIComponent.Child> children) {
    int rows = 0;
    final UIComponent.Child menuChild = children.get(0);
    final UIComponent menuComponent = menuChild.getComponent();
    if (menuComponent != null) {
      rows++;
    }

    final UIComponent.Child contentChild = children.get(1);
    final UIComponent contentComponent = contentChild.getComponent();
    if (contentComponent != null) {
      rows++;
    }

    grid.resizeRows(rows);

    if (menuComponent != null) {
      final InternalGwtComponent component = UIConverter.create(proxy, menuComponent);

      grid.getRowFormatter().getElement(0).getStyle().setHeight(26, Style.Unit.PX);

      grid.setWidget(0, 0, component.asWidget());
    }

    if (contentComponent != null) {
      final InternalGwtComponent component = UIConverter.create(proxy, contentComponent);

      grid.setWidget(rows - 1, 0, component.asWidget());
    }
  }
}
