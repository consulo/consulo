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
package consulo.builtinWebServer.webSocket;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-06-14
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface WebSocketAccepter {
    ExtensionPointName<WebSocketAccepter> EP_NAME = ExtensionPointName.create(WebSocketAccepter.class);

    void accept(@Nonnull WebSocketConnection connection, @Nonnull byte[] array);

    void accept(@Nonnull WebSocketConnection connection, @Nonnull String text);
}
