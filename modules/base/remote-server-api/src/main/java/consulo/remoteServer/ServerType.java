// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.remoteServer;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.remoteServer.configuration.RemoteServer;
import consulo.remoteServer.configuration.ServerConfiguration;
import consulo.remoteServer.configuration.deployment.DeploymentConfigurator;
import consulo.remoteServer.runtime.Deployment;
import consulo.remoteServer.runtime.ServerConnector;
import consulo.remoteServer.runtime.ServerTaskExecutor;
import consulo.remoteServer.runtime.deployment.SingletonDeploymentSourceType;
import consulo.remoteServer.runtime.deployment.debug.DebugConnector;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ServerType<C extends ServerConfiguration> {

    public static final ExtensionPointName<ServerType> EP_NAME = ExtensionPointName.create(ServerType.class);

    private final String myId;
    @Nonnull
    private final String myDeploymentId;
    private final LocalizeValue myPresentableName;
    private final Image myIcon;

    protected ServerType(@NotNull String id, @Nonnull String deploymentId, LocalizeValue presentableName, Image icon) {
        myId = id;
        myDeploymentId = deploymentId;
        myPresentableName = presentableName;
        myIcon = icon;
    }

    @Nonnull
    public final String getId() {
        return myId;
    }

    public final LocalizeValue getPresentableName() {
        return myPresentableName;
    }

    /**
     * This method must be overridden and a proper ID must be returned from it (it'll be used as a key in run configuration file).
     */
    @Nonnull
    public final String getDeploymentConfigurationFactoryId() {
        return myDeploymentId;
    }

    @NotNull
    public String getHelpTopic() {
        return "reference.settings.clouds";
    }

    @NotNull
    public final Image getIcon() {
        return myIcon;
    }

    /**
     * Returns whether the instance returned from {@link #createDefaultConfiguration()} has <em>reasonably good</em> chances to work correctly.
     * The auto-detected instance is <em>not</em> required to work perfectly, connection to it will be tested, and the instance will
     * be persisted only if the test is successful.
     * <p>
     * The capability to auto-detect configurations will unlock UI elements which normally require user to manually configure the server.
     * E.g. deployments for auto-detecting server types will always be shown in the 'New' popup in 'Edit Configurations' dialog.
     */
    public boolean canAutoDetectConfiguration() {
        return false;
    }

    @NotNull
    public abstract C createDefaultConfiguration();

    @NotNull
    public RemoteServerConfigurable createServerConfigurable(@NotNull C configuration) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public abstract DeploymentConfigurator<?, C> createDeploymentConfigurator(Project project);

    /**
     * Returns list of the singleton deployment sources types available in addition to the project-dependent deployment sources
     * enumerated via {@link DeploymentConfigurator#getAvailableDeploymentSources()}.
     */
    public List<SingletonDeploymentSourceType> getSingletonDeploymentSourceTypes() {
        return Collections.emptyList();
    }

    /**
     * @return <code>false</code>, if all supported deployment sources are of {@link SingletonDeploymentSourceType} type, so
     * {@link DeploymentConfigurator#getAvailableDeploymentSources()} <strong>now is and always will be</strong> empty.
     */
    public boolean mayHaveProjectSpecificDeploymentSources() {
        return true;
    }

    public abstract @NotNull ServerConnector<?> createConnector(@NotNull C configuration, @NotNull ServerTaskExecutor asyncTasksExecutor);

    public @NotNull ServerConnector<?> createConnector(@NotNull RemoteServer<C> server, @NotNull ServerTaskExecutor asyncTasksExecutor) {
        return createConnector(server.getConfiguration(), asyncTasksExecutor);
    }

    /**
     * @return a non-null instance of {@link DebugConnector} if the server supports deployment in debug mode
     */
    public @Nullable DebugConnector<?, ?> createDebugConnector() {
        return null;
    }

    public @NotNull Comparator<Deployment> getDeploymentComparator() {
        return Comparator.comparing(Deployment::getName);
    }

    public @Nullable String getCustomToolWindowId() {
        return null;
    }
}
