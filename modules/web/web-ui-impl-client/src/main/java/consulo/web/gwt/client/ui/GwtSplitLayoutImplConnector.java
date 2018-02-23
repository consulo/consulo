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

import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.communication.StateChangeEvent;
import consulo.web.gwt.client.util.ArrayUtil2;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.layout.SplitLayoutState;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
public abstract class GwtSplitLayoutImplConnector extends GwtLayoutConnector implements ResizeHandler {
  private HandlerRegistration myCloseRegister;

  @Override
  public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent connectorHierarchyChangeEvent) {
    List<Widget> widgets = GwtUIUtil.remapWidgets(this);

    setWidget(getWidget(), ArrayUtil2.safeGet(widgets, 0), ArrayUtil2.safeGet(widgets, 1));
  }

  private void setWidget(GwtSplitLayoutImpl panel, @Nullable Widget o1, @Nullable Widget o2) {
    if (o1 != null) {
      GwtUIUtil.fill(o1);
    }

    panel.setFirstWidget(o1);

    if (o2 != null) {
      GwtUIUtil.fill(o2);
    }

    panel.setSecondWidget(o2);
  }

  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);
    getWidget().setSplitPosition(getState().myProportion + "%");
  }

  @Override
  protected void init() {
    super.init();
    myCloseRegister = Window.addResizeHandler(this);
  }

  @Override
  public void onUnregister() {
    super.onUnregister();
    myCloseRegister.removeHandler();
  }

  @Override
  public SplitLayoutState getState() {
    return (SplitLayoutState)super.getState();
  }

  @Override
  public GwtSplitLayoutImpl getWidget() {
    return (GwtSplitLayoutImpl)super.getWidget();
  }

  @Override
  public void onResize(ResizeEvent event) {
    getWidget().updateOnResize();
  }
}