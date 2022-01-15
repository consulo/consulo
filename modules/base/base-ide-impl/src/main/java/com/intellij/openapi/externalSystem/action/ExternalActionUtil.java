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
package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemLocalSettings;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Vladislav.Soroka
 * @since 9/18/13
 */
public class ExternalActionUtil {
  @Nonnull
  public static MyInfo getProcessingInfo(@Nonnull DataContext context) {
    ExternalProjectPojo externalProject = context.getData(ExternalSystemDataKeys.SELECTED_PROJECT);
    if (externalProject == null) {
      return MyInfo.EMPTY;
    }

    ProjectSystemId externalSystemId = context.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    if (externalSystemId == null) {
      return MyInfo.EMPTY;
    }

    Project ideProject = context.getData(CommonDataKeys.PROJECT);
    if (ideProject == null) {
      return MyInfo.EMPTY;
    }

    AbstractExternalSystemSettings<?, ?, ?> settings = ExternalSystemApiUtil.getSettings(ideProject, externalSystemId);
    ExternalProjectSettings externalProjectSettings = settings.getLinkedProjectSettings(externalProject.getPath());
    AbstractExternalSystemLocalSettings localSettings = ExternalSystemApiUtil.getLocalSettings(ideProject, externalSystemId);

    return new MyInfo(externalProjectSettings == null ? null : settings,
                      localSettings == null ? null : localSettings,
                      externalProjectSettings == null ? null : externalProject,
                      ideProject,
                      externalSystemId);
  }

  public static class MyInfo {

    public static final MyInfo EMPTY = new MyInfo(null, null, null, null, null);

    @javax.annotation.Nullable
    public final AbstractExternalSystemSettings<?, ?, ?> settings;
    @Nullable public final AbstractExternalSystemLocalSettings  localSettings;
    @javax.annotation.Nullable
    public final ExternalProjectPojo                  externalProject;
    @Nullable public final Project                              ideProject;
    @javax.annotation.Nullable
    public final ProjectSystemId                      externalSystemId;

    MyInfo(@Nullable AbstractExternalSystemSettings<?, ?, ?> settings,
           @Nullable AbstractExternalSystemLocalSettings localSettings,
           @javax.annotation.Nullable ExternalProjectPojo externalProject,
           @javax.annotation.Nullable Project ideProject,
           @Nullable ProjectSystemId externalSystemId)
    {
      this.settings = settings;
      this.localSettings = localSettings;
      this.externalProject = externalProject;
      this.ideProject = ideProject;
      this.externalSystemId = externalSystemId;
    }
  }
}

