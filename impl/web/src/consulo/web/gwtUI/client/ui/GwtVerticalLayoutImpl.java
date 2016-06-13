/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwtUI.client.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.SimplePanel;
import consulo.web.gwtUI.client.UIConverter;
import consulo.web.gwtUI.client.WebSocketProxy;
import consulo.web.gwtUI.client.util.GwtUIUtil2;
import consulo.web.gwtUI.shared.UIComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class GwtVerticalLayoutImpl extends Grid implements InternalGwtComponent {
  private SimplePanel myPanel = GwtUIUtil2.fillAndReturn(new SimplePanel());

  public GwtVerticalLayoutImpl() {
    super(1, 1);

    updateLastRow(-1);

    GwtUIUtil2.fill(this);
  }

  @Override
  public void updateState(@NotNull Map<String, String> map) {
    DefaultVariables.updateState(map, this);
  }

  @Override
  public void addChildren(WebSocketProxy proxy, UIComponent.Child child) {
    final int rowCount = getRowCount();
    resizeRows(rowCount + 1);

    setWidget(rowCount - 1, 0, UIConverter.create(proxy, child.getComponent()));

    updateLastRow(rowCount - 1);
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
