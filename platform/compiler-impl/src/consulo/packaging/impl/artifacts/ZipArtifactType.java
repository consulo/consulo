package consulo.packaging.impl.artifacts;

import com.intellij.icons.AllIcons;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementOutputKind;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import consulo.packaging.impl.elements.ZipArchivePackagingElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 15:59/18.06.13
 */
public class ZipArtifactType extends ArtifactType {
  protected ZipArtifactType() {
    super("zip", "Zip");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public String getDefaultPathFor(@NotNull PackagingElementOutputKind kind) {
    return "/";
  }

  @NotNull
  @Override
  public CompositePackagingElement<?> createRootElement(@NotNull String artifactName) {
    return new ZipArchivePackagingElement(ArtifactUtil.suggestArtifactFileName(artifactName) + ".zip");
  }
}
