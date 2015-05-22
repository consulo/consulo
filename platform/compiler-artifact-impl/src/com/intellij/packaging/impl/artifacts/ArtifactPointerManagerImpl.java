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
package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.*;
import org.consulo.util.pointers.NamedPointerImpl;
import org.consulo.util.pointers.NamedPointerManagerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 */
public class ArtifactPointerManagerImpl extends NamedPointerManagerImpl<Artifact> implements ArtifactPointerManager {
  private final Project myProject;

  public ArtifactPointerManagerImpl(Project project) {
    myProject = project;
    project.getMessageBus().connect().subscribe(ArtifactManager.TOPIC, new ArtifactListener() {
      @Override
      public void artifactRemoved(@NotNull Artifact artifact) {
        unregisterPointer(artifact);
      }

      @Override
      public void artifactAdded(@NotNull Artifact artifact) {
        updatePointers(artifact);
      }

      @Override
      public void artifactChanged(@NotNull Artifact artifact, @NotNull String oldName) {
        updatePointers(artifact, oldName);
      }
    });
  }

  @Override
  public void unregisterPointers(List<? extends Artifact> value) {
    super.unregisterPointers(value);
  }

  @NotNull
  @Override
  public ArtifactPointer create(@NotNull Artifact value) {
    return (ArtifactPointer)super.create(value);
  }

  @NotNull
  @Override
  public ArtifactPointer create(@NotNull String name) {
    return (ArtifactPointer)super.create(name);
  }

  @NotNull
  @Override
  public ArtifactPointer create(@NotNull Artifact artifact, @NotNull ArtifactModel artifactModel) {
    return create(artifactModel.getOriginalArtifact(artifact));
  }

  @Nullable
  @Override
  public Artifact findByName(@NotNull String name) {
    return ArtifactManager.getInstance(myProject).findArtifact(name);
  }

  @Override
  public NamedPointerImpl<Artifact> createImpl(String name) {
    return new ArtifactPointerImpl(name);
  }

  @Override
  public NamedPointerImpl<Artifact> createImpl(Artifact artifact) {
    return new ArtifactPointerImpl(artifact);
  }
}
