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

import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ConfigurationErrorQuickFix;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureProblemDescription;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.daemon.ProjectStructureProblemType;
import consulo.compiler.artifact.element.PackagingElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author nik
 */
public class ArtifactProblemDescription extends ProjectStructureProblemDescription {
  private final List<PackagingElement<?>> myPathToPlace;

  public ArtifactProblemDescription(@Nonnull String message, @Nonnull ProjectStructureProblemType problemType,
                                    @Nullable List<PackagingElement<?>> pathToPlace, @Nonnull PlaceInArtifact place,
                                    final List<ConfigurationErrorQuickFix> quickFixList) {
    super(message, null, place, problemType, quickFixList);
    myPathToPlace = pathToPlace;
  }

  @Nullable
  public List<PackagingElement<?>> getPathToPlace() {
    return myPathToPlace;
  }
}
