// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.runtime.deployment;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.io.File;

/**
 * There may be only a single instance of the deployment source of this type, all the configuration bits are stored in the
 * {@link DeploymentConfiguration}.
 * <p/>
 * Deployment sources of this type are excluded from the choice of the deployment sources in the "generic" deployment run configuration.
 * Instead, deployment configurations for this particular type will be managed by ad hoc
 * {@link com.intellij.execution.configurations.ConfigurationFactory}. Thus, user will not be able to switch from
 * {@link SingletonDeploymentSourceType} to any other deployment source without recreating of the configuration.
 */
public class SingletonDeploymentSourceType extends DeploymentSourceType<DeploymentSource> {
    private final LocalizeValue myPresentableName;
    private final SingletonDeploymentSource mySourceInstance;

    public SingletonDeploymentSourceType(@Nonnull String id, LocalizeValue name, @Nonnull Image icon) {
        super(id);
        myPresentableName = name;
        mySourceInstance = new SingletonDeploymentSource(icon, getClass());
    }

    protected static <T extends SingletonDeploymentSourceType> T findExtension(@Nonnull Class<? extends T> clazz) {
        return DeploymentSourceType.EP_NAME.findExtension(clazz);
    }

    public @Nonnull DeploymentSource getSingletonSource() {
        return mySourceInstance;
    }

    @Override
    public void save(@Nonnull DeploymentSource source, @Nonnull Element tag) {
        //
    }

    @Override
    public @Nonnull DeploymentSource load(@Nonnull Element tag, @Nonnull Project project) {
        return getSingletonSource();
    }

    public final LocalizeValue getPresentableName() {
        return myPresentableName;
    }

    private static class SingletonDeploymentSource implements DeploymentSource {
        private final Class<? extends SingletonDeploymentSourceType> myTypeClass;
        private final Image myIcon;

        SingletonDeploymentSource(@Nonnull Image icon, @Nonnull Class<? extends SingletonDeploymentSourceType> typeClass) {
            myIcon = icon;
            myTypeClass = typeClass;
        }

        @Override
        public final @Nullable File getFile() {
            return null;
        }

        @Override
        public final @Nullable String getFilePath() {
            return null;
        }

        @Override
        public final boolean isArchive() {
            return false;
        }

        @Override
        public final boolean isValid() {
            return true;
        }

        @Override
        @Nullable
        public Image getIcon() {
            return myIcon;
        }

        @Override
        @Nonnull
        public final LocalizeValue getPresentableName() {
            return getType().getPresentableName();
        }

        @Override
        public @Nonnull SingletonDeploymentSourceType getType() {
            return findExtension(myTypeClass);
        }
    }
}
