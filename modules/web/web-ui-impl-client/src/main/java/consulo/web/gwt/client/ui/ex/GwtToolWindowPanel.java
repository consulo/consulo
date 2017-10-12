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

import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class GwtToolWindowPanel extends DockPanel {
  public GwtToolWindowPanel() {
    setHorizontalAlignment(DockPanel.ALIGN_CENTER);
    for (DockLayoutState.Constraint constraint : DockLayoutState.Constraint.values()) {
      if (constraint == DockLayoutState.Constraint.CENTER) {
        continue;
      }

      GwtToolWindowStripePanel panel = new GwtToolWindowStripePanel(constraint);
      GwtUIUtil.fill(panel.asWidget());

      switch (constraint) {
        case TOP:
          add(panel, NORTH);

          setAnywhereSize(panel, "22px", "100%", "Bottom");
          break;
        case BOTTOM:
          add(panel, SOUTH);

          setAnywhereSize(panel, "22px", "100%", "Top");
          break;
        case LEFT:
          add(panel, WEST);

          setAnywhereSize(panel, "100%", "22px", "Right");
          break;
        case RIGHT:
          add(panel, EAST);

          setAnywhereSize(panel, "100%", "22px", "Left");
          break;
        case CENTER:
          break;
      }
    }

    HorizontalPanel centerPanel = new HorizontalPanel();

    add(centerPanel, CENTER);

    setAnywhereSize(centerPanel, "100%", "100%", null);
  }

  private void setAnywhereSize(IsWidget widget, String height, String width, String borderPosition) {
    Widget w = widget.asWidget();

    w.setHeight(height);
    w.setWidth(width);

    setCellHeight(w, height);
    setCellWidth(w, width);

    if(borderPosition != null) {
      setCellStyleProperty(w, "border" + borderPosition + "Color", "gray");
      setCellStyleProperty(w, "border" + borderPosition + "Style", "solid");
      setCellStyleProperty(w, "border" + borderPosition + "Width", "1px");
    }
  }
}
