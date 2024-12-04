// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.annotation.component.ServiceImpl;
import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunConfigurationEditor;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.project.Project;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurationManager;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.remoteServer.localize.RemoteServerLocalize;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.annotation.Nullable;

import java.util.List;

@Singleton
@ServiceImpl
public final class DeploymentConfigurationManagerImpl extends DeploymentConfigurationManager {

    private final @Nonnull Project myProject;

    @Inject
    public DeploymentConfigurationManagerImpl(@Nonnull Project project) {
        myProject = project;
    }

    @Override
    @Nonnull
    public List<RunnerAndConfigurationSettings> getDeploymentConfigurations(@Nonnull ServerType<?> serverType) {
        DeployToServerConfigurationType<?> configurationType = DeployToServerConfigurationTypesRegistrar.getConfigurationType(serverType);
        return RunManager.getInstance(myProject).getConfigurationSettingsList(configurationType);
    }

    @Override
    public void createAndRunConfiguration(@Nonnull ServerType<?> serverType,
                                          @Nullable RemoteServer<?> remoteServer,
                                          @Nullable DeploymentSourceType<?> sourceType) {
        DeployToServerConfigurationType<?> configurationType = DeployToServerConfigurationTypesRegistrar.getConfigurationType(serverType);
        RunManager runManager = RunManager.getInstance(myProject);
        ConfigurationFactory factory = configurationType.getFactoryForType(sourceType);
        RunnerAndConfigurationSettings settings = runManager.createRunConfiguration(configurationType.getDisplayName().get(), factory);
        DeployToServerRunConfiguration<?, ?> runConfiguration = (DeployToServerRunConfiguration<?, ?>) settings.getConfiguration();
        runConfiguration.onNewConfigurationCreated();
        if (remoteServer != null) {
            runConfiguration.setServerName(remoteServer.getName());
        }

        boolean result = RunConfigurationEditor.getInstance(myProject).editConfiguration(myProject, settings, RemoteServerLocalize.dialogTitleCreateDeploymentConfiguration().get(),
            DefaultRunExecutor.getRunExecutorInstance());

        if (result) {
            runManager.addConfiguration(settings);
            runManager.setSelectedConfiguration(settings);
            ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance());
        }
    }
}
