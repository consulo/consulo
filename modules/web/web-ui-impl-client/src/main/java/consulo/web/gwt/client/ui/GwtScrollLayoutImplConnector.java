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

import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.StyleConstants;
import com.vaadin.client.ui.AbstractSingleComponentContainerConnector;
import com.vaadin.shared.ui.Connect;

/**
 * @author VISTALL
 * @since 2020-05-10
 */
@Connect(canonicalName = "consulo.ui.web.internal.layout.WebScrollLayoutImpl.Vaadin")
public class GwtScrollLayoutImplConnector extends AbstractSingleComponentContainerConnector {
  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  protected void updateComponentSize() {
    GwtComponentSizeUpdater.updateForLayout(this);
  }

  @Override
  public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent connectorHierarchyChangeEvent) {
    getWidget().clear();
    getWidget().add(getContentWidget());
  }

  @Override
  public void updateCaption(ComponentConnector componentConnector) {
  }

  @Override
  public GwtScrollLayoutImpl getWidget() {
    return (GwtScrollLayoutImpl)super.getWidget();
  }
}
