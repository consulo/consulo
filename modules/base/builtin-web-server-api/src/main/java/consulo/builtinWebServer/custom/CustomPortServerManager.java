/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.builtinWebServer.custom;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import jakarta.annotation.Nullable;

import java.util.Map;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class CustomPortServerManager {
    public abstract void cannotBind(Exception e, int port);

    public interface CustomPortService {
        boolean rebind();

        boolean isBound();
    }

    public abstract int getPort();

    public abstract boolean isAvailableExternally();

    public abstract void setManager(@Nullable CustomPortService manager);

    /**
     * This server will accept only XML-RPC requests if this method returns not-null map of XMl-RPC handlers
     */
    @Nullable
    public Map<String, Object> createXmlRpcHandlers() {
        return null;
    }
}