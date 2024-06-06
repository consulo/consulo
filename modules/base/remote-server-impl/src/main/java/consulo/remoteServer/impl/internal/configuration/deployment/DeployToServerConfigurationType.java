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
package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.remoteServer.impl.internal.configuration.localServer.LocalServerRunConfiguration;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.remoteServer.runtime.local.LocalRunner;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author nik
 */
public class DeployToServerConfigurationType extends ConfigurationTypeBase {
  private final ServerType<?> myServerType;

  public DeployToServerConfigurationType(ServerType<?> serverType) {
    super(serverType.getId() + "-deploy", RemoteServerLocalize.deployToServerDisplayName(serverType.getPresentableName()), RemoteServerLocalize.deployToServerDisplayDescription(
      serverType.getPresentableName()), serverType.getIcon());
    addFactory(new DeployToServerConfigurationFactory());
    LocalRunner localRunner = serverType.getLocalRunner();
    if (localRunner != null) {
      addFactory(new LocalServerConfigurationFactory(localRunner));
    }
    myServerType = serverType;
  }

  public class DeployToServerConfigurationFactory extends ConfigurationFactory {
    public DeployToServerConfigurationFactory() {
      super(DeployToServerConfigurationType.this);
    }

    @Nonnull
    @Override
    public String getId() {
      return getType() + "#remote";
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
      return RemoteServerLocalize.deployToServerFactoryRemote();
    }

    @Override
    public boolean isApplicable(@Nonnull Project project) {
      return myServerType.isConfigurationTypeIsAvailable(project);
    }

    @Override
    public void onNewConfigurationCreated(@Nonnull RunConfiguration configuration) {
      DeployToServerRunConfiguration deployConfiguration = (DeployToServerRunConfiguration)configuration;
      if (deployConfiguration.getServerName() == null) {
        RemoteServer<?> server = ContainerUtil.getFirstItem(RemoteServersManager.getInstance().getServers(myServerType));
        if (server != null) {
          deployConfiguration.setServerName(server.getName());
        }
      }

      if (deployConfiguration.getDeploymentSource() == null) {
        List<DeploymentSource> sources = deployConfiguration.getDeploymentConfigurator().getAvailableDeploymentSources();
        DeploymentSource source = ContainerUtil.getFirstItem(sources);
        if (source != null) {
          deployConfiguration.setDeploymentSource(source);
          DeploymentSourceType type = source.getType();
          type.setBuildBeforeRunTask(configuration, source);
        }
      }
    }

    @Override
    public RunConfiguration createTemplateConfiguration(Project project) {
      DeploymentConfigurator<?> deploymentConfigurator = myServerType.createDeploymentConfigurator(project);
      return new DeployToServerRunConfiguration(project, this, "", myServerType, deploymentConfigurator);
    }
  }

  public class LocalServerConfigurationFactory extends ConfigurationFactory {
    private final LocalRunner myLocalRunner;

    public LocalServerConfigurationFactory(LocalRunner localRunner) {
      super(DeployToServerConfigurationType.this);
      myLocalRunner = localRunner;
    }

    @Nonnull
    @Override
    public String getId() {
      return getType().getId() + "#local";
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
      return RemoteServerLocalize.deployToServerFactoryLocal();
    }

    @Override
    public void onNewConfigurationCreated(@Nonnull RunConfiguration configuration) {
      LocalServerRunConfiguration deployConfiguration = (LocalServerRunConfiguration)configuration;

      if (deployConfiguration.getDeploymentSource() == null) {
        List<DeploymentSource> sources = deployConfiguration.getDeploymentConfigurator().getAvailableDeploymentSources();
        DeploymentSource source = ContainerUtil.getFirstItem(sources);
        if (source != null) {
          deployConfiguration.setDeploymentSource(source);
          DeploymentSourceType type = source.getType();
          type.setBuildBeforeRunTask(configuration, source);
        }
      }
    }

    @Override
    public boolean isApplicable(@Nonnull Project project) {
      return myServerType.isConfigurationTypeIsAvailable(project);
    }

    @Override
    public RunConfiguration createTemplateConfiguration(Project project) {
      DeploymentConfigurator<?> deploymentConfigurator = myServerType.createDeploymentConfigurator(project);
      return new LocalServerRunConfiguration(project, this, "", myServerType, deploymentConfigurator, myLocalRunner);
    }
  }
}
