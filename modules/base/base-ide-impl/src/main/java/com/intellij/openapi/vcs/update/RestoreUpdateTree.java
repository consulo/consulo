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
package com.intellij.openapi.vcs.update;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectReloadState;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.changes.committed.CommittedChangesCache;
import com.intellij.openapi.vcs.ex.ProjectLevelVcsManagerEx;
import consulo.ui.UIAccess;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@State(name = "RestoreUpdateTree", storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED))
public class RestoreUpdateTree implements PersistentStateComponent<Element> {
  @Nonnull
  public static RestoreUpdateTree getInstance(Project project) {
    return ServiceManager.getService(project, RestoreUpdateTree.class);
  }

  public static class MyStartUpActivity implements StartupActivity.DumbAware {
    @Override
    public void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project) {
      RestoreUpdateTree restoreUpdateTree = RestoreUpdateTree.getInstance(project);
      UpdateInfo updateInfo = restoreUpdateTree.myUpdateInfo;

      if (updateInfo != null && !updateInfo.isEmpty() && ProjectReloadState.getInstance(project).isAfterAutomaticReload()) {
        ActionInfo actionInfo = updateInfo.getActionInfo();
        if (actionInfo != null) {
          ProjectLevelVcsManagerEx.getInstanceEx(project).showUpdateProjectInfo(updateInfo.getFileInformation(), VcsBundle.message("action.display.name.update"), actionInfo, false);

          CommittedChangesCache.getInstance(project).refreshIncomingChangesAsync();
        }
        restoreUpdateTree.myUpdateInfo = null;
      }
      else {
        restoreUpdateTree.myUpdateInfo = null;
      }
    }
  }

  private static final String UPDATE_INFO = "UpdateInfo";

  private final Project myProject;
  private UpdateInfo myUpdateInfo;

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
