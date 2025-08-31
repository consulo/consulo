/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.PlaceInProjectStructure;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureElement;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class PlaceInArtifact extends PlaceInProjectStructure {
  private final Artifact myArtifact;
  private final ArtifactsStructureConfigurableContext myContext;
  private final String myParentPath;
  private final PackagingElement<?> myPackagingElement;

  public PlaceInArtifact(Artifact artifact, ArtifactsStructureConfigurableContext context, @Nullable String parentPath,
                         @Nullable PackagingElement<?> packagingElement) {
    myArtifact = artifact;
    myContext = context;
    myParentPath = parentPath;
    myPackagingElement = packagingElement;
  }

  @Nonnull
  @Override
  public ProjectStructureElement getContainingElement() {
    return myContext.getOrCreateArtifactElement(myArtifact);
  }

  @Override
  public String getPlacePath() {
    if (myParentPath != null && myPackagingElement != null) {
      //todo[nik] use id of element?
      return myParentPath + "/" + myPackagingElement.getType().getId();
    }
    return null;
  }

  @Nonnull
  @Override
  @RequiredUIAccess
  public AsyncResult<Void> navigate(@Nonnull Project project) {
    Artifact artifact = myContext.getArtifactModel().getArtifactByOriginal(myArtifact);
    return ShowSettingsUtil.getInstance().showProjectStructureDialog(project, projectStructureSelector -> {
      projectStructureSelector.select(artifact, true).doWhenDone(() -> {
        ArtifactEditorEx artifactEditor = (ArtifactEditorEx)myContext.getOrCreateEditor(artifact);
        if (myParentPath != null && myPackagingElement != null) {
          artifactEditor.getLayoutTreeComponent().selectNode(myParentPath, myPackagingElement);
        }
      });
    });
  }
}
