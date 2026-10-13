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
package consulo.remoteServer.impl.internal.platformAware;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileChooser.internal.PlatformFileChooserContributor;
import consulo.fileChooser.internal.PlatformFileChooserContributorProvider;
import consulo.fileChooser.internal.UniversalFileChooserContributor;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.platformAware.PlatformAwareServerType;
import consulo.remoteServer.runtime.ServerConnectionManager;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * A chooser tab per remote server of a platform aware type; opening the tab connects to the server.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ExtensionImpl
public class RemoteServerFileChooserContributorProvider implements PlatformFileChooserContributorProvider {
    private final RemoteServersManager myServersManager;
    private final ServerConnectionManager myConnectionManager;

    @Inject
    public RemoteServerFileChooserContributorProvider(RemoteServersManager serversManager, ServerConnectionManager connectionManager) {
        myServersManager = serversManager;
        myConnectionManager = connectionManager;
    }

    @Override
    public List<UniversalFileChooserContributor> getContributors() {
        List<UniversalFileChooserContributor> contributors = new ArrayList<>();
        for (RemoteServer<?> server : myServersManager.getServers()) {
            if (server.getType() instanceof PlatformAwareServerType<?>) {
                RemoteServerPlatformTarget target = new RemoteServerPlatformTarget(server, myConnectionManager);
                contributors.add(new PlatformFileChooserContributor(target.getDisplayName(), target.getIcon(), target::getPlatform, target::connect));
            }
        }
        return contributors;
    }
}
