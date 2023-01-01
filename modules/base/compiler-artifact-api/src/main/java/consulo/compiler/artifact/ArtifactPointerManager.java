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
package consulo.compiler.artifact;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.util.pointer.NamedPointerManager;
import consulo.project.Project;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface ArtifactPointerManager extends NamedPointerManager<Artifact> {
  @Nonnull
  public static ArtifactPointerManager getInstance(Project project) {
    return project.getInstance(ArtifactPointerManager.class);
  }

  @Nonnull
  @Override
  ArtifactPointer create(@Nonnull String name);

  @Nonnull
  @Override
  ArtifactPointer create(@Nonnull Artifact value);

  @Nonnull
  ArtifactPointer create(@Nonnull Artifact artifact, @Nonnull ArtifactModel artifactModel);
}
