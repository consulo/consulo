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

import com.google.web.bindery.autobean.shared.AutoBean;
import com.sksamuel.gwt.websockets.Websocket;
import consulo.web.gwtUI.shared.AutoBeanJsonUtil;
import consulo.web.gwtUI.shared.UIClientEvent;
import consulo.web.gwtUI.shared.UIClientEventType;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class WebSocketProxy {
  public interface Consumer<T> {
    void consume(T value);
  }

  private Websocket myWebsocket;
  private String mySessionId;

  public WebSocketProxy(Websocket websocket) {
    myWebsocket = websocket;
  }

  public void setSessionId(String sessionId) {
    mySessionId = sessionId;
  }

  public void send(UIClientEventType eventType, Consumer<UIClientEvent> consumer) {
    final AutoBean<UIClientEvent> bean = UIEntryPoint.ourEventFactory.clientEvent();
    final UIClientEvent clientEvent = bean.as();

    clientEvent.setSessionId(mySessionId);
    clientEvent.setType(eventType);

    consumer.consume(clientEvent);

    myWebsocket.send(AutoBeanJsonUtil.toJson(bean));
  }
}
