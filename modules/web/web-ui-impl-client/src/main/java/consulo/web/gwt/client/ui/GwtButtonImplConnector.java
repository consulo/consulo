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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.vaadin.client.StyleConstants;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.shared.ui.state.button.ButtonRpc;

/**
 * @author VISTALL
 * @since 13-Sep-17
 */
@Connect(canonicalName = "consulo.ui.web.internal.WebButtonImpl.Vaadin")
public class GwtButtonImplConnector extends AbstractComponentConnector implements ClickHandler {
  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  protected void updateComponentSize() {
    GwtComponentSizeUpdater.updateForComponent(this);
  }

  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);

    getWidget().setText(getState().caption);
  }

  @Override
  protected void init() {
    super.init();
    getWidget().addClickHandler(this);
  }

  @Override
  public GwtButtonImpl getWidget() {
    return (GwtButtonImpl)super.getWidget();
  }

  @Override
  public void onClick(ClickEvent event) {
    getRpcProxy(ButtonRpc.class).onClick();
  }
}
