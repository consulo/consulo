/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.artifact.element;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.compiler.CompilerBundle;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactPointerManager;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ArtifactUtil;
import consulo.compiler.localize.CompilerLocalize;
import consulo.project.Project;
import consulo.ui.image.Image;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import java.util.*;

/**
 * @author nik
 */
@ExtensionImpl
public class ArtifactElementType extends ComplexPackagingElementType<ArtifactPackagingElement> {
  @Nonnull
  public static ArtifactElementType getInstance() {
    return getInstance(ArtifactElementType.class);
  }

  @Inject
  public ArtifactElementType() {
    super("artifact", CompilerLocalize.elementTypeNameArtifact());
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public boolean isAvailableForAdd(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact) {
    return !getAvailableArtifacts(context, artifact, false).isEmpty();
  }

  @Override
  @Nonnull
  public List<? extends ArtifactPackagingElement> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact, @Nonnull CompositePackagingElement<?> parent) {
    List<Artifact> artifacts = context.chooseArtifacts(getAvailableArtifacts(context, artifact, false), CompilerBundle.message("dialog.title.choose.artifacts"));
    List<ArtifactPackagingElement> elements = new ArrayList<>();
    for (Artifact selected : artifacts) {
      ArtifactPointerManager pointerManager = ArtifactPointerManager.getInstance(context.getProject());
      elements.add(new ArtifactPackagingElement(pointerManager, pointerManager.create(selected, context.getArtifactModel())));
    }
    return elements;
  }

  @Nonnull
  public static List<? extends Artifact> getAvailableArtifacts(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact, boolean notIncludedOnly) {
    Set<Artifact> result = new HashSet<>(Arrays.asList(context.getArtifactModel().getArtifacts()));
    if (notIncludedOnly) {
      ArtifactUtil.processPackagingElements(artifact, getInstance(), artifactPackagingElement -> {
        result.remove(artifactPackagingElement.findArtifact(context));
        return true;
      }, context, true);
    }
    result.remove(artifact);
    Iterator<Artifact> iterator = result.iterator();
    while (iterator.hasNext()) {
      Artifact another = iterator.next();
      boolean notContainThis = ArtifactUtil.processPackagingElements(another, getInstance(), element -> !artifact.getName().equals(element.getArtifactName()), context, true);
      if (!notContainThis) {
        iterator.remove();
      }
    }
    ArrayList<Artifact> list = new ArrayList<>(result);
    Collections.sort(list, ArtifactManager.ARTIFACT_COMPARATOR);
    return list;
  }

  @Override
  @Nonnull
  public ArtifactPackagingElement createEmpty(@Nonnull Project project) {
    return new ArtifactPackagingElement(ArtifactPointerManager.getInstance(project));
  }

  @Override
  public String getShowContentActionText() {
    return "Show Content of Included Artifacts";
  }
}
