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
package consulo.web.internal.servlet;

import consulo.builtinWebServer.webSocket.WebSocketConnection;
import consulo.logging.Logger;
import jakarta.websocket.Session;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
final class JakartaWebSocketConnection implements WebSocketConnection {
    private static final Logger LOG = Logger.getInstance(JakartaWebSocketConnection.class);

    private final Session mySession;

    JakartaWebSocketConnection(Session session) {
        mySession = session;
    }

    @Override
    public void send(String text) {
        if (mySession.isOpen()) {
            mySession.getAsyncRemote().sendText(text);
        }
    }

    @Override
    public void send(byte[] data) {
        if (mySession.isOpen()) {
            mySession.getAsyncRemote().sendBinary(ByteBuffer.wrap(data));
        }
    }

    void sendHeartbeat() {
        if (!mySession.isOpen()) {
            return;
        }

        try {
            mySession.getAsyncRemote().sendPing(ByteBuffer.allocate(0));
        }
        catch (IOException e) {
            LOG.debug(e);
        }
    }
}
