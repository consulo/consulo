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
package consulo.remoteServer.configuration.deployment;

import consulo.execution.configuration.ui.SettingsEditor;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public abstract class DeploymentConfigurator<D extends DeploymentConfiguration, S extends ServerConfiguration> {
    @Nonnull
    public abstract List<DeploymentSource> getAvailableDeploymentSources();

    @Nonnull
    public abstract D createDefaultConfiguration(@Nonnull DeploymentSource source);

    @Nullable
    public abstract SettingsEditor<D> createEditor(@Nonnull DeploymentSource source, @Nullable RemoteServer<S> server);

    /**
     * @see LocatableConfiguration#isGeneratedName()
     */
    public boolean isGeneratedConfigurationName(@Nonnull String name,
                                                @Nonnull DeploymentSource deploymentSource,
                                                @Nonnull D deploymentConfiguration) {
        return false;
    }

    /**
     * @see LocatableConfiguration#suggestedName()
     */
    @Nullable
    public String suggestConfigurationName(@Nonnull DeploymentSource deploymentSource, @Nonnull D deploymentConfiguration) {
        return null;
    }
}
