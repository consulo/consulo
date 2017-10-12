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

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.util.ArrayUtil2;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.shared.ui.state.RootPanelState;

import java.util.List;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class GwtRootPanelImpl extends Grid {
  public GwtRootPanelImpl() {
    super(2, 1);
    setStyleName("ui-root-panel");
    GwtUIUtil.fill(this);
  }

  public void setChildren(List<Widget> children, RootPanelState state) {
    clear();
    resize(2, 1);

    int rows = 0;
    final Widget menuComponent = state.menuBarExists ? ArrayUtil2.safeGet(children, 0) : null;
    if (state.menuBarExists) {
      rows++;
    }

    final Widget contentComponent = state.contentExists ? ArrayUtil2.safeGet(children, state.menuBarExists ? 1 : 0) : null;
    if (state.contentExists) {
      rows++;
    }

    resizeRows(rows);

    if (menuComponent != null) {
      getRowFormatter().getElement(0).getStyle().setHeight(22, Style.Unit.PX);

      setWidget(0, 0, menuComponent);
    }

    if (contentComponent != null) {
      GwtUIUtil.fill(contentComponent);

      setWidget(rows - 1, 0, contentComponent);
    }
  }
}
