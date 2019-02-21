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

import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.ui.GwtLayoutConnector;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.ex.state.toolWindow.ToolWindowStripeState;

import java.util.List;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
@Connect(canonicalName = "consulo.web.ui.ex.WebToolWindowStripeImpl.Vaadin")
public class GwtToolWindowStripeConnector extends GwtLayoutConnector {
  @Override
  public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent connectorHierarchyChangeEvent) {
    getWidget().removeAll();

    List<Widget> widgets = GwtUIUtil.remapWidgets(this);

    for (Widget widget : widgets) {
      getWidget().addButton((GwtToolWindowStripeButton)widget);
    }
  }

  @Override
  public GwtToolWindowStripe getWidget() {
    return (GwtToolWindowStripe)super.getWidget();
  }

  @Override
  public ToolWindowStripeState getState() {
    return (ToolWindowStripeState)super.getState();
  }
}
