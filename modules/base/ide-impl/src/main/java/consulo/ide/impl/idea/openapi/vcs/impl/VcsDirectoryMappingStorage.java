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

package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.inject.Inject;

/**
 * @author yole
 */
@State(name = "VcsDirectoryMappings", storages = @Storage("vcs.xml"))
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class VcsDirectoryMappingStorage implements PersistentStateComponent<Element> {
  private final ProjectLevelVcsManager myVcsManager;

  @Inject
  public VcsDirectoryMappingStorage(ProjectLevelVcsManager vcsManager) {
    myVcsManager = vcsManager;
  }

  @Override
  public Element getState() {
    Element e = new Element("state");
    ((ProjectLevelVcsManagerImpl)myVcsManager).writeDirectoryMappings(e);
    return e;
  }

  @Override
  public void loadState(Element state) {
    ((ProjectLevelVcsManagerImpl)myVcsManager).readDirectoryMappings(state);
  }
}
