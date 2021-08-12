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

import com.google.gwt.core.client.Scheduler;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.ui.image.ImageConverter;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;
import consulo.web.gwt.shared.ui.state.menu.MenuItemRpc;
import consulo.web.gwt.shared.ui.state.menu.MenuItemState;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
@Connect(canonicalName = "consulo.ui.web.internal.WebMenuItemImpl.Vaadin")
public class GwtMenuItemImplConnector extends AbstractComponentConnector implements Scheduler.ScheduledCommand {
  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);

    getWidget().getItem().setHTML(createTextLayout(getState().caption, getState().myImageState).getElement().getString());
    getWidget().getItem().setScheduledCommand(this);
  }

  @Override
  public MenuItemState getState() {
    return (MenuItemState)super.getState();
  }

  @Override
  public GwtMenuItemImpl getWidget() {
    return (GwtMenuItemImpl)super.getWidget();
  }

  @Nonnull
  public static GwtHorizontalLayoutImpl createTextLayout(String text, MultiImageState state) {
    GwtHorizontalLayoutImpl layout = new GwtHorizontalLayoutImpl();
    layout.setHorizontalAlignment(GwtHorizontalLayoutImpl.ALIGN_LEFT);

    if (state != null) {
      layout.add(ImageConverter.create(state));
    }

    GwtLabelImpl label = new GwtLabelImpl();
    label.setText(text);
    layout.add(label);
    return layout;
  }

  @Override
  public void execute() {
    getRpcProxy(MenuItemRpc.class).onClick();
  }
}
