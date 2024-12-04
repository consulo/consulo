// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.impl.internal.configuration.deployment;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.remoteServer.ServerType;
import consulo.remoteServer.configuration.RemoteServersManager;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.remoteServer.runtime.deployment.SingletonDeploymentSourceType;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class DeployToServerConfigurationType<C extends ServerConfiguration> extends ConfigurationTypeBase {

    private final @Nonnull ServerType<C> myServerType;
    private final @Nullable MultiSourcesConfigurationFactory myMultiSourcesFactory;
    private final Map<String, SingletonTypeConfigurationFactory> myPerTypeFactories = new HashMap<>();

    public DeployToServerConfigurationType(@Nonnull ServerType<C> serverType) {
        super(serverType.getId() + "-deploy",
            serverType.getPresentableName(),
            RemoteServerLocalize.deployToServerConfigurationTypeDescription(serverType.getPresentableName()),
            Image.empty(Image.DEFAULT_ICON_SIZE));

        myServerType = serverType;
        if (serverType.mayHaveProjectSpecificDeploymentSources()) {
            myMultiSourcesFactory = new MultiSourcesConfigurationFactory();
            addFactory(myMultiSourcesFactory);
        }
        else {
            myMultiSourcesFactory = null;
        }

        for (SingletonDeploymentSourceType next : serverType.getSingletonDeploymentSourceTypes()) {
            SingletonTypeConfigurationFactory nextFactory = new SingletonTypeConfigurationFactory(next);
            addFactory(nextFactory);
            myPerTypeFactories.put(next.getId(), nextFactory);
        }
    }

    /**
     * @param sourceType hint for a type of deployment source or null if unknown
     */
    public @Nonnull ConfigurationFactory getFactoryForType(@Nullable DeploymentSourceType<?> sourceType) {
        ConfigurationFactory result = null;
        if (sourceType instanceof SingletonDeploymentSourceType &&
            myServerType.getSingletonDeploymentSourceTypes().contains(sourceType)) {
            result = myPerTypeFactories.get(sourceType.getId());
        }
        if (result == null) {
            result = myMultiSourcesFactory;
        }
        assert result != null : "server type: " + myServerType.getId() + ", requested source type: " + sourceType;
        return result;
    }

    @Override
    public @Nonnull Image getIcon() {
        return myServerType.getIcon();
    }

    public String getHelpTopic() {
        return "reference.dialogs.rundebug." + getId();
    }

    // todo do not extends ConfigurationFactoryEx once Google Cloud Tools plugin will get rid of getFactory() usage
    public abstract class DeployToServerConfigurationFactory extends ConfigurationFactory {

        public DeployToServerConfigurationFactory() {
            super(DeployToServerConfigurationType.this);
        }

        @Override
        public boolean isApplicable(@Nonnull Project project) {
            return myServerType.canAutoDetectConfiguration() ||
                !RemoteServersManager.getInstance().getServers(myServerType).isEmpty();
        }

        @Override
        public @Nonnull DeployToServerRunConfiguration<C, ?> createTemplateConfiguration(@Nonnull Project project) {
            DeploymentConfigurator<?, C> deploymentConfigurator = myServerType.createDeploymentConfigurator(project);
            return new DeployToServerRunConfiguration<>(project, this, "", myServerType, deploymentConfigurator);
        }
    }

    public final class MultiSourcesConfigurationFactory extends DeployToServerConfigurationFactory {

        @Override
        public @Nonnull @NonNls String getId() {
            //compatibility reasons, before 173 it was the only configuration factory stored with this ID
            return myServerType.getDeploymentConfigurationFactoryId();
        }
    }

    public final class SingletonTypeConfigurationFactory extends DeployToServerConfigurationFactory {

        private final @Nonnull
        @NonNls String mySourceTypeId;
        private final LocalizeValue myPresentableName;

        public SingletonTypeConfigurationFactory(@Nonnull SingletonDeploymentSourceType sourceType) {
            mySourceTypeId = sourceType.getId();
            myPresentableName = sourceType.getPresentableName();
        }

        @Override
        public @Nonnull @NonNls String getId() {
            return mySourceTypeId;
        }

        @Override
        public LocalizeValue getDisplayName() {
            return myPresentableName;
        }

        @Override
        public @Nonnull DeployToServerRunConfiguration<C, ?> createTemplateConfiguration(@Nonnull Project project) {
            DeployToServerRunConfiguration<C, ?> result = super.createTemplateConfiguration(project);
            DeploymentSourceType<?> type = getSourceTypeImpl();
            if (type instanceof SingletonDeploymentSourceType) {
                result.lockDeploymentSource((SingletonDeploymentSourceType) type);
            }
            return result;
        }

        private @Nullable DeploymentSourceType<?> getSourceTypeImpl() {
            return DeploymentSourceType.EP_NAME.findFirstSafe(next -> mySourceTypeId.equals(next.getId()));
        }

        public boolean isEditableInDumbMode() {
            return getSourceTypeImpl().isEditableInDumbMode();
        }
    }

    public ServerType<C> getServerType() {
        return myServerType;
    }
}

