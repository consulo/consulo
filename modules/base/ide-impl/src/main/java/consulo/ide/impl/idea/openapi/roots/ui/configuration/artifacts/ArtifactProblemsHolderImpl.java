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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ConfigurationErrorQuickFix;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureProblemType;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureProblemsHolder;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.PackagingElementPath;
import consulo.ide.impl.idea.packaging.impl.ui.ArtifactProblemsHolderBase;
import consulo.compiler.artifact.ui.ArtifactEditor;
import consulo.compiler.artifact.ui.ArtifactProblemQuickFix;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author nik
 */
public class ArtifactProblemsHolderImpl extends ArtifactProblemsHolderBase {
  private final ArtifactsStructureConfigurableContext myContext;
  private final Artifact myOriginalArtifact;
  private final ProjectStructureProblemsHolder myProblemsHolder;

  public ArtifactProblemsHolderImpl(ArtifactsStructureConfigurableContext context,
                                    Artifact originalArtifact,
                                    ProjectStructureProblemsHolder problemsHolder) {
    super(context);
    myContext = context;
    myOriginalArtifact = originalArtifact;
    myProblemsHolder = problemsHolder;
  }

  @Override
  public void registerError(@Nonnull String message,
                            @Nonnull String problemTypeId,
                            @Nullable List<PackagingElement<?>> pathToPlace,
                            @Nonnull ArtifactProblemQuickFix... quickFixes) {
    registerProblem(message, pathToPlace, ProjectStructureProblemType.error(problemTypeId), quickFixes);
  }

  @Override
  public void registerWarning(@Nonnull String message,
                              @Nonnull String problemTypeId, @Nullable List<PackagingElement<?>> pathToPlace,
                              @Nonnull ArtifactProblemQuickFix... quickFixes) {
    registerProblem(message, pathToPlace, ProjectStructureProblemType.warning(problemTypeId), quickFixes);
  }

  private void registerProblem(@Nonnull String message, @Nullable List<PackagingElement<?>> pathToPlace,
                               ProjectStructureProblemType problemType, @Nonnull ArtifactProblemQuickFix... quickFixes) {
    String parentPath;
    PackagingElement<?> element;
    if (pathToPlace != null && !pathToPlace.isEmpty()) {
      parentPath = PackagingElementPath.createPath(pathToPlace.subList(1, pathToPlace.size()-1)).getPathString();
      element = pathToPlace.get(pathToPlace.size() - 1);
    }
    else {
      parentPath = null;
      element = null;
    }
    Artifact artifact = myContext.getArtifactModel().getArtifactByOriginal(myOriginalArtifact);
    PlaceInArtifact place = new PlaceInArtifact(artifact, myContext, parentPath, element);
    myProblemsHolder.registerProblem(new ArtifactProblemDescription(message, problemType, pathToPlace, place, convertQuickFixes(quickFixes)));
  }

  private List<ConfigurationErrorQuickFix> convertQuickFixes(ArtifactProblemQuickFix[] quickFixes) {
    List<ConfigurationErrorQuickFix> result = new SmartList<ConfigurationErrorQuickFix>();
    for (final ArtifactProblemQuickFix fix : quickFixes) {
      result.add(new ConfigurationErrorQuickFix(fix.getActionName()) {
        @Override
        public void performFix(DataContext dataContext) {
          ArtifactEditor editor = myContext.getOrCreateEditor(myOriginalArtifact);
          fix.performFix(((ArtifactEditorEx)editor).getContext());
        }
      });
    }
    return result;
  }
}
