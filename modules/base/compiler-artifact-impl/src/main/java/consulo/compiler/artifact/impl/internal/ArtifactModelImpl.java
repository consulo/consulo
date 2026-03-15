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
package consulo.compiler.artifact.impl.internal;

import consulo.annotation.access.RequiredWriteAction;
import consulo.compiler.artifact.*;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.event.ArtifactListener;
import consulo.proxy.EventDispatcher;
import org.jspecify.annotations.Nullable;

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
    addListener(new ArtifactListener() {
      @Override
      public void artifactChanged(Artifact artifact, String oldName) {
        artifactsChanged();
      }
    });
  }

  @Override
  protected List<? extends Artifact> getArtifactsList() {
    List<ArtifactImpl> list = new ArrayList<>();
    for (ArtifactImpl artifact : myOriginalArtifacts) {
      ArtifactImpl copy = myArtifact2ModifiableCopy.get(artifact);
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
  
  public ModifiableArtifact addArtifact(String name, ArtifactType artifactType) {
    return addArtifact(name,
                       artifactType,
                       artifactType.createRootElement(PackagingElementFactory.getInstance(myArtifactManager.getProject()), name));
  }

  @Override
  
  public ModifiableArtifact addArtifact(String name,
                                        ArtifactType artifactType,
                                        CompositePackagingElement<?> rootElement) {
    String uniqueName = generateUniqueName(name);
    String outputPath = ArtifactUtil.getDefaultArtifactOutputPath(uniqueName, myArtifactManager.getProject());
    ArtifactImpl artifact = new ArtifactImpl(uniqueName, artifactType, false, rootElement, outputPath, myDispatcher);
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
  public void addListener(ArtifactListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void removeListener(ArtifactListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public void removeArtifact(Artifact artifact) {
    ArtifactImpl artifactImpl = (ArtifactImpl)artifact;
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
  
  public ModifiableArtifact getOrCreateModifiableArtifact(Artifact artifact) {
    ArtifactImpl artifactImpl = (ArtifactImpl)artifact;
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
  
  public Artifact getOriginalArtifact(Artifact artifact) {
    ArtifactImpl original = myModifiable2Original.get(artifact);
    return original != null ? original : artifact;
  }

  @Override
  
  public ArtifactImpl getArtifactByOriginal(Artifact artifact) {
    ArtifactImpl artifactImpl = (ArtifactImpl)artifact;
    ArtifactImpl copy = myArtifact2ModifiableCopy.get(artifactImpl);
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
