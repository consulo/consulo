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

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.shared.ui.state.tab.TabbedLayoutState;

import java.util.Map;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
@Connect(canonicalName = "consulo.ui.internal.WGwtTabbedLayoutImpl")
public class GwtTabbedLayoutImpl extends TabPanel {
  public GwtTabbedLayoutImpl() {
    setStyleName("ui-tabbed-layout");
    getDeckPanel().setStyleName("ui-tabbed-layout-bottom");
  }

  public void setTabs(int index, Map<TabbedLayoutState.TabState, Widget> map) {
    clear();

    for (Map.Entry<TabbedLayoutState.TabState, Widget> entry : map.entrySet()) {
      add(entry.getValue(), GwtComboBoxImplConnector.buildItem(entry.getKey()));
    }

    selectTab(index);
  }
}
