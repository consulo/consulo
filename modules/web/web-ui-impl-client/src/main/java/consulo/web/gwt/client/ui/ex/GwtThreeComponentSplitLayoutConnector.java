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

import com.google.gwt.event.shared.HandlerRegistration;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.HasComponentsConnector;
import com.vaadin.client.StyleConstants;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.ex.state.ThreeComponentSplitLayoutState;

import java.util.List;

/**
 * @author VISTALL
 * @since 23-Oct-17
 */
@Connect(canonicalName = "consulo.ui.web.internal.WebThreeComponentSplitLayoutImpl.Vaadin")
public class GwtThreeComponentSplitLayoutConnector extends AbstractComponentConnector implements HasComponentsConnector {
  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);

    ThreeComponentSplitLayoutState state = getState();
    getWidget().rebuild(GwtUIUtil.connector2Widget(state.myLeftComponent), GwtUIUtil.connector2Widget(state.myRightComponent), GwtUIUtil.connector2Widget(state.myCenterComponent));
  }

  @Override
  public GwtThreeComponentSplitLayout getWidget() {
    return (GwtThreeComponentSplitLayout)super.getWidget();
  }

  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  protected void updateComponentSize() {
    //GwtComponentSizeUpdater.update(this);
  }

  @Override
  public ThreeComponentSplitLayoutState getState() {
    return (ThreeComponentSplitLayoutState)super.getState();
  }

  @Override
  public void updateCaption(ComponentConnector connector) {
    
  }

  @Override
  public List<ComponentConnector> getChildComponents() {
    return null;
  }

  @Override
  public void setChildComponents(List<ComponentConnector> children) {

  }

  @Override
  public HandlerRegistration addConnectorHierarchyChangeHandler(ConnectorHierarchyChangeEvent.ConnectorHierarchyChangeHandler handler) {
    return null;
  }
}
