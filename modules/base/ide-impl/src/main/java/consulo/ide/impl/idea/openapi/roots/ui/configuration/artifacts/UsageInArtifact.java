package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.PlaceInProjectStructure;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElementUsage;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.ui.image.Image;

import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class UsageInArtifact extends ProjectStructureElementUsage {
  private final Artifact myOriginalArtifact;
  private final ArtifactsStructureConfigurableContext myContext;
  private final ProjectStructureElement mySourceElement;
  private final ProjectStructureElement myContainingElement;
  private final String myParentPath;
  private final PackagingElement<?> myPackagingElement;

  public UsageInArtifact(Artifact originalArtifact,
                         ArtifactsStructureConfigurableContext context,
                         ProjectStructureElement sourceElement,
                         ArtifactProjectStructureElement containingElement,
                         String parentPath,
                         PackagingElement<?> packagingElement) {
    myOriginalArtifact = originalArtifact;
    myContext = context;
    mySourceElement = sourceElement;
    myContainingElement = containingElement;
    myParentPath = parentPath;
    myPackagingElement = packagingElement;
  }

  @Override
  public ProjectStructureElement getSourceElement() {
    return mySourceElement;
  }

  @Override
  public ProjectStructureElement getContainingElement() {
    return myContainingElement;
  }

  public void removeElement() {
    getOrCreateEditor().removePackagingElement(myParentPath, myPackagingElement);
  }

  private ArtifactEditorEx getOrCreateEditor() {
    return (ArtifactEditorEx)myContext.getOrCreateEditor(myOriginalArtifact);
  }

  public void replaceElement(PackagingElement<?> replacement) {
    getOrCreateEditor().replacePackagingElement(myParentPath, myPackagingElement, replacement);
  }

  @Override
  public String getPresentableName() {
    return myOriginalArtifact.getName();
  }

  @Override
  public PlaceInProjectStructure getPlace() {
    return new PlaceInArtifact(myOriginalArtifact, myContext, myParentPath, myPackagingElement);
  }

  @Override
  public int hashCode() {
    return myOriginalArtifact.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof UsageInArtifact && ((UsageInArtifact)obj).myOriginalArtifact.equals(myOriginalArtifact);
  }

  @Override
  public Image getIcon() {
    return myOriginalArtifact.getArtifactType().getIcon();
  }

  @Nullable
  @Override
  public String getPresentableLocationInElement() {
    return "[" + myParentPath + "]";
  }

  @Override
  public void removeSourceElement() {
    removeElement();
  }
}
