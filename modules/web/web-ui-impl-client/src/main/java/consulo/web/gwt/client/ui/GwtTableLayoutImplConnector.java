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
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.layout.TableLayoutState;

import java.util.List;

/**
 * @author VISTALL
 * @since 2020-08-25
 */
@Connect(canonicalName = "consulo.ui.web.internal.layout.WebTableLayoutImpl.Vaadin")
public class GwtTableLayoutImplConnector extends GwtLayoutConnector {
  @Override
  public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent connectorHierarchyChangeEvent) {
    List<Widget> widgets = GwtUIUtil.remapWidgets(this);
    List<TableLayoutState.TableCell> constraints = getState().myConstraints;

    getWidget().setComponents(widgets, constraints);
  }

  @Override
  public GwtTableLayoutImpl getWidget() {
    return (GwtTableLayoutImpl)super.getWidget();
  }

  @Override
  public TableLayoutState getState() {
    return (TableLayoutState)super.getState();
  }
}
