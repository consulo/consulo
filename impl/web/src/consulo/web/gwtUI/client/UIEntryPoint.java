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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.sksamuel.gwt.websockets.Websocket;
import com.sksamuel.gwt.websockets.WebsocketListener;
import consulo.web.gwtUI.client.ui.GwtComponentImpl;
import consulo.web.gwtUI.client.util.Log;
import consulo.web.gwtUI.client.util.UIUtil;
import consulo.web.gwtUI.shared.*;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class UIEntryPoint implements EntryPoint {
  public static final UIEventFactory ourEventFactory = GWT.create(UIEventFactory.class);

  @Override
  public void onModuleLoad() {
    Widget widget = UIUtil.loadingPanel();

    final RootPanel rootPanel = RootPanel.get();

    rootPanel.add(widget);

    final String consuloSessionId = Cookies.getCookie("ConsuloSessionId");
    if (consuloSessionId == null) {
      return;
    }

    String url = "ws://" + Window.Location.getHost() + "/ws";
    final Websocket websocket = new Websocket(url);

    final WebSocketProxy proxy = new WebSocketProxy(websocket);
    proxy.setSessionId(consuloSessionId);

    websocket.addListener(new WebsocketListener() {
      @Override
      public void onClose() {
      }

      @Override
      public void onMessage(String msg) {
        Log.log("receive: " + msg);

        AutoBean<UIServerEvent> bean = AutoBeanCodex.decode(ourEventFactory, UIServerEvent.class, msg);

        final UIServerEvent event = bean.as();
        final List<UIComponent> components = event.getComponents();

        switch (event.getType()) {
          case createRoot:
            rootPanel.clear();
            if (components != null) {
              for (UIComponent component : components) {
                rootPanel.add(UIConverter.create(proxy, component));
              }
            }
            break;
          case stateChanged:
            if (components != null) {
              for (UIComponent component : components) {
                final GwtComponentImpl temp = UIConverter.get(component.getId());
                final Map<String, String> variables = component.getVariables();
                if(variables != null) {
                  temp.updateState(variables);
                }
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

    websocket.open();
  }
}
