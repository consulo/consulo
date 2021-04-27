/*
 * Copyright 2013-2020 consulo.io
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
package consulo.sandboxPlugin.webSocket;

import consulo.builtInServer.websocket.WebSocketAccepter;
import consulo.builtInServer.websocket.WebSocketConnection;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

/**
 * @author VISTALL
 * @since 2020-06-14
 */
public class TestWebSocketAccepter implements WebSocketAccepter {
  @Override
  public void accept(@Nonnull WebSocketConnection connection, @Nonnull byte[] array) {
    connection.send("this was byte array".getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public void accept(@Nonnull WebSocketConnection connection, @Nonnull String text) {
    connection.send("reply for " + text);
  }
}
