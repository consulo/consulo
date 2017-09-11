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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalSplitPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.client.StyleConstants;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractLayoutConnector;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.layout.SplitLayoutState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
@Connect(canonicalName = "consulo.ui.internal.WGwtHorizontalSplitLayoutImpl")
public class GwtHorizontalSplitLayoutImplConnector extends AbstractLayoutConnector {
  @Override
  protected void updateWidgetStyleNames() {
    super.updateWidgetStyleNames();

    setWidgetStyleName(StyleConstants.UI_WIDGET, false);
  }

  @Override
  public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent connectorHierarchyChangeEvent) {
    List<Widget> widgets = GwtUIUtil.remapWidgets(this);

    setWidget(getWidget(), safeGet(widgets, 0), safeGet(widgets, 1));
  }

  @Override
  public void onStateChanged(StateChangeEvent stateChangeEvent) {
    super.onStateChanged(stateChangeEvent);

    getWidget().setSplitPosition(getState().myProportion + "%");
  }

  @Override
  public void updateCaption(ComponentConnector componentConnector) {
  }

  @Override
  public SplitLayoutState getState() {
    return (SplitLayoutState)super.getState();
  }

  @Override
  public HorizontalSplitPanel getWidget() {
    return (HorizontalSplitPanel)super.getWidget();
  }

  @Override
  protected HorizontalSplitPanel createWidget() {
    HorizontalSplitPanel widget = new HorizontalSplitPanel();
    widget.setStyleName("ui-horizontal-split-panel");
    widget.setHeight(null);
    return widget;
  }

  @Nullable
  public static <T> T safeGet(@Nullable List<? extends T> array, int index) {
    if (array == null) return null;
    if (index < 0 || array.size() <= index) return null;
    return array.get(index);
  }

  public void setWidget(HorizontalSplitPanel panel, @Nullable Widget o1, @Nullable Widget o2) {
    if (o1 != null) {
      GwtUIUtil.fill(o1);

      o1.addAttachHandler(event -> {
        final int height = panel.getElement().getClientHeight();
        Window.alert("attach " + o1.getOffsetHeight() + o1.getElement().getClientHeight());

        if (height != 0) {
          Window.alert("set " + o1.getElement().getClientHeight());
          panel.setHeight(o1.getElement().getClientHeight() + "px");
        }
      });
      panel.setLeftWidget(o1);
    }
    else {
      panel.setLeftWidget(null);
    }

    if (o2 != null) {
      GwtUIUtil.fill(o2);
    }
    panel.setRightWidget(o2);
  }
}