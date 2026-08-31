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
package consulo.mcpServer.impl.internal.http;

import consulo.annotation.component.ExtensionImpl;
import consulo.builtinWebServer.custom.CustomPortServerManagerBase;
import consulo.logging.Logger;
import consulo.mcpServer.impl.internal.setting.McpServerSettings;
import jakarta.inject.Inject;

/**
 * Binds a second listener dedicated to MCP, so that agents get a stable URL that does not move when
 * the built-in web server is reconfigured, and so that MCP can be turned off without affecting it.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
@ExtensionImpl
public class McpCustomPortServerManager extends CustomPortServerManagerBase {
    private static final Logger LOG = Logger.getInstance(McpCustomPortServerManager.class);

    private final McpServerSettings mySettings;

    @Inject
    public McpCustomPortServerManager(McpServerSettings settings) {
        mySettings = settings;
    }

    @Override
    public void cannotBind(Exception e, int port) {
        LOG.warn("Cannot start MCP server on port " + port, e);
    }

    @Override
    public int getPort() {
        return mySettings.isEnabled() ? mySettings.getPort() : -1;
    }

    @Override
    public boolean isAvailableExternally() {
        return false;
    }
}
