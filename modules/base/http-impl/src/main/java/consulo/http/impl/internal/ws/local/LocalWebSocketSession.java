/*
 * Copyright 2013-2026 consulo.io
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
package consulo.http.impl.internal.ws.local;

import consulo.http.ws.WebSocketSession;
import consulo.util.dataholder.UserDataHolderBase;

import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;

/**
 * @author VISTALL
 * @since 2026-03-15
 */
public class LocalWebSocketSession extends UserDataHolderBase implements WebSocketSession {
    private final HttpClient myClient;
    private final WebSocket myWebSocket;

    public LocalWebSocketSession(HttpClient client, WebSocket webSocket) {
        myClient = client;
        myWebSocket = webSocket;
    }

    @Override
    public void send(String text) {
        myWebSocket.sendText(text, true);
    }

    @Override
    public void send(byte[] data) {
        myWebSocket.sendBinary(ByteBuffer.wrap(data), true);
    }

    @Override
    public void close() {
        myWebSocket.abort();

        myClient.close();
    }
}
