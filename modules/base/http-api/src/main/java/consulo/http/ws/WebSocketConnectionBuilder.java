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
package consulo.http.ws;


import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-03-15
 */
public interface WebSocketConnectionBuilder {
    WebSocketConnectionBuilder onOpen(Consumer<WebSocketSession> consumer);

    WebSocketConnectionBuilder onText(BiConsumer<WebSocketSession, String> consumer);

    WebSocketConnectionBuilder onBinary(BiConsumer<WebSocketSession, byte[]> consumer);

    WebSocketConnectionBuilder onClose(Consumer<WebSocketSession> consumer);

    WebSocketConnectionBuilder onError(BiConsumer<WebSocketSession, Throwable> consumer);

    
    WebSocketSession connect(String url) throws Exception;
}
