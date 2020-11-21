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

import com.vaadin.client.StyleConstants;
import com.vaadin.client.annotations.OnStateChange;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.ex.state.toolWindow.ToolWindowStripeButtonRpc;
import consulo.web.gwt.shared.ui.ex.state.toolWindow.ToolWindowStripeButtonState;

/**
 * @author VISTALL
 * @since 12-Oct-17
 */
@Connect(canonicalName = "consulo.web.ui.ex.WebToolWindowStripeButtonImpl.Vaadin")
public class GwtToolWindowStripeButtonConnector extends AbstractComponentConnector {
  @Override
  protected void init() {
    super.init();

    getWidget().setClickListener(() -> {
      getRpcProxy(ToolWindowStripeButtonRpc.class).onClick();
      getConnection().getServerRpcQueue().flush();
    });
  }

  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);

    GwtToolWindowStripeButton widget = getWidget();
    widget.doLayout();

    widget.build(getState().caption, getState().myImageState);
  }

  @OnStateChange("mySelected")
  private void onSelected() {
    getWidget().setSelected(getState().mySelected);
  }

  @OnStateChange("mySecondary")
  private void changeSideTool() {
    GwtToolWindowStripeButton widget = getWidget();

    GwtToolWindowStripeInner stripeInner = GwtUIUtil.getParentOf(widget, GwtToolWindowStripeInner.class);

    if (stripeInner == null) {
      return;
    }

    stripeInner.add(widget, getState().mySecondary);
  }

  @Override
  protected void updateComponentSize() {
    //GwtComponentSizeUpdater.update(this);
  }

  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  public GwtToolWindowStripeButton getWidget() {
    return (GwtToolWindowStripeButton)super.getWidget();
  }

  @Override
  public ToolWindowStripeButtonState getState() {
    return (ToolWindowStripeButtonState)super.getState();
  }
}
