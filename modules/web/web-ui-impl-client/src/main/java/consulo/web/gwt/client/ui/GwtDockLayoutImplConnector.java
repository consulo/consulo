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

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.ui.border.GwtBorderSetter;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

import java.util.List;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
@Connect(canonicalName = "consulo.ui.web.internal.layout.WebDockLayoutImpl.Vaadin")
public class GwtDockLayoutImplConnector extends GwtLayoutConnector {
  @Override
  public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent connectorHierarchyChangeEvent) {
    GwtDockLayoutImpl dockLayout = getWidget();
    dockLayout.clear();

    boolean noCenterElement = true;
    List<ComponentConnector> childComponents = getChildComponents();
    for (int i = 0; i < childComponents.size(); i++) {
      ComponentConnector connector = childComponents.get(i);
      DockLayoutState.Constraint constraint = getState().myConstraints.get(i);

      Widget widget = GwtUIUtil.fillAndReturn(connector.getWidget());
      switch (constraint) {
        case TOP:
          dockLayout.add(widget, GwtDockLayoutImpl.NORTH);
          break;
        case BOTTOM:
          dockLayout.add(widget, GwtDockLayoutImpl.SOUTH);
          break;
        case LEFT:
          dockLayout.add(widget, GwtDockLayoutImpl.WEST);
          break;
        case RIGHT:
          dockLayout.add(widget, GwtDockLayoutImpl.EAST);
          break;
        case CENTER:
          setCenterWidget(dockLayout, widget);
          noCenterElement = false;
          break;
      }
    }

    if (noCenterElement) {
      setCenterWidget(dockLayout, new SimplePanel());
    }
  }

  private static void setCenterWidget(GwtDockLayoutImpl dockLayout, Widget widget) {
    dockLayout.add(widget, GwtDockLayoutImpl.CENTER);
    dockLayout.setCellHeight(widget, "100%");
    dockLayout.setCellWidth(widget, "100%");
  }

  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);

    GwtBorderSetter.set(getWidget(), getState().myBorderListState);
  }

  @Override
  public DockLayoutState getState() {
    return (DockLayoutState)super.getState();
  }

  @Override
  public GwtDockLayoutImpl getWidget() {
    return (GwtDockLayoutImpl)super.getWidget();
  }
}
