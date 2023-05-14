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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems;

import consulo.project.Project;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactPointer;
import consulo.compiler.artifact.ArtifactPointerManager;
import consulo.compiler.artifact.ArtifactPointerUtil;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementOutputKind;
import consulo.ide.impl.packaging.impl.artifacts.ZipArtifactType;
import consulo.compiler.artifact.ui.ArtifactElementPresentation;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.compiler.artifact.ui.SourceItemPresentation;
import consulo.ide.impl.idea.packaging.ui.SourceItemWeights;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactSourceItem extends PackagingSourceItem {
  private final Artifact myArtifact;

  public ArtifactSourceItem(@Nonnull Artifact artifact) {
    myArtifact = artifact;
  }

  @Override
  public SourceItemPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    final ArtifactPointer pointer = ArtifactPointerUtil.getPointerManager(context.getProject()).create(myArtifact, context.getArtifactModel());
    return new DelegatedSourceItemPresentation(new ArtifactElementPresentation(pointer, context)) {
      @Override
      public int getWeight() {
        return SourceItemWeights.ARTIFACT_WEIGHT;
      }
    };
  }

  @Override
  @Nonnull
  public List<? extends PackagingElement<?>> createElements(@Nonnull ArtifactEditorContext context) {
    final Project project = context.getProject();
    final ArtifactPointer pointer = ArtifactPointerManager.getInstance(project).create(myArtifact, context.getArtifactModel());
    return Collections.singletonList(PackagingElementFactory.getInstance(context.getProject()).createArtifactElement(pointer, project));
  }

  public boolean equals(Object obj) {
    return obj instanceof ArtifactSourceItem && myArtifact.equals(((ArtifactSourceItem)obj).myArtifact);
  }

  @Nonnull
  @Override
  public PackagingElementOutputKind getKindOfProducedElements() {
    return myArtifact.getArtifactType() instanceof ZipArtifactType ? PackagingElementOutputKind.JAR_FILES : PackagingElementOutputKind.OTHER;
  }

  public Artifact getArtifact() {
    return myArtifact;
  }

  public int hashCode() {
    return myArtifact.hashCode();
  }
}
