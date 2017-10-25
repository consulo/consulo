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
package consulo.web.gwt.client.ui.tree;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import consulo.web.gwt.client.ui.GwtComboBoxImplConnector;
import consulo.web.gwt.client.ui.GwtHorizontalLayoutImpl;
import consulo.web.gwt.shared.ui.state.tree.TreeState;

/**
* @author VISTALL
* @since 16-Sep-17
*/
class GwtTreeCell extends AbstractCell<TreeState.TreeNodeState> {
  private GwtTreeImpl myCellTree;

  public GwtTreeCell() {
    super(BrowserEvents.DBLCLICK);
  }

  public void init(GwtTreeImpl cellTree) {
    myCellTree = cellTree;
  }

  @Override
  public void render(Context context, TreeState.TreeNodeState value, SafeHtmlBuilder sb) {
    GwtHorizontalLayoutImpl layout = GwtComboBoxImplConnector.buildItem(value);

    SafeHtml safeValue = SafeHtmlUtils.fromSafeConstant(layout.toString());

    sb.append(safeValue);
  }

  @Override
  public void onBrowserEvent(Context context, Element parent, TreeState.TreeNodeState value, NativeEvent event, ValueUpdater<TreeState.TreeNodeState> valueUpdater) {
    String type = event.getType();

    if (type.equals(BrowserEvents.DBLCLICK)) {

      myCellTree.getTreeViewModel().getDoubleClickHandler().accept(value);
    }
    else {
      super.onBrowserEvent(context, parent, value, event, valueUpdater);
    }
  }
}
