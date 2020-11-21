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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.ApplicationHolder;
import consulo.web.gwt.client.ComponentColors;
import consulo.web.gwt.client.util.GwtStyleUtil;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.RGBColorShared;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class GwtToolWindowPanel extends VerticalPanel {
  private final Map<DockLayoutState.Constraint, GwtToolWindowStripe> myStripes = new HashMap<>();

  private final SimplePanel myCenterPanel;

  public GwtToolWindowPanel() {
    setHorizontalAlignment(DockPanel.ALIGN_CENTER);

    myCenterPanel = new SimplePanel();
  }

  public void build(List<Widget> widgetList) {
    clear();
    myStripes.clear();
    myCenterPanel.setWidget(null);

    Widget centerWidget = null;
    int i = 0;
    for (Widget panel : widgetList) {
      if (panel instanceof GwtToolWindowStripe) {
        DockLayoutState.Constraint constraint = DockLayoutState.Constraint.values()[i++];

        ((GwtToolWindowStripe)panel).assign(constraint, this);

        GwtUIUtil.fill(panel);

        myStripes.put(constraint, (GwtToolWindowStripe)panel);
      }
      else {
        centerWidget = panel;
      }
    }

    if (centerWidget != null) {
      GwtUIUtil.fill(centerWidget);
    }

    myCenterPanel.setWidget(centerWidget);

    doLayout();
  }

  public void doLayout() {
    clear();

    GwtToolWindowStripe topLayout = myStripes.get(DockLayoutState.Constraint.TOP);
    if (topLayout != null && topLayout.canShow()) {
      add(topLayout);
      setAnywhereSize(this, topLayout, "22px", "100%", "Bottom");
    }

    HorizontalPanel centerBlock = new HorizontalPanel();
    GwtUIUtil.fill(centerBlock);

    GwtToolWindowStripe leftLayout = myStripes.get(DockLayoutState.Constraint.LEFT);
    if (leftLayout != null && leftLayout.canShow()) {
      centerBlock.add(leftLayout);
      setAnywhereSize(centerBlock, leftLayout, "100%", "22px", "Right");
    }

    add(centerBlock);

    centerBlock.add(myCenterPanel);

    setAnywhereSize(centerBlock, myCenterPanel, "100%", "100%", null);

    GwtToolWindowStripe rightLayout = myStripes.get(DockLayoutState.Constraint.RIGHT);
    if (rightLayout != null && rightLayout.canShow()) {
      centerBlock.add(rightLayout);
      setAnywhereSize(centerBlock, rightLayout, "100%", "22px", "Left");
    }

    GwtToolWindowStripe bottomLayout = myStripes.get(DockLayoutState.Constraint.BOTTOM);
    if (bottomLayout != null && bottomLayout.canShow()) {
      add(bottomLayout);
      setAnywhereSize(this, bottomLayout, "22px", "100%", "Top");
    }
  }

  private static void setAnywhereSize(CellPanel panel, Widget widget, String height, String width, String borderPosition) {
    widget.setHeight(height);
    widget.setWidth(width);

    panel.setCellHeight(widget, height);
    panel.setCellWidth(widget, width);


    if (borderPosition != null) {
      Element widgetTd = panel.getWidgetTd(widget);

      RGBColorShared borderColor = ApplicationHolder.INSTANCE.getComponentColor(ComponentColors.BORDER);

      widgetTd.getStyle().setProperty("border" + borderPosition + "Color", GwtStyleUtil.toString(borderColor));
      widgetTd.getStyle().setProperty("border" + borderPosition + "Style", "solid");
      widgetTd.getStyle().setProperty("border" + borderPosition + "Width", "1px");
    }
  }
}
