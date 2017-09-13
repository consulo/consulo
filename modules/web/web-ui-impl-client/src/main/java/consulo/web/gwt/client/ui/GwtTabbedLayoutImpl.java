/*
 * Copyright 2013-2016 consulo.io
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
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.ui.image.ImageConverter;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutState;

import java.util.Map;
import java.util.function.IntConsumer;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
@Connect(canonicalName = "consulo.ui.internal.WGwtTabbedLayoutImpl")
public class GwtTabbedLayoutImpl extends TabPanel {
  private IntConsumer myCloseHandler;

  public GwtTabbedLayoutImpl() {
    setStyleName("ui-tabbed-layout");
    getDeckPanel().setStyleName("ui-tabbed-layout-bottom");
  }

  public void setTabs(int index, Map<TabbedLayoutState.TabState, Widget> map) {
    clear();

    for (Map.Entry<TabbedLayoutState.TabState, Widget> entry : map.entrySet()) {
      TabbedLayoutState.TabState state = entry.getKey();
      GwtHorizontalLayoutImpl tabWidget = GwtComboBoxImplConnector.buildItem(state);
      if (state.myCloseButton != null) {
        SimplePanel closeButton = new SimplePanel();

        tabWidget.add(closeButton);
        tabWidget.setCellHorizontalAlignment(closeButton, HasHorizontalAlignment.ALIGN_RIGHT);

        Widget closeIcon = ImageConverter.create(state.myCloseButton);
        closeButton.setWidget(closeIcon);
        Widget closeHoveredIcon = ImageConverter.create(state.myCloseHoverButton);

        closeButton.addDomHandler(event -> {
          closeButton.setWidget(closeHoveredIcon);
        }, MouseOverEvent.getType());

        closeButton.addDomHandler(event -> {
          closeButton.setWidget(closeIcon);
        }, MouseOutEvent.getType());

        closeButton.addDomHandler(event -> {
          if (myCloseHandler != null) {
            int idx = getWidgetIndex(entry.getValue());
            if (idx != -1) {
              myCloseHandler.accept(idx);
            }
          }
        }, ClickEvent.getType());
      }
      else {
        tabWidget.addStyleName("gwt-TabBarItem-no-close-button");
      }
      add(entry.getValue(), tabWidget);
    }

    selectTab(index);
  }

  public void setCloseHandler(IntConsumer closeHandler) {
    myCloseHandler = closeHandler;
  }
}
