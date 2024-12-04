// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.application.Application;
import consulo.execution.configuration.RunConfigurationExtensionsManager;
import consulo.execution.configuration.RunProfile;
import consulo.remoteServer.configuration.deployment.DeployToServerRunConfigurationExtension;
import consulo.remoteServer.runtime.deployment.DeploymentTask;
import jakarta.annotation.Nonnull;

public class DeployToServerRunConfigurationExtensionsManager
    extends RunConfigurationExtensionsManager<consulo.remoteServer.configuration.deployment.DeployToServerRunConfiguration<?, ?>, DeployToServerRunConfigurationExtension> {

    public DeployToServerRunConfigurationExtensionsManager() {
        super(DeployToServerRunConfigurationExtension.class);
    }

    public static DeployToServerRunConfigurationExtensionsManager getInstance() {
        return Application.get().getInstance(DeployToServerRunConfigurationExtensionsManager.class);
    }

    public void patchDeploymentTask(@Nonnull DeploymentTask<?> deploymentTask) {
        RunProfile runProfile = deploymentTask.getExecutionEnvironment().getRunProfile();
        if (runProfile instanceof DeployToServerRunConfiguration<?, ?> runConfiguration) {
            processApplicableExtensions(runConfiguration, next -> {
                next.patchDeploymentTask(runConfiguration, deploymentTask);
            });
        }
    }

    public void extendCreatedConfiguration(DeployToServerRunConfiguration<?, ?> configuration) {
        processApplicableExtensions(configuration, next -> {
            next.extendCreatedConfiguration(configuration);
        });
    }
}
