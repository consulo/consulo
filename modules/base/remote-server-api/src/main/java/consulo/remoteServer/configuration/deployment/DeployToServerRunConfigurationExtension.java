// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.configuration.deployment;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.execution.action.Location;
import consulo.execution.configuration.RunConfigurationExtensionBase;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import jakarta.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class DeployToServerRunConfigurationExtension extends RunConfigurationExtensionBase<DeployToServerRunConfiguration<?, ?>> {
    public void patchDeploymentTask(@Nonnull DeployToServerRunConfiguration<?, ?> runConfiguration,
                                    @Nonnull DeploymentTask<?> deploymentTask) {
        //
    }

    @Override
    protected void extendCreatedConfiguration(@Nonnull DeployToServerRunConfiguration<?, ?> configuration,
                                              @Nonnull Location location) {
        extendCreatedConfiguration(configuration);
    }

    public void extendCreatedConfiguration(@Nonnull DeployToServerRunConfiguration<?, ?> configuration) {
        //
    }
}
