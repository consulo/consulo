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
import consulo.web.gwt.client.util.ExceptionUtil;
import consulo.web.gwt.client.util.Log;
import consulo.web.gwt.shared.UIClientEvent;
import consulo.web.gwt.shared.UIClientEventType;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WebSocketProxy {
  public interface Consumer<T> {
    void consume(T value);
  }

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

  public void send(UIClientEventType eventType, Consumer<UIClientEvent> consumer) {
    try {
      final UIClientEvent clientEvent = new UIClientEvent();

      clientEvent.setSessionId(mySessionId);
      clientEvent.setType(eventType);

      consumer.consume(clientEvent);

      final String data = UIMappers.ourUIClientEventMapper.write(clientEvent);

      Log.log("send: " + data);

      myWebsocket.send(data);
    }
    catch (Exception e) {
      Window.alert("Failed to serialize " + ExceptionUtil.toString(e));
    }
  }
}
