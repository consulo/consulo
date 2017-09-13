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
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.util.GwtUIUtil;

import java.util.List;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class GwtVerticalLayoutImpl extends Grid {
  private SimplePanel myPanel = GwtUIUtil.fillAndReturn(new SimplePanel());

  public GwtVerticalLayoutImpl() {
    super(1, 1);

    updateLastRow(-1);

    GwtUIUtil.fill(this);
  }

  public void setChildren(List<Widget> widgets) {
    clear();

    resizeRows(1);

    for (Widget child : widgets) {
      final int rowCount = getRowCount();
      resizeRows(rowCount + 1);

      setWidget(rowCount - 1, 0, child);

      updateLastRow(rowCount - 1);

      //child.setWidth("100%");

      //Style style = getCellFormatter().getElement(rowCount - 1, 0).getStyle();
      //style.setWidth(100, Style.Unit.PCT);
    }
  }

  private void updateLastRow(int prevRow) {
    if (prevRow != -1) {
      final Style style = getCellFormatter().getElement(prevRow, 0).getStyle();

      style.clearHeight();
      style.clearWidth();
    }

    final int rowCount = getRowCount();
    final int lastRow = rowCount - 1;
    setWidget(lastRow, 0, myPanel);

    final Style style = getCellFormatter().getElement(lastRow, 0).getStyle();
    style.setHeight(100, Style.Unit.PCT);
    style.setWidth(100, Style.Unit.PCT);
  }
}
