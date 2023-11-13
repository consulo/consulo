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

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactModel;
import consulo.compiler.artifact.ArtifactType;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author nik
 */
public abstract class ArtifactModelBase implements ArtifactModel {
  private Map<String, Artifact> myArtifactsMap;
  private Artifact[] myArtifactsArray;
  public static final Predicate<Artifact> VALID_ARTIFACT_CONDITION = artifact -> !(artifact instanceof InvalidArtifact);

  protected abstract List<? extends Artifact> getArtifactsList();

  @Nonnull
  public Artifact[] getArtifacts() {
    if (myArtifactsArray == null) {
      final List<? extends Artifact> validArtifacts = ContainerUtil.findAll(getArtifactsList(), VALID_ARTIFACT_CONDITION);
      myArtifactsArray = validArtifacts.toArray(new Artifact[validArtifacts.size()]);
    }
    return myArtifactsArray;
  }

  @Override
  public List<? extends Artifact> getAllArtifactsIncludingInvalid() {
    return Collections.unmodifiableList(getArtifactsList());
  }

  public Artifact findArtifact(@Nonnull String name) {
    if (myArtifactsMap == null) {
      myArtifactsMap = new HashMap<String, Artifact>();
      for (Artifact artifact : getArtifactsList()) {
        myArtifactsMap.put(artifact.getName(), artifact);
      }
    }
    return myArtifactsMap.get(name);
  }

  @Nonnull
  public Artifact getArtifactByOriginal(@Nonnull Artifact artifact) {
    return artifact;
  }

  @Nonnull
  public Artifact getOriginalArtifact(@Nonnull Artifact artifact) {
    return artifact;
  }

  @Nonnull
  public Collection<? extends Artifact> getArtifactsByType(@Nonnull ArtifactType type) {
    final List<Artifact> result = new ArrayList<Artifact>();
    for (Artifact artifact : getArtifacts()) {
      if (artifact.getArtifactType().equals(type)) {
        result.add(artifact);
      }
    }
    return result;
  }

  protected void artifactsChanged() {
    myArtifactsMap = null;
    myArtifactsArray = null;
  }
}
