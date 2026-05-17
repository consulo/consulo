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
package consulo.enviroment.remoteAgent;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.remoteServer.platformAware.PlatformAwareServerType;
import consulo.remoteServer.RemoteServerConfigurable;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurationBase;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.remoteServer.runtime.ServerTaskExecutor;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
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
    public RemoteAgentServerType() {
        super("remote-agent", "RemoteAgentDeployment",
            LocalizeValue.localizeTODO("Remote Agent"),
            PlatformIconGroup.webreferencesServer());
    }

    @Override
    public RemoteAgentServerConfiguration createDefaultConfiguration() {
        return new RemoteAgentServerConfiguration();
    }

    @Override
    public RemoteServerConfigurable createServerConfigurable(RemoteAgentServerConfiguration configuration) {
        return new RemoteAgentServerConfigurable(configuration);
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
        return new RemoteAgentServerConnector(configuration);
    }

    private static class RemoteAgentServerConfigurable extends RemoteServerConfigurable {
        private final RemoteAgentServerConfiguration myConfiguration;
        private TextBox myHostField;
        private TextBox myPortField;

        RemoteAgentServerConfigurable(RemoteAgentServerConfiguration configuration) {
            myConfiguration = configuration;
        }

        @RequiredUIAccess
        @Nullable
        @Override
        public Component createUIComponent(Disposable parentDisposable) {
            VerticalLayout layout = VerticalLayout.create();

            layout.add(Label.create(LocalizeValue.localizeTODO("Host:")));
            myHostField = TextBox.create(myConfiguration.getHost());
            layout.add(myHostField);

            layout.add(Label.create(LocalizeValue.localizeTODO("Port:")));
            myPortField = TextBox.create(String.valueOf(myConfiguration.getPort()));
            layout.add(myPortField);

            return layout;
        }

        @RequiredUIAccess
        @Override
        public boolean isModified() {
            if (myHostField == null) {
                return false;
            }
            return !myHostField.getValue().equals(myConfiguration.getHost())
                || !myPortField.getValue().equals(String.valueOf(myConfiguration.getPort()));
        }

        @RequiredUIAccess
        @Override
        public void apply() throws ConfigurationException {
            if (myHostField == null) {
                return;
            }
            myConfiguration.setHost(myHostField.getValue());
            try {
                myConfiguration.setPort(Integer.parseInt(myPortField.getValue()));
            }
            catch (NumberFormatException e) {
                throw new ConfigurationException(LocalizeValue.localizeTODO("Invalid port number"));
            }
        }

        @RequiredUIAccess
        @Override
        public void reset() {
            if (myHostField != null) {
                myHostField.setValue(myConfiguration.getHost());
                myPortField.setValue(String.valueOf(myConfiguration.getPort()));
            }
        }
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

        @Nullable
        @Override
        public SettingsEditor<DeploymentConfigurationBase> createEditor(
            DeploymentSource source,
            @Nullable RemoteServer<RemoteAgentServerConfiguration> server) {
            return null;
        }
    }
}
