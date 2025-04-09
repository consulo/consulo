package consulo.remoteServer.impl.internal.runtime.deployment;

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactPointer;
import consulo.compiler.artifact.element.ArtifactRootElement;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.localize.LocalizeValue;
import consulo.remoteServer.configuration.deployment.ArtifactDeploymentSource;
import consulo.remoteServer.configuration.deployment.DeploymentSourceType;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;

import jakarta.annotation.Nonnull;

import java.io.File;

/**
 * @author nik
 */
public class ArtifactDeploymentSourceImpl implements ArtifactDeploymentSource {
    private final ArtifactPointer myPointer;

    public ArtifactDeploymentSourceImpl(@Nonnull ArtifactPointer pointer) {
        myPointer = pointer;
    }

    @Nonnull
    @Override
    public ArtifactPointer getArtifactPointer() {
        return myPointer;
    }

    @Override
    public Artifact getArtifact() {
        return myPointer.get();
    }

    @Override
    public File getFile() {
        String path = getFilePath();
        return path != null ? new File(path) : null;
    }

    @Override
    public String getFilePath() {
        Artifact artifact = getArtifact();
        if (artifact != null) {
            String outputPath = artifact.getOutputPath();
            if (outputPath != null) {
                CompositePackagingElement<?> rootElement = artifact.getRootElement();
                if (!(rootElement instanceof ArtifactRootElement<?>)) {
                    outputPath += "/" + rootElement.getName();
                }
                return FileUtil.toSystemDependentName(outputPath);
            }
        }
        return null;
    }

    @Nonnull
    @Override
    public LocalizeValue getPresentableName() {
        return LocalizeValue.of(myPointer.getName());
    }

    @Override
    public Image getIcon() {
        Artifact artifact = getArtifact();
        return artifact != null ? artifact.getArtifactType().getIcon() : null;
    }

    @Override
    public boolean isValid() {
        return getArtifact() != null;
    }

    @Override
    public boolean isArchive() {
        Artifact artifact = getArtifact();
        return artifact != null && !(artifact.getRootElement() instanceof ArtifactRootElement<?>);
    }

    @Override
    public boolean equals(Object o) {
        return o == this
            || o instanceof ArtifactDeploymentSourceImpl that
            && myPointer.equals(that.myPointer);
    }

    @Override
    public int hashCode() {
        return myPointer.hashCode();
    }

    @Nonnull
    @Override
    public DeploymentSourceType<?> getType() {
        return DeploymentSourceType.EP_NAME.findExtensionOrFail(ArtifactDeploymentSourceType.class);
    }
}
