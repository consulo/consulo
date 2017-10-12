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

import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.layout.DockLayoutState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class GwtToolWindowPanel extends DockPanel {
  private final Map<DockLayoutState.Constraint, GwtToolWindowStripePanel> myPanels = new HashMap<>();

  private final Map<String, GwtToolWindowStripeButton> myButtons = new HashMap<>();
  private final Map<String, GwtInternalDecorator> myInternalDecorators = new HashMap<>();

  private SplitLayoutPanel myCenterSplitLayout = new SplitLayoutPanel(2);

  private SimplePanel myLeftPanel = new SimplePanel();
  private SimplePanel myRightPanel = new SimplePanel();

  public GwtToolWindowPanel() {
    setHorizontalAlignment(DockPanel.ALIGN_CENTER);
    for (DockLayoutState.Constraint constraint : DockLayoutState.Constraint.values()) {
      if (constraint == DockLayoutState.Constraint.CENTER) {
        continue;
      }

      GwtToolWindowStripePanel panel = new GwtToolWindowStripePanel(constraint);
      GwtUIUtil.fill(panel.asWidget());

      myPanels.put(constraint, panel);

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

    add(myCenterSplitLayout, CENTER);

    setAnywhereSize(myCenterSplitLayout, "100%", "100%", null);

    GwtEditorImpl widget = new GwtEditorImpl();
    widget.setSize("100%", "100%");

    myCenterSplitLayout.addWest(myLeftPanel, 250);
    myCenterSplitLayout.addEast(myRightPanel, 250);

    myCenterSplitLayout.setWidgetHidden(myLeftPanel, true);
    myCenterSplitLayout.setWidgetHidden(myRightPanel, true);

    myCenterSplitLayout.add(widget);

    addButton(DockLayoutState.Constraint.LEFT, "Project", new Label("Some Data"));

    showOrHide("Project");
  }

  @NotNull
  private GwtToolWindowStripeButton addButton(DockLayoutState.Constraint constraint, String text, Widget widget) {
    GwtToolWindowStripePanel panel = myPanels.get(constraint);

    GwtToolWindowStripeButton button = new GwtToolWindowStripeButton(text, constraint, this);

    myButtons.put(text, button);

    panel.addButton(button);

    myInternalDecorators.put(text, new GwtInternalDecorator(widget));

    if (constraint == DockLayoutState.Constraint.LEFT || constraint == DockLayoutState.Constraint.RIGHT) {
      button.setWidth("22px");
    }
    else if (constraint == DockLayoutState.Constraint.TOP || constraint == DockLayoutState.Constraint.BOTTOM) {
      button.setHeight("22px");
    }
    return button;
  }

  public void showOrHide(String id) {
    GwtToolWindowStripeButton button = myButtons.get(id);
    if (button == null) {
      return;
    }

    boolean isActive = !button.isActive();

    button.setActive(isActive);

    GwtInternalDecorator decorator = myInternalDecorators.get(id);
    assert decorator != null;

    DockLayoutState.Constraint position = button.getPosition();
    if (position == DockLayoutState.Constraint.LEFT || position == DockLayoutState.Constraint.RIGHT) {
      SimplePanel simplePanel;
      if (position == DockLayoutState.Constraint.LEFT) {
        simplePanel = myLeftPanel;
      }
      else {
        simplePanel = myRightPanel;
      }

      myCenterSplitLayout.setWidgetHidden(simplePanel, !isActive);

      if(isActive) {
        simplePanel.setWidget(decorator);
        myCenterSplitLayout.setWidgetSize(simplePanel, 250);
      }
      else {
        simplePanel.setWidget(null);
      }
    }
  }

  private void setAnywhereSize(IsWidget widget, String height, String width, String borderPosition) {
    Widget w = widget.asWidget();

    w.setHeight(height);
    w.setWidth(width);

    setCellHeight(w, height);
    setCellWidth(w, width);

    if (borderPosition != null) {
      setCellStyleProperty(w, "border" + borderPosition + "Color", "gray");
      setCellStyleProperty(w, "border" + borderPosition + "Style", "solid");
      setCellStyleProperty(w, "border" + borderPosition + "Width", "1px");
    }
  }
}
