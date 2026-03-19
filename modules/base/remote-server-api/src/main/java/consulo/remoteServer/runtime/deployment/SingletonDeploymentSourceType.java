// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.remoteServer.runtime.deployment;

import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.remoteServer.configuration.deployment.DeploymentSource;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;
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

    public SingletonDeploymentSourceType(String id, LocalizeValue name, Image icon) {
        super(id);
        myPresentableName = name;
        mySourceInstance = new SingletonDeploymentSource(icon, getClass());
    }

    protected static <T extends SingletonDeploymentSourceType> T findExtension(Class<? extends T> clazz) {
        return DeploymentSourceType.EP_NAME.findExtension(clazz);
    }

    public DeploymentSource getSingletonSource() {
        return mySourceInstance;
    }

    @Override
    public void save(DeploymentSource source, Element tag) {
        //
    }

    @Override
    public DeploymentSource load(Element tag, Project project) {
        return getSingletonSource();
    }

    public final LocalizeValue getPresentableName() {
        return myPresentableName;
    }

    private static class SingletonDeploymentSource implements DeploymentSource {
        private final Class<? extends SingletonDeploymentSourceType> myTypeClass;
        private final Image myIcon;

        SingletonDeploymentSource(Image icon, Class<? extends SingletonDeploymentSourceType> typeClass) {
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
        public @Nullable Image getIcon() {
            return myIcon;
        }

        @Override
        
        public final LocalizeValue getPresentableName() {
            return getType().getPresentableName();
        }

        @Override
        public SingletonDeploymentSourceType getType() {
            return findExtension(myTypeClass);
        }
    }
}
