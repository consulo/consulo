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

import consulo.http.impl.internal.ws.WebSocketConnectionBuilderImpl;
import consulo.http.ws.WebSocketSession;
import consulo.logging.Logger;
import consulo.util.lang.ref.SimpleReference;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author VISTALL
 * @since 2026-03-15
 */
public class LocalWebSocketConnectionBuilderImpl extends WebSocketConnectionBuilderImpl {
    private static final Logger LOG = Logger.getInstance(LocalWebSocketConnectionBuilderImpl.class);

    @Override
    public WebSocketSession connect(String url) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        SimpleReference<LocalWebSocketSession> sessionRef = new SimpleReference<>();

        WebSocket webSocket = client.newWebSocketBuilder()
            .buildAsync(URI.create(url), new WebSocket.Listener() {
                private final StringBuilder myBuff = new StringBuilder();

                @Override
                public void onOpen(WebSocket webSocket) {
                    webSocket.request(1);

                    if (myOnOpen != null) {
                        myOnOpen.accept(sessionRef.get());
                    }
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    myBuff.append(data);

                    if (last) {
                        String msg = myBuff.toString();

                        myBuff.setLength(0);

                        if (myOnText != null) {
                            myOnText.accept(sessionRef.get(), msg);
                        }
                    }

                    webSocket.request(1);
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                    webSocket.request(1);
                    // TODO onBinary
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    if (myOnClose != null) {
                        myOnClose.accept(sessionRef.get());
                    }

                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    LOG.warn(error);

                    if (myOnError != null) {
                        myOnError.accept(sessionRef.get(), error);
                    }
                }
            }).join();

        LocalWebSocketSession session = new LocalWebSocketSession(client, webSocket);
        sessionRef.set(session);
        return session;
    }
}
