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

import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementFactory;
import com.intellij.util.EventDispatcher;
import consulo.annotation.access.RequiredWriteAction;
import consulo.packaging.artifacts.ArtifactPointerUtil;
import consulo.packaging.impl.artifacts.ArtifactPointerManagerImpl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
public class ArtifactModelImpl extends ArtifactModelBase implements ModifiableArtifactModel {
  private final List<ArtifactImpl> myOriginalArtifacts;
  private final ArtifactManagerImpl myArtifactManager;
  private final Map<ArtifactImpl, ArtifactImpl> myArtifact2ModifiableCopy = new HashMap<>();
  private final Map<ArtifactImpl, ArtifactImpl> myModifiable2Original = new HashMap<>();
  private final EventDispatcher<ArtifactListener> myDispatcher = EventDispatcher.create(ArtifactListener.class);

  public ArtifactModelImpl(ArtifactManagerImpl artifactManager, List<ArtifactImpl> originalArtifacts) {
    myArtifactManager = artifactManager;
    myOriginalArtifacts = new ArrayList<>(originalArtifacts);
    addListener(new ArtifactAdapter() {
      @Override
      public void artifactChanged(@Nonnull Artifact artifact, @Nonnull String oldName) {
        artifactsChanged();
      }
    });
  }

  @Override
  protected List<? extends Artifact> getArtifactsList() {
    final List<ArtifactImpl> list = new ArrayList<>();
    for (ArtifactImpl artifact : myOriginalArtifacts) {
      final ArtifactImpl copy = myArtifact2ModifiableCopy.get(artifact);
      if (copy != null) {
        list.add(copy);
      }
      else {
        list.add(artifact);
      }
    }
    return list;
  }

  @Override
  @Nonnull
  public ModifiableArtifact addArtifact(@Nonnull final String name, @Nonnull ArtifactType artifactType) {
    return addArtifact(name, artifactType, artifactType.createRootElement(PackagingElementFactory.getInstance(myArtifactManager.getProject()), name));
  }

  @Override
  @Nonnull
  public ModifiableArtifact addArtifact(@Nonnull String name, @Nonnull ArtifactType artifactType, CompositePackagingElement<?> rootElement) {
    final String uniqueName = generateUniqueName(name);
    final String outputPath = ArtifactUtil.getDefaultArtifactOutputPath(uniqueName, myArtifactManager.getProject());
    final ArtifactImpl artifact = new ArtifactImpl(uniqueName, artifactType, false, rootElement, outputPath, myDispatcher);
    myOriginalArtifacts.add(artifact);
    myArtifact2ModifiableCopy.put(artifact, artifact);
    myModifiable2Original.put(artifact, artifact);

    artifactsChanged();
    myDispatcher.getMulticaster().artifactAdded(artifact);
    return artifact;
  }

  private String generateUniqueName(String baseName) {
    String name = baseName;
    int i = 2;
    while (true) {
      if (findArtifact(name) == null) {
        return name;
      }
      name = baseName + i++;
    }
  }

  @Override
  public void addListener(@Nonnull ArtifactListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void removeListener(@Nonnull ArtifactListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void removeArtifact(@Nonnull Artifact artifact) {
    final ArtifactImpl artifactImpl = (ArtifactImpl)artifact;
    ArtifactImpl original = myModifiable2Original.remove(artifactImpl);
    if (original != null) {
      myOriginalArtifacts.remove(original);
    }
    else {
      original = artifactImpl;
    }
    myArtifact2ModifiableCopy.remove(original);
    myOriginalArtifacts.remove(original);
    artifactsChanged();
    myDispatcher.getMulticaster().artifactRemoved(original);
  }

  @Override
  @Nonnull
  public ModifiableArtifact getOrCreateModifiableArtifact(@Nonnull Artifact artifact) {
    final ArtifactImpl artifactImpl = (ArtifactImpl)artifact;
    if (myModifiable2Original.containsKey(artifactImpl)) {
      return artifactImpl;
    }

    ArtifactImpl modifiableCopy = myArtifact2ModifiableCopy.get(artifactImpl);
    if (modifiableCopy == null) {
      modifiableCopy = artifactImpl.createCopy(myDispatcher);
      myDispatcher.getMulticaster().artifactChanged(modifiableCopy, artifact.getName());
      myArtifact2ModifiableCopy.put(artifactImpl, modifiableCopy);
      myModifiable2Original.put(modifiableCopy, artifactImpl);
      artifactsChanged();
    }
    return modifiableCopy;
  }

  @Override
  @Nonnull
  public Artifact getOriginalArtifact(@Nonnull Artifact artifact) {
    final ArtifactImpl original = myModifiable2Original.get(artifact);
    return original != null ? original : artifact;
  }

  @Override
  @Nonnull
  public ArtifactImpl getArtifactByOriginal(@Nonnull Artifact artifact) {
    final ArtifactImpl artifactImpl = (ArtifactImpl)artifact;
    final ArtifactImpl copy = myArtifact2ModifiableCopy.get(artifactImpl);
    return copy != null ? copy : artifactImpl;
  }

  @Override
  public boolean isModified() {
    return !myOriginalArtifacts.equals(myArtifactManager.getArtifactsList()) || !myArtifact2ModifiableCopy.isEmpty();
  }

  @Override
  @RequiredWriteAction
  public void commit() {
    myArtifactManager.commit(this);
  }

  @Override
  public void dispose() {
    List<Artifact> artifacts = new ArrayList<>();
    for (ArtifactImpl artifact : myModifiable2Original.keySet()) {
      if (myModifiable2Original.get(artifact).equals(artifact)) {
        artifacts.add(artifact);
      }
    }
    ((ArtifactPointerManagerImpl)ArtifactPointerUtil.getPointerManager(myArtifactManager.getProject())).unregisterPointers(artifacts);
  }

  @Override
  @Nullable
  public ArtifactImpl getModifiableCopy(Artifact artifact) {
    //noinspection SuspiciousMethodCalls
    return myArtifact2ModifiableCopy.get(artifact);
  }

  public List<ArtifactImpl> getOriginalArtifacts() {
    return myOriginalArtifacts;
  }
}
