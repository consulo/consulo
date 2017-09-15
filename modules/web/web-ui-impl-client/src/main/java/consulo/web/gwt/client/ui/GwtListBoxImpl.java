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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.ui.advancedGwt.ComboBoxSelectItem;
import org.gwt.advanced.client.datamodel.ListModelEvent;
import org.gwt.advanced.client.datamodel.ListModelListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class GwtListBoxImpl extends FlowPanel {
  private int mySelectedIndex = -1;

  private List<ComboBoxSelectItem> myList = new ArrayList<>();

  private List<ListModelListener> myModelListeners = new ArrayList<>();

  public GwtListBoxImpl() {
    setStyleName("ui-list-box");
  }

  public void addListModelListener(ListModelListener listModelListener) {
    myModelListeners.add(listModelListener);
  }

  public void setItems(int selectedIndex, List<Widget> list) {
    clear();

    int i = 0;
    for (Widget widget : list) {
      final int index = i++;

      final ComboBoxSelectItem comboBoxSelectItem = new ComboBoxSelectItem();
      myList.add(comboBoxSelectItem);

      comboBoxSelectItem.setWidth("100%");

      comboBoxSelectItem.setWidget(widget);
      comboBoxSelectItem.addClickHandler(event -> setSelectedIndex(index));


      add(comboBoxSelectItem);
    }

    setSelectedIndex(selectedIndex, false);
  }

  public void setSelectedIndex(int selectedIndex) {
    setSelectedIndex(selectedIndex, true);
  }

  public void setSelectedIndex(int selectedIndex, boolean fireEvents) {
    if (mySelectedIndex != -1) {
      for (ComboBoxSelectItem selectItem : myList) {
        selectItem.removeStyleName("selected");
      }
    }

    mySelectedIndex = selectedIndex;

    ComboBoxSelectItem target = selectedIndex == -1 ? null : myList.get(selectedIndex);

    if (target != null) {
      target.addStyleName("selected");
    }

    if (fireEvents) {
      ListModelEvent event = new ListModelEvent(null, String.valueOf(selectedIndex), selectedIndex, ListModelEvent.SELECT_ITEM);
      for (ListModelListener modelListener : myModelListeners) {
        modelListener.onModelEvent(event);
      }
    }
  }
}
