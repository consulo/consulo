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

import com.google.gwt.user.client.Window;
import com.sksamuel.gwt.websockets.Websocket;
import com.sksamuel.gwt.websockets.WebsocketListener;
import consulo.web.gwt.shared.UIClientEvent;
import consulo.web.gwt.shared.UIClientEventType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WebSocketProxy {
  private final Websocket myWebsocket;
  private final String mySessionId;

  public WebSocketProxy(String consuloSessionId) {
    mySessionId = consuloSessionId;
    String url = "ws://" + Window.Location.getHost() + "/ws";
    myWebsocket = new Websocket(url);

    Window.addCloseHandler(event -> myWebsocket.close());
  }

  public void open() {
    myWebsocket.open();
  }

  public void addListener(WebsocketListener websocketListener) {
    myWebsocket.addListener(websocketListener);
  }

  public void sendFireListener(long componentId, long mask, Consumer<Map<String, Object>> varSet) {
    send(UIClientEventType.invokeEvent, clientEvent -> {
      Map<String, Object> vars = new HashMap<>();
      vars.put("type", mask);
      vars.put("componentId", componentId);

      varSet.accept(vars);

      clientEvent.setVariables(vars);
    });
  }

  public void send(UIClientEventType eventType, Consumer<UIClientEvent> consumer) {
  }
}
