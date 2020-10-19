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
package consulo.packaging.impl.artifacts;

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.impl.artifacts.ArtifactPointerImpl;
import consulo.util.pointers.NamedPointerImpl;
import consulo.util.pointers.NamedPointerManagerImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author VISTALL
 */
@Singleton
public class ArtifactPointerManagerImpl extends NamedPointerManagerImpl<Artifact> implements ArtifactPointerManager {
  private final Provider<ArtifactManager> myArtifactManagerProvider;

  @Inject
  public ArtifactPointerManagerImpl(Project project, Provider<ArtifactManager> artifactManagerProvider) {
    myArtifactManagerProvider = artifactManagerProvider;
    project.getMessageBus().connect().subscribe(ArtifactManager.TOPIC, new ArtifactListener() {
      @Override
      public void artifactRemoved(@Nonnull Artifact artifact) {
        unregisterPointer(artifact);
      }

      @Override
      public void artifactAdded(@Nonnull Artifact artifact) {
        updatePointers(artifact);
      }

      @Override
      public void artifactChanged(@Nonnull Artifact artifact, @Nonnull String oldName) {
        updatePointers(artifact, oldName);
      }
    });
  }

  @Override
  public void unregisterPointers(List<? extends Artifact> value) {
    super.unregisterPointers(value);
  }

  @Nonnull
  @Override
  public ArtifactPointer create(@Nonnull Artifact value) {
    return (ArtifactPointer)super.create(value);
  }

  @Nonnull
  @Override
  public ArtifactPointer create(@Nonnull String name) {
    return (ArtifactPointer)super.create(name);
  }

  /**
   * Special method for serialization, while target artifact manager did not provide recursion.
   *
   * It will be called from com.intellij.packaging.impl.elements.ArtifactPackagingElement, while ArtifactManager#loadState()
   */
  @Nonnull
  public ArtifactPointer create(@Nonnull ArtifactManager artifactManager, @Nonnull String name) {
    return (ArtifactPointer)create(name, artifactManager::findArtifact);
  }

  @Nonnull
  @Override
  public ArtifactPointer create(@Nonnull Artifact artifact, @Nonnull ArtifactModel artifactModel) {
    return create(artifactModel.getOriginalArtifact(artifact));
  }

  @Nullable
  @Override
  protected Artifact findByName(@Nonnull String name) {
    return myArtifactManagerProvider.get().findArtifact(name);
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
