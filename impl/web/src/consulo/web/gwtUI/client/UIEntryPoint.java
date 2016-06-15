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
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamFactory;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RootPanel;
import com.sksamuel.gwt.websockets.Websocket;
import com.sksamuel.gwt.websockets.WebsocketListener;
import consulo.web.gwtUI.client.ui.InternalGwtComponent;
import consulo.web.gwtUI.client.util.GwtUIUtil2;
import consulo.web.gwtUI.client.util.Log;
import consulo.web.gwtUI.shared.UIClientEvent;
import consulo.web.gwtUI.shared.UIClientEventType;
import consulo.web.gwtUI.shared.UIComponent;
import consulo.web.gwtUI.shared.UIServerEvent;
import org.gwt.advanced.client.util.ThemeHelper;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class UIEntryPoint implements EntryPoint {
  public static final SerializationStreamFactory factory = (SerializationStreamFactory)GWT.create(HackService.class);

  @Override
  public void onModuleLoad() {
    ThemeHelper.getInstance().setThemeName("classic");

    final RootPanel rootPanel = RootPanel.get();
    rootPanel.add(GwtUIUtil2.loadingPanel());

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

        final UIServerEvent event;
        try {
          final SerializationStreamReader streamReader = factory.createStreamReader(msg);
          event = (UIServerEvent)streamReader.readObject();
        }
        catch (SerializationException e) {
          Window.alert(e.getMessage());
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
          case stateChanged:
            if (components != null) {
              for (UIComponent component : components) {
                final InternalGwtComponent temp = UIConverter.get(component.getId());

                final Map<String, Serializable> variables = component.getVariables();
                temp.updateState(variables == null ? Collections.<String, Serializable>emptyMap() : variables);
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
