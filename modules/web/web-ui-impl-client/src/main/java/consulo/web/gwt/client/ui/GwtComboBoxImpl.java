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

import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwt.client.UIConverter;
import consulo.web.gwt.client.WebSocketProxy;
import consulo.web.gwt.client.ui.advancedGwt.WidgetComboBox;
import consulo.web.gwt.shared.UIClientEvent;
import consulo.web.gwt.shared.UIClientEventType;
import consulo.web.gwt.shared.UIComponent;
import org.gwt.advanced.client.datamodel.ListDataModel;
import org.gwt.advanced.client.datamodel.ListModelEvent;
import org.gwt.advanced.client.datamodel.ListModelListener;
import org.gwt.advanced.client.ui.widget.combo.ListItemFactory;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class GwtComboBoxImpl extends WidgetComboBox implements InternalGwtComponentWithListeners, InternalGwtComponentWithChildren {
  /**
   * Item list with by index
   * <p/>
   * Contains null item too, that why - get component by index, need +1
   */
  private List<UIComponent.Child> myItemsWithNullItem = new ArrayList<UIComponent.Child>();
  private WebSocketProxy myProxy;

  public GwtComboBoxImpl() {
    setLazyRenderingEnabled(false);

    setListItemFactory(new ListItemFactory() {
      @Override
      public Widget createWidget(Object value) {
        int index = value == null ? 0 : ((Integer)value + 1);
        final UIComponent.Child child = myItemsWithNullItem.get(index);
        assert child != null;
        return UIConverter.create(myProxy, child.getComponent()).asWidget();
      }

      @Override
      public String convert(Object value) {
        throw new UnsupportedOperationException("this should not never called");
      }
    });
  }

  @Override
  public void setupListeners(final WebSocketProxy proxy, final long componentId) {
    getModel().addListModelListener(new ListModelListener() {
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

  @Override
  public void updateState(@NotNull Map<String, Serializable> map) {
    final int size = (Integer)map.get("size");

    final ListDataModel model = getModel();
    model.clear();
    for (int i = 0; i < size; i++) {
      model.add(String.valueOf(i), i);
    }

    setSelectedIndex((Integer)map.get("index"));
  }

  @Override
  public void addChildren(WebSocketProxy proxy, List<UIComponent.Child> children) {
    myProxy = proxy;
    for (UIComponent.Child child : children) {
      myItemsWithNullItem.add(child);
    }
  }
}
