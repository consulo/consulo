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
package consulo.remoteServer.agent.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.credentialStorage.PasswordSafe;
import consulo.remoteServer.agent.impl.icon.RemoteAgentIconGroup;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.remoteServer.RemoteServerConfigurable;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurationBase;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.platformAware.PlatformAwareServerType;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.remoteServer.runtime.ServerTaskExecutor;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * ServerType extension for Consulo remote agent connections.
 * Registers in the Servers toolwindow.
 *
 * @author VISTALL
 * @since 2026-03-17
 */
@ExtensionImpl
public class RemoteAgentServerType extends PlatformAwareServerType<RemoteAgentServerConfiguration> {
    private final ApplicationConcurrency myApplicationConcurrency;
    private final RemoteAgentCredentials myCredentials;

    @Inject
    public RemoteAgentServerType(ApplicationConcurrency applicationConcurrency, PasswordSafe passwordSafe) {
        super("remote-agent", "RemoteAgentDeployment",
            LocalizeValue.localizeTODO("Consulo Remote Agent"),
            RemoteAgentIconGroup.remoteagent());
        myApplicationConcurrency = applicationConcurrency;
        myCredentials = new RemoteAgentCredentials(passwordSafe, applicationConcurrency.executor());
    }

    @Override
    public RemoteAgentServerConfiguration createDefaultConfiguration() {
        return new RemoteAgentServerConfiguration();
    }

    @Override
    public RemoteServerConfigurable createServerConfigurable(RemoteAgentServerConfiguration configuration) {
        return new RemoteAgentServerConfigurable(configuration, myCredentials);
    }

    @Override
    public DeploymentConfigurator<?, RemoteAgentServerConfiguration> createDeploymentConfigurator(Project project) {
        return new RemoteAgentDeploymentConfigurator();
    }

    @Override
    public boolean mayHaveProjectSpecificDeploymentSources() {
        return false;
    }

    @Override
    public ServerConnector<?> createConnector(RemoteAgentServerConfiguration configuration,
                                              ServerTaskExecutor asyncTasksExecutor) {
        return new RemoteAgentServerConnector(configuration, myCredentials, myApplicationConcurrency.executor());
    }

    private static class RemoteAgentDeploymentConfigurator
        extends DeploymentConfigurator<DeploymentConfigurationBase, RemoteAgentServerConfiguration> {
        @Override
        public List<DeploymentSource> getAvailableDeploymentSources() {
            return Collections.emptyList();
        }

        @Override
        public DeploymentConfigurationBase createDefaultConfiguration(DeploymentSource source) {
            return new DeploymentConfigurationBase() {
            };
        }

        @Override
        public @Nullable RunConfigurationFragmentEditor<DeploymentConfigurationBase> createEditor(
            DeploymentSource source,
            @Nullable RemoteServer<RemoteAgentServerConfiguration> server) {
            return null;
        }
    }
}
