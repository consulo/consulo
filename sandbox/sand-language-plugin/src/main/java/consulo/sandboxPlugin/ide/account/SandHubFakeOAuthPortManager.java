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
package consulo.sandboxPlugin.ide.account;

import consulo.annotation.component.ExtensionImpl;
import consulo.builtinWebServer.custom.CustomPortServerManagerBase;
import consulo.logging.Logger;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class SandHubFakeOAuthPortManager extends CustomPortServerManagerBase {
    public static final int PORT = 63399;

    private static final Logger LOG = Logger.getInstance(SandHubFakeOAuthPortManager.class);

    @Override
    public void cannotBind(Exception e, int port) {
        LOG.warn("Cannot start Sand Hub fake OAuth server on port " + port, e);
    }

    @Override
    public int getPort() {
        return PORT;
    }

    @Override
    public boolean isAvailableExternally() {
        return false;
    }
}
