/*
 * Copyright 2013-2020 consulo.io
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
import consulo.web.gwt.shared.ui.state.layout.TableLayoutState;

import java.util.List;

/**
 * @author VISTALL
 * @since 2020-08-25
 */
public class GwtTableLayoutImpl extends Grid {
  public void setComponents(List<Widget> widgets, List<TableLayoutState.TableCell> constraints) {
    clear();

    int rows = 0;
    int columns = 0;
    for (TableLayoutState.TableCell cell : constraints) {
      if (cell.row > rows) {
        rows = cell.row;
      }

      if (cell.column > columns) {
        columns = cell.column;
      }
    }

    resize(rows + 1, columns + 1);

    CellFormatter cellFormatter = getCellFormatter();
    for (int i = 0; i < widgets.size(); i++) {
      Widget widget = widgets.get(i);
      TableLayoutState.TableCell cell = constraints.get(i);

      setWidget(cell.row, cell.column, widget);

      if(cell.fill) {
        cellFormatter.getElement(cell.row, cell.column).getStyle().setHeight(100, Style.Unit.PCT);
      }
    }
  }
}
