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

import com.vaadin.client.StyleConstants;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.ui.image.ImageConverter;
import consulo.web.gwt.shared.ui.state.combobox.ComboBoxState;
import consulo.web.gwt.shared.ui.state.image.MultiImageState;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
@Connect(canonicalName = "consulo.ui.web.internal.WebComboBoxImpl.Vaadin")
public class GwtComboBoxImplConnector extends AbstractComponentConnector {
  @Override
  protected void updateComponentSize() {
    GwtComponentSizeUpdater.updateForComponent(this);
  }

  @Override
  protected void init() {
    super.init();

    getWidget().setConnection(getConnection());
  }

  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);

    getWidget().setItems(getState().mySelectedIndex, getState().myItems);
  }

  public static GwtHorizontalLayoutImpl buildItem(ComboBoxState.Item item) {
    GwtHorizontalLayoutImpl layout = new GwtHorizontalLayoutImpl();
    layout.setHorizontalAlignment(GwtHorizontalLayoutImpl.ALIGN_LEFT);

    MultiImageState imageState = item.myImageState;
    if (imageState != null) {
      layout.add(ImageConverter.create(imageState));
    }

    for (ComboBoxState.ItemSegment segment : item.myItemSegments) {
      GwtLabelImpl label = new GwtLabelImpl();
      label.setText(segment.myText);
      layout.add(label);
    }

    return layout;
  }

  @Override
  public GwtComboBoxImpl getWidget() {
    return (GwtComboBoxImpl)super.getWidget();
  }

  @Override
  public ComboBoxState getState() {
    return (ComboBoxState)super.getState();
  }
}
