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
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import consulo.web.gwt.client.UIConverter;
import consulo.web.gwt.client.WebSocketProxy;
import consulo.web.gwtUI.client.ui.advancedGwt.ComboBoxSelectItem;
import consulo.web.gwt.shared.UIClientEvent;
import consulo.web.gwt.shared.UIClientEventType;
import consulo.web.gwt.shared.UIComponent;
import org.gwt.advanced.client.datamodel.ListModelEvent;
import org.gwt.advanced.client.datamodel.ListModelListener;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16-Jun-16
 */
public class GwtListBoxImpl extends FlowPanel implements InternalGwtComponentWithChildren, InternalGwtComponentWithListeners {
  /**
   * Item list with by index
   */
  private WebSocketProxy myProxy;

  private int mySelectedIndex = -1;

  private List<ComboBoxSelectItem> myList = new ArrayList<ComboBoxSelectItem>();

  private List<ListModelListener> myModelListeners = new ArrayList<ListModelListener>();

  public GwtListBoxImpl() {
  }

  @Override
  public void addChildren(WebSocketProxy proxy, List<UIComponent.Child> children) {
    myProxy = proxy;
    int i = 0;
    for (UIComponent.Child child : children) {
      final int index = i++;
      final IsWidget widget = UIConverter.create(myProxy, child.getComponent());

      final ComboBoxSelectItem comboBoxSelectItem = new ComboBoxSelectItem();
      myList.add(comboBoxSelectItem);

      comboBoxSelectItem.setWidth("100%");

      comboBoxSelectItem.setWidget(widget);
      comboBoxSelectItem.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          setSelectedIndex(index);
        }
      });


      add(comboBoxSelectItem);
    }
  }

  @Override
  public void updateState(@NotNull Map<String, Serializable> map) {
    final int size = (Integer)map.get("size");

    setSelectedIndex((Integer)map.get("index"));
  }

  public void setSelectedIndex(int selectedIndex) {
    mySelectedIndex = selectedIndex;

    ComboBoxSelectItem target = selectedIndex == -1 ? null : myList.get(selectedIndex);

    for (ComboBoxSelectItem selectItem : myList) {
      if (selectItem == target) {
        continue;
      }
      selectItem.getElement().getStyle().setBackgroundColor(null);
    }

    if (target != null) {
      target.getElement().getStyle().setBackgroundColor("gray");
    }

    ListModelEvent event = new ListModelEvent(null, String.valueOf(selectedIndex), selectedIndex, ListModelEvent.SELECT_ITEM);
    for (ListModelListener modelListener : myModelListeners) {
      modelListener.onModelEvent(event);
    }
  }

  @Override
  public void setupListeners(final WebSocketProxy proxy, final long componentId) {
    myModelListeners.add(new ListModelListener() {
      @Override
      public void onModelEvent(final ListModelEvent event) {
        if (event.getType() == ListModelEvent.SELECT_ITEM) {
          proxy.send(UIClientEventType.invokeEvent, new WebSocketProxy.Consumer<UIClientEvent>() {
            @Override
            public void consume(UIClientEvent clientEvent) {
              Map<String, Serializable> vars = new HashMap<String, Serializable>();
              vars.put("type", "select");
              vars.put("componentId", componentId);
              vars.put("index", event.getItemIndex());

              clientEvent.setVariables(vars);
            }
          });
        }
      }
    });
  }
}
