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

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
@ServerEndpoint(value = "/ui")
public class UIWebSocketHandler {
  @OnOpen
  public void onOpen(Session session) {
    System.out.println("Connected ... " + session.getId());

    session.getAsyncRemote().sendText("test me");
  }

  @OnMessage
  public String onMessage(String message, Session session) {
    if (message.equals("quit")) {
      try {
        session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Game ended"));
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }

    }
    return message;
  }

  @OnClose
  public void onClose(Session session, CloseReason closeReason) {
    System.out.println(String.format("Session %s closed because of %s", session.getId(), closeReason));
  }
}
