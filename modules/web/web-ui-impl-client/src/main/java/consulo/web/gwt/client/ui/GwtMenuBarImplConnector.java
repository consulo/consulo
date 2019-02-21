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

import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ConnectorHierarchyChangeEvent;
import com.vaadin.shared.ui.Connect;
import consulo.web.gwt.client.util.GwtUIUtil;

import java.util.List;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
@Connect(canonicalName = "consulo.ui.web.internal.WebMenuBarImpl.Vaadin")
public class GwtMenuBarImplConnector extends GwtLayoutConnector {
  @Override
  public void onConnectorHierarchyChange(ConnectorHierarchyChangeEvent connectorHierarchyChangeEvent) {
    addItems(GwtUIUtil.remapWidgets(this), getWidget());
  }

  @Override
  public GwtMenuBarImpl getWidget() {
    return (GwtMenuBarImpl)super.getWidget();
  }

  public static void addItems(List<Widget> child, MenuBar bar) {
    for (Widget widget : child) {
      if (widget instanceof GwtMenuItemImpl) {
        bar.addItem(((GwtMenuItemImpl)widget).getItem());
      }
      else if (widget instanceof GwtMenuSeparatorImpl) {
        bar.addSeparator();
      }
      else if(widget instanceof GwtMenuImpl) {
        bar.addItem(((GwtMenuImpl)widget).getMenu());
      }
    }
  }
}
