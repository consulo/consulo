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
package consulo.web.servlet.ui;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.vm.AutoBeanFactorySource;
import consulo.web.gwtUI.shared.UIClientEvent;
import consulo.web.gwtUI.shared.UIEventFactory;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
@ServerEndpoint(value = "/ws")
public class UIWebSocketHandler {
  private static UIEventFactory ourEventFactory = AutoBeanFactorySource.create(UIEventFactory.class);

  @OnOpen
  public void onOpen(Session session) {
    System.out.println("Connected ... " + session.getId());
  }

  @OnMessage
  public void onMessage(String message, Session session) {
    AutoBean<UIClientEvent> bean = AutoBeanCodex.decode(ourEventFactory, UIClientEvent.class, message);

    UIClientEvent clientEvent = bean.as();
    switch (clientEvent.getType()) {
      case sessionOpen:
        UISessionManager.ourInstance.onSessionOpen(session, clientEvent, ourEventFactory);
        break;
      case invokeEvent:
        UISessionManager.ourInstance.onInvokeEvent(session, clientEvent, ourEventFactory);
        break;
    }
  }

  @OnClose
  public void onClose(Session session, CloseReason closeReason) {
    System.out.println(String.format("Session %s closed because of %s", session.getId(), closeReason));
  }
}
