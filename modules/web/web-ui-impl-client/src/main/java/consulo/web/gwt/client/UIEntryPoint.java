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
package consulo.web.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RootPanel;
import com.sksamuel.gwt.websockets.WebsocketListener;
import consulo.web.gwt.client.ui.GwtModalWindowImpl;
import consulo.web.gwt.client.ui.InternalGwtComponent;
import consulo.web.gwt.client.util.ExceptionUtil;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.client.util.Log;
import consulo.web.gwt.shared.UIClientEvent;
import consulo.web.gwt.shared.UIClientEventType;
import consulo.web.gwt.shared.UIComponent;
import consulo.web.gwt.shared.UIServerEvent;
import org.gwt.advanced.client.util.ThemeHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class UIEntryPoint implements EntryPoint {
  @Override
  public void onModuleLoad() {
    ThemeHelper.getInstance().setThemeName("classic");

    final RootPanel rootPanel = RootPanel.get();
    rootPanel.add(GwtUIUtil.loadingPanel());

    final String consuloSessionId = Cookies.getCookie("ConsuloSessionId");
    if (consuloSessionId == null) {
      return;
    }
    final WebSocketProxy proxy = new WebSocketProxy(consuloSessionId);
    proxy.addListener(new WebsocketListener() {
      @Override
      public void onClose() {
      }

      @Override
      public void onMessage(String json) {
        Log.log("receive: " + json);

        final UIServerEvent event;
        try {
          event = UIMappers.ourUIServerEventMapper.read(json);
        }
        catch (Exception e) {
          Window.alert("Failed to serialize " + ExceptionUtil.toString(e));
          e.printStackTrace();
          return;
        }

        final List<UIComponent> components = event.getComponents();

        switch (event.getType()) {
          case createRoot:
            IsWidget root = null;
            if (components != null) {
              for (UIComponent component : components) {
                root = UIConverter.create(proxy, component);
                break;
              }

              assert root != null;
              rootPanel.clear();
              rootPanel.add(root.asWidget());
            }
            break;
          case showModal:
            GwtModalWindowImpl modal = null;
            if (components != null) {
              for (UIComponent component : components) {
                modal = (GwtModalWindowImpl)UIConverter.create(proxy, component);
                break;
              }

              //FIXME [VISTALL] nothing?
            }
            break;
          case stateChanged:
            if (components != null) {
              for (UIComponent component : components) {
                final InternalGwtComponent temp = UIConverter.get(component.getId());

                final Map<String, Object> variables = component.getVariables();
                temp.updateState(variables == null ? Collections.<String, Object>emptyMap() : variables);
              }
            }
            break;
          default:
            Window.alert("Unknown event: " + event.getType());
            break;
        }
      }

      @Override
      public void onOpen() {
        proxy.send(UIClientEventType.sessionOpen, new WebSocketProxy.Consumer<UIClientEvent>() {
          @Override
          public void consume(UIClientEvent value) {
            // nothing
          }
        });
      }
    });

    proxy.open();
  }
}
