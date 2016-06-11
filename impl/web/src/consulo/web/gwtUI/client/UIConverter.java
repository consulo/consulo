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
package consulo.web.gwtUI.client;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import consulo.web.gwtUI.client.ui.GwtCheckBoxImpl;
import consulo.web.gwtUI.client.ui.GwtComponentImpl;
import consulo.web.gwtUI.client.ui.GwtDockPanelImpl;
import consulo.web.gwtUI.shared.UIComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class UIConverter {
  interface Factory {
    Widget create();
  }

  private static Map<String, Factory> ourMap = new HashMap<String, Factory>();

  static {
    ourMap.put("consulo.ui.internal.WGwtCheckBoxImpl", new Factory() {
      @Override
      public Widget create() {
        return new GwtCheckBoxImpl();
      }
    });
    ourMap.put("consulo.ui.internal.WGwtDockPanelImpl", new Factory() {
      @Override
      public Widget create() {
        return new GwtDockPanelImpl();
      }
    });
  }

  public static Widget create(WebSocketProxy proxy, UIComponent component) {
    final String type = component.getType();
    Factory factory = ourMap.get(type);
    if (factory == null) {
      return new Label("Type " + type + " is not resolved");
    }

    final Widget widget = factory.create();

    final Map<String, String> variables = component.getVariables();
    if(variables != null) {
      if(widget instanceof GwtComponentImpl) {
        ((GwtComponentImpl)widget).init(proxy, component.getId(), variables);
      }
    }

    final List<UIComponent.Child> children = component.getChildren();
    if(children != null) {
      for (UIComponent.Child child : children) {
        if(widget instanceof GwtComponentImpl) {
          ((GwtComponentImpl)widget).addChildren(proxy, child);
        }
      }
    }
    return widget;
  }
}
