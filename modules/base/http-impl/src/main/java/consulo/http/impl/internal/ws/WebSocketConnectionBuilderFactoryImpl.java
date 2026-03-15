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

import consulo.annotation.component.ServiceImpl;
import consulo.http.impl.internal.ws.local.LocalWebSocketConnectionBuilderImpl;
import consulo.http.ws.WebSocketConnectionBuilder;
import consulo.http.ws.WebSocketConnectionBuilderFactory;
import consulo.platform.Platform;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-03-15
 */
@Singleton
@ServiceImpl
public class WebSocketConnectionBuilderFactoryImpl implements WebSocketConnectionBuilderFactory {
    @Override
    public WebSocketConnectionBuilder newBuilder(@Nonnull Platform platform) {
        // TODO platform handling
        return new LocalWebSocketConnectionBuilderImpl();
    }
}
