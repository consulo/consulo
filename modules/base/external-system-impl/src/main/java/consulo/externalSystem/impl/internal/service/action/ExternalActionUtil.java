/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.externalSystem.impl.internal.service.action;

import consulo.dataContext.DataContext;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.model.project.ExternalProjectPojo;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.setting.AbstractExternalSystemLocalSettings;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.view.ProjectNode;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 * @since 9/18/13
 */
public class ExternalActionUtil {
  
  public static MyInfo getProcessingInfo(DataContext context) {
    ExternalProjectPojo externalProject = context.getData(ExternalSystemDataKeys.SELECTED_PROJECT);
    if (externalProject == null) {
      // Fall back to the new ExternalProjectsView-based selection (ProjectNode).
      // DetachExternalProjectAction / RefreshExternalProjectAction use this util but the new view
      // only populates SELECTED_PROJECT_NODE, not the legacy SELECTED_PROJECT key.
      ProjectNode projectNode = context.getData(ExternalSystemDataKeys.SELECTED_PROJECT_NODE);
      if (projectNode != null) {
        ProjectData projectData = projectNode.getData();
        if (projectData != null) {
          externalProject = new ExternalProjectPojo(
            projectData.getInternalName(),
            projectData.getLinkedExternalProjectPath()
          );
        }
      }
    }
    if (externalProject == null) {
      return MyInfo.EMPTY;
    }

    ProjectSystemId externalSystemId = context.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    if (externalSystemId == null) {
      return MyInfo.EMPTY;
    }

    Project ideProject = context.getData(Project.KEY);
    if (ideProject == null) {
      return MyInfo.EMPTY;
    }

    AbstractExternalSystemSettings<?, ?, ?> settings = ExternalSystemApiUtil.getSettings(ideProject, externalSystemId);
    ExternalProjectSettings externalProjectSettings = settings.getLinkedProjectSettings(externalProject.getPath());
    AbstractExternalSystemLocalSettings localSettings = ExternalSystemApiUtil.getLocalSettings(ideProject, externalSystemId);

    return new MyInfo(
      externalProjectSettings == null ? null : settings,
      localSettings == null ? null : localSettings,
      externalProjectSettings == null ? null : externalProject,
      ideProject,
      externalSystemId
    );
  }

  public static class MyInfo {

    public static final MyInfo EMPTY = new MyInfo(null, null, null, null, null);

    public final @Nullable AbstractExternalSystemSettings<?, ?, ?> settings;
    public final @Nullable AbstractExternalSystemLocalSettings localSettings;
    public final @Nullable ExternalProjectPojo externalProject;
    public final @Nullable Project ideProject;
    public final @Nullable ProjectSystemId externalSystemId;

    MyInfo(
      @Nullable AbstractExternalSystemSettings<?, ?, ?> settings,
      @Nullable AbstractExternalSystemLocalSettings localSettings,
      @Nullable ExternalProjectPojo externalProject,
      @Nullable Project ideProject,
      @Nullable ProjectSystemId externalSystemId
    ) {
      this.settings = settings;
      this.localSettings = localSettings;
      this.externalProject = externalProject;
      this.ideProject = ideProject;
      this.externalSystemId = externalSystemId;
    }
  }
}
