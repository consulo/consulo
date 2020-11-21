/*
 * Copyright 2013-2018 consulo.io
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
import consulo.web.gwt.client.ui.image.ImageConverter;
import consulo.web.gwt.shared.ui.state.button.ButtonRpc;
import consulo.web.gwt.shared.ui.state.button.ButtonState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;

/**
 * @author VISTALL
 * @since 2018-05-11
 */
@Connect(canonicalName = "consulo.ui.web.internal.WebHyperlinkImpl.Vaadin")
public class GwtHyperlinkImplConnector extends AbstractComponentConnector implements ClickHandler {
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

    getWidget().getAnchor().setText(getState().caption);
    MultiImageState imageState = getState().myImageState;
    getWidget().setImage(ImageConverter.create(imageState), imageState.myWidth, imageState.myHeight);

    getWidget().rebuild();
  }

  @Override
  protected void init() {
    super.init();
    getWidget().getAnchor().addClickHandler(this);
  }

  @Override
  public ButtonState getState() {
    return (ButtonState)super.getState();
  }

  @Override
  public GwtHyperlinkImpl getWidget() {
    return (GwtHyperlinkImpl)super.getWidget();
  }

  @Override
  public void onClick(ClickEvent event) {
    getRpcProxy(ButtonRpc.class).onClick();
  }
}
