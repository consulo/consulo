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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.CheckBox;
import consulo.web.gwtUI.client.WebSocketProxy;
import consulo.web.gwtUI.shared.UIClientEvent;
import consulo.web.gwtUI.shared.UIClientEventType;
import consulo.web.gwtUI.shared.UIComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class GwtCheckBoxImpl extends CheckBox implements GwtComponentImpl {
  @Override
  public void init(final WebSocketProxy proxy, final String componentId) {
    addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(final ValueChangeEvent<Boolean> event) {
        proxy.send(UIClientEventType.invokeEvent, new WebSocketProxy.Consumer<UIClientEvent>() {
          @Override
          public void consume(UIClientEvent clientEvent) {
            Map<String, String> vars = new HashMap<String, String>();
            vars.put("componentId", componentId);
            vars.put("selected", String.valueOf(event.getValue()));

            clientEvent.setVariables(vars);
          }
        });
      }
    });
  }

  @Override
  public void updateState(Map<String, String> map) {
    final String text = map.get("text");
    if(text != null) {
      setText(text);
    }
    final String selected = map.get("selected");
    if(selected != null) {
      setValue(Boolean.valueOf(selected));
    }
  }

  @Override
  public void addChildren(WebSocketProxy proxy, UIComponent.Child child) {
  }
}
