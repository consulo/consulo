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
package com.intellij.remoteServer.impl.configuration.deployment;

import com.intellij.execution.configuration.ConfigurationFactoryEx;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.remoteServer.ServerType;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator;
import com.intellij.remoteServer.configuration.deployment.DeploymentSource;
import com.intellij.remoteServer.configuration.deployment.DeploymentSourceType;
import com.intellij.remoteServer.configuration.localServer.LocalRunner;
import com.intellij.remoteServer.impl.configuration.localServer.LocalServerRunConfiguration;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author nik
 */
public class DeployToServerConfigurationType extends ConfigurationTypeBase {
  private final ServerType<?> myServerType;

  public DeployToServerConfigurationType(ServerType<?> serverType) {
    super(serverType.getId() + "-deploy", serverType.getPresentableName() + " Deployment", "Deploy to " + serverType.getPresentableName() + " run configuration", serverType.getIcon());
    addFactory(new DeployToServerConfigurationFactory());
    LocalRunner localRunner = serverType.getLocalRunner();
    if (localRunner != null) {
      addFactory(new LocalServerConfigurationFactory(localRunner));
    }
    myServerType = serverType;
  }

  public class DeployToServerConfigurationFactory extends ConfigurationFactoryEx {
    public DeployToServerConfigurationFactory() {
      super(DeployToServerConfigurationType.this);
    }

    @Override
    public String getName() {
      return "Remote";
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

  public class LocalServerConfigurationFactory extends ConfigurationFactoryEx {
    private final LocalRunner myLocalRunner;

    public LocalServerConfigurationFactory(LocalRunner localRunner) {
      super(DeployToServerConfigurationType.this);
      myLocalRunner = localRunner;
    }

    @Override
    public String getName() {
      return "Local";
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
