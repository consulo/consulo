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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.ui.advancedGwt.ComboBoxDataModel2;
import consulo.web.gwt.client.ui.advancedGwt.WidgetComboBox;
import consulo.web.gwt.shared.ui.state.combobox.ComboBoxState;
import org.gwt.advanced.client.ui.widget.combo.ListItemFactory;
import org.gwt.advanced.client.util.ThemeHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class GwtComboBoxImpl extends WidgetComboBox {
  static {
    ThemeHelper.getInstance().setThemeName("classic");
  }

  /**
   * Item list with by index
   * <p>
   * Contains null item too, that why - get component by index, need +1
   */
  private List<ComboBoxState.Item> myItemsWithNullItem = new ArrayList<>();

  public GwtComboBoxImpl() {
    setLazyRenderingEnabled(false);
    setListItemFactory(new ListItemFactory() {
      @Override
      public Widget createWidget(Object value) {
        int index = value == null ? 0 : ((Integer)value + 1);
        final ComboBoxState.Item item = myItemsWithNullItem.isEmpty() ? null : myItemsWithNullItem.get(index);
        if (item == null) {
          return new Label(""); // empty item when no items
        }
        return GwtComboBoxImplConnector.buildItem(item);
      }

      @Override
      public String convert(Object value) {
        throw new UnsupportedOperationException("this should not never called");
      }
    });
  }

  public void setItems(int selectedIndex, List<ComboBoxState.Item> widgets) {
    myItemsWithNullItem.clear();
    myItemsWithNullItem.addAll(widgets);

    final ComboBoxDataModel2 model = (ComboBoxDataModel2)getModel();

    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < (widgets.size() - 1); i++) {
      map.put(String.valueOf(i), i);
    }
    model.set(map);
    model.setSelectedIndex(selectedIndex);
  }

  @Override
  public void clear() {
    super.clear();

    myItemsWithNullItem.clear();
  }
}
