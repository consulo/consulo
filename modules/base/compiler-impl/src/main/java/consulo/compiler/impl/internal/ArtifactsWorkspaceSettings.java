/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Tag;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
@Singleton
@State(name = "ArtifactsWorkspaceSettings", storages = {@Storage(StoragePathMacros.WORKSPACE_FILE)})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class ArtifactsWorkspaceSettings implements PersistentStateComponent<ArtifactsWorkspaceSettings.ArtifactsWorkspaceSettingsState> {
  public static class ArtifactsWorkspaceSettingsState {
    @Tag("artifacts-to-build")
    @AbstractCollection(surroundWithTag = false, elementTag = "artifact", elementValueAttribute = "name")
    public List<String> myArtifactsToBuild = new ArrayList<>();
  }

  public static ArtifactsWorkspaceSettings getInstance(@Nonnull Project project) {
    return project.getInstance(ArtifactsWorkspaceSettings.class);
  }

  private final ArtifactManager myArtifactManager;
  private ArtifactsWorkspaceSettingsState myState = new ArtifactsWorkspaceSettingsState();

  @Inject
  public ArtifactsWorkspaceSettings(ArtifactManager artifactManager) {
    myArtifactManager = artifactManager;
  }

  public List<Artifact> getArtifactsToBuild() {
    List<Artifact> result = new ArrayList<>();
    for (String name : myState.myArtifactsToBuild) {
      ContainerUtil.addIfNotNull(result, myArtifactManager.findArtifact(name));
    }
    return result;
  }

  public void setArtifactsToBuild(@Nonnull Collection<? extends Artifact> artifacts) {
    myState.myArtifactsToBuild.clear();
    for (Artifact artifact : artifacts) {
      myState.myArtifactsToBuild.add(artifact.getName());
    }
    Collections.sort(myState.myArtifactsToBuild);
  }

  @Override
  public ArtifactsWorkspaceSettingsState getState() {
    return myState;
  }

  @Override
  public void loadState(ArtifactsWorkspaceSettingsState state) {
    myState = state;
  }
}
