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
package consulo.http.impl.internal.ws;

import consulo.http.ws.WebSocketConnectionBuilder;
import consulo.http.ws.WebSocketSession;
import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-03-15
 */
public abstract class WebSocketConnectionBuilderImpl implements WebSocketConnectionBuilder {
    protected Consumer<WebSocketSession> myOnOpen;
    protected BiConsumer<WebSocketSession, String> myOnText;
    protected BiConsumer<WebSocketSession, byte[]> myOnBinary;
    protected Consumer<WebSocketSession> myOnClose;
    protected BiConsumer<WebSocketSession, Throwable> myOnError;

    @Override
    public WebSocketConnectionBuilder onOpen(@Nonnull Consumer<WebSocketSession> consumer) {
        myOnOpen = consumer;
        return this;
    }

    @Override
    public WebSocketConnectionBuilder onText(@Nonnull BiConsumer<WebSocketSession, String> consumer) {
        myOnText = consumer;
        return this;
    }

    @Override
    public WebSocketConnectionBuilder onBinary(@Nonnull BiConsumer<WebSocketSession, byte[]> consumer) {
        myOnBinary = consumer;
        return this;
    }

    @Override
    public WebSocketConnectionBuilder onClose(@Nonnull Consumer<WebSocketSession> consumer) {
        myOnClose = consumer;
        return this;
    }

    @Override
    public WebSocketConnectionBuilder onError(@Nonnull BiConsumer<WebSocketSession, Throwable> consumer) {
        myOnError = consumer;
        return this;
    }
}
