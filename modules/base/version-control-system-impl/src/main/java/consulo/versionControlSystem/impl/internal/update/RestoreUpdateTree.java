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
package consulo.versionControlSystem.impl.internal.update;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.*;
import consulo.project.Project;
import consulo.versionControlSystem.update.ActionInfo;
import consulo.versionControlSystem.update.UpdatedFiles;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

@Singleton
@State(name = "RestoreUpdateTree", storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class RestoreUpdateTree implements PersistentStateComponent<Element> {
  @Nonnull
  public static RestoreUpdateTree getInstance(Project project) {
    return project.getInstance(RestoreUpdateTree.class);
  }

  private static final String UPDATE_INFO = "UpdateInfo";

  private final Project myProject;
  protected UpdateInfo myUpdateInfo;

  @Inject
  public RestoreUpdateTree(Project project) {
    myProject = project;
  }

  public void registerUpdateInformation(UpdatedFiles updatedFiles, ActionInfo actionInfo) {
    myUpdateInfo = new UpdateInfo(myProject, updatedFiles, actionInfo);
  }

  @Nullable
  @Override
  public Element getState() {
    if (myUpdateInfo != null) {
      Element child = new Element(UPDATE_INFO);
      myUpdateInfo.writeExternal(child);

      return new Element("state").addContent(child);
    }

    return null;
  }

  @Override
  public void loadState(Element state) {
    Element child = state.getChild(UPDATE_INFO);
    if (child != null) {
      UpdateInfo updateInfo = new UpdateInfo(myProject);
      updateInfo.readExternal(child);
      myUpdateInfo = updateInfo;
    }
  }
}
