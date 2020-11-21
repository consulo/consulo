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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.StyleConstants;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.ui.GwtComponentSizeUpdater;
import consulo.web.gwt.client.util.GwtUIUtil;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
@Connect(canonicalName = "consulo.ui.web.internal.ex.WebLoadingPanelImpl.Vaadin")
public class GwtLoadingPanelImplConnector extends AbstractComponentConnector {
  @Override
  protected void updateComponentSize() {
    GwtComponentSizeUpdater.updateForComponent(this);
  }

  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  protected Widget createWidget() {
    return createPanel();
  }

  public static Widget createPanel() {
    // http://tobiasahlin.com/spinkit/
    // MIT
    FlowPanel flowPanel = new FlowPanel();
    flowPanel.addStyleName("sk-cube-grid");

    for (int i = 1; i <= 9; i++) {
      FlowPanel child = new FlowPanel();
      child.addStyleName("sk-cube sk-cube" + i);
      flowPanel.add(child);
    }

    FlowPanel container = GwtUIUtil.fillAndReturn(new FlowPanel());
    container.getElement().getStyle().setProperty("display", "flex");
    container.getElement().getStyle().setProperty("justifyContent", "center");

    flowPanel.getElement().getStyle().setProperty("alignSelf", "center");
    container.add(flowPanel);
    return container;
  }
}
