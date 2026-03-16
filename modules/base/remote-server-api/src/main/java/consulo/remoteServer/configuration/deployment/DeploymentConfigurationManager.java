// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.configuration.deployment;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.project.Project;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import org.jspecify.annotations.Nullable;

import java.util.List;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class DeploymentConfigurationManager {
    
    public static DeploymentConfigurationManager getInstance(Project project) {
        return project.getInstance(DeploymentConfigurationManager.class);
    }

    public abstract List<RunnerAndConfigurationSettings> getDeploymentConfigurations(ServerType<?> serverType);

    public abstract void createAndRunConfiguration(ServerType<?> serverType,
                                                   @Nullable RemoteServer<?> remoteServer,
                                                   @Nullable DeploymentSourceType<?> sourceType);
}
