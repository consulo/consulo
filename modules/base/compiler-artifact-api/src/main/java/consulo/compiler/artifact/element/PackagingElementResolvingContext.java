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

import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactModel;
import consulo.compiler.artifact.internal.DefaultPackagingElementResolvingContext;
import consulo.content.library.Library;
import consulo.module.content.layer.ModulesProvider;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public interface PackagingElementResolvingContext {
  @Nonnull
  static PackagingElementResolvingContext of(Project project, ArtifactManager artifactManager) {
    return new DefaultPackagingElementResolvingContext(project, artifactManager);
  }

  @Nonnull
  Project getProject();

  @Nonnull
  ArtifactModel getArtifactModel();

  @Nonnull
  ModulesProvider getModulesProvider();

  @Nullable
  Library findLibrary(@Nonnull String level, @Nonnull String libraryName);
}
