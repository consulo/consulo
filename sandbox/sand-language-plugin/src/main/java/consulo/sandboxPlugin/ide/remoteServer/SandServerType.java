/*
 * Copyright 2013-2019 consulo.io
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
package consulo.sandboxPlugin.ide.remoteServer;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.remoteServer.RemoteServerConfigurable;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.*;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.remoteServer.runtime.ServerTaskExecutor;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VISTALL
 * @since 2019-02-25
 */
@ExtensionImpl
public class SandServerType extends ServerType<SandServerConfiguration> {
    public SandServerType() {
        super("sand", "SandDeployment", LocalizeValue.localizeTODO("Sand"), PlatformIconGroup.actionsHelp());
    }

    @Nonnull
    @Override
    public SandServerConfiguration createDefaultConfiguration() {
        return new SandServerConfiguration();
    }

    @Override
    @NotNull
    public RemoteServerConfigurable createServerConfigurable(SandServerConfiguration configuration) {
        return new RemoteServerConfigurable() {
            @RequiredUIAccess
            @Override
            public boolean isModified() {
                return false;
            }

            @RequiredUIAccess
            @Nullable
            @Override
            public Component createUIComponent() {
                return Label.create("Sand stub UI");
            }

            @RequiredUIAccess
            @Override
            public void apply() throws ConfigurationException {

            }

            @RequiredUIAccess
            @Override
            public void reset() {

            }
        };
    }

    @Override
    public @NotNull DeploymentConfigurator<?, SandServerConfiguration> createDeploymentConfigurator(Project project) {
        return new DeploymentConfigurator<>() {
            @Nonnull
            @Override
            public List<DeploymentSource> getAvailableDeploymentSources() {
                Module[] modules = ModuleManager.getInstance(project).getModules();
                DeploymentSourceFactory deploymentSourceFactory = project.getInstance(DeploymentSourceFactory.class);
                return Arrays.stream(modules)
                    .map(module -> deploymentSourceFactory.createModuleDeploymentSource(module))
                    .collect(Collectors.toList());
            }

            @NotNull
            @Override
            public DeploymentConfiguration createDefaultConfiguration(DeploymentSource source) {
                return new DummyDeploymentConfiguration();
            }

            @Nullable
            @Override
            public SettingsEditor<DeploymentConfiguration> createEditor(DeploymentSource source, @Nullable RemoteServer<SandServerConfiguration> server) {
                return null;
            }
        };
    }

    @Nonnull
    @Override
    public ServerConnector<?> createConnector(@Nonnull SandServerConfiguration configuration, @Nonnull ServerTaskExecutor asyncTasksExecutor) {
        return new ServerConnector<>() {
            @Override
            public void connect(@Nonnull ConnectionCallback<DeploymentConfiguration> callback) {
                callback.errorOccurred("error");
            }
        };
    }
}
