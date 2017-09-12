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
package consulo.web.gwt.client.ui;

import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.StyleConstants;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
@Connect(canonicalName = "consulo.ui.internal.WGwtTabbedLayoutImpl")
public class GwtTabbedLayoutImplConnector extends GwtLayoutConnector {
  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  public GwtTabbedLayoutImpl getWidget() {
    return (GwtTabbedLayoutImpl)super.getWidget();
  }

  @Override
  public TabbedLayoutState getState() {
    return (TabbedLayoutState)super.getState();
  }

  @Override
  public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent connectorHierarchyChangeEvent) {
    Map<TabbedLayoutState.TabState, Widget> map = new LinkedHashMap<>();
    List<Widget> widgets = GwtUIUtil.remapWidgets(this);
    for (int i = 0; i < widgets.size(); i++) {
      map.put(getState().myTabStates.get(i), widgets.get(i));
    }
    getWidget().setTabs(getState().mySelected, map);
  }
}
