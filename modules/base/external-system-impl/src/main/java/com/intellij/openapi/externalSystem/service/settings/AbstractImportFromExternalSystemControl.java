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
package com.intellij.openapi.externalSystem.service.settings;

import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.settings.ExternalSystemSettingsListener;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.externalSystem.util.ExternalSystemSettingsControl;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A control which knows how to manage settings of external project being imported.
 *
 * @author Denis Zhdanov
 * @since 4/30/13 2:33 PM
 */
public abstract class AbstractImportFromExternalSystemControl<ProjectSettings extends ExternalProjectSettings, L extends ExternalSystemSettingsListener<ProjectSettings>, SystemSettings extends AbstractExternalSystemSettings<SystemSettings, ProjectSettings, L>> {
  @Nonnull
  private final SystemSettings mySystemSettings;
  @Nonnull
  private final ProjectSettings myProjectSettings;

  @Nonnull
  private final ExternalSystemSettingsControl<ProjectSettings> myProjectSettingsControl;
  @Nonnull
  private final ProjectSystemId myExternalSystemId;
  @Nullable
  private final ExternalSystemSettingsControl<SystemSettings> mySystemSettingsControl;

  protected AbstractImportFromExternalSystemControl(@Nonnull ProjectSystemId externalSystemId, @Nonnull SystemSettings systemSettings, @Nonnull ProjectSettings projectSettings) {
    myExternalSystemId = externalSystemId;
    mySystemSettings = systemSettings;
    myProjectSettings = projectSettings;
    myProjectSettingsControl = createProjectSettingsControl(projectSettings);
    mySystemSettingsControl = createSystemSettingsControl(systemSettings);
  }

  public abstract void onLinkedProjectPathChange(@Nonnull String path);

  /**
   * Creates a control for managing given project settings.
   *
   * @param settings target external project settings
   * @return control for managing given project settings
   */
  @Nonnull
  protected abstract ExternalSystemSettingsControl<ProjectSettings> createProjectSettingsControl(@Nonnull ProjectSettings settings);

  /**
   * Creates a control for managing given system-level settings (if any).
   *
   * @param settings target system settings
   * @return a control for managing given system-level settings;
   * <code>null</code> if current external system doesn't have system-level settings (only project-level settings)
   */
  @Nullable
  protected abstract ExternalSystemSettingsControl<SystemSettings> createSystemSettingsControl(@Nonnull SystemSettings settings);

  @Nonnull
  public ExternalSystemSettingsControl<ProjectSettings> getProjectSettingsControl() {
    return myProjectSettingsControl;
  }

  @Nullable
  public ExternalSystemSettingsControl<SystemSettings> getSystemSettingsControl() {
    return mySystemSettingsControl;
  }

  @Nonnull
  public SystemSettings getSystemSettings() {
    return mySystemSettings;
  }

  @Nonnull
  public ProjectSettings getProjectSettings() {
    return myProjectSettings;
  }

  public void apply(String linkedProjectPath, @Nullable Project currentProject) throws ConfigurationException {
    if (StringUtil.isEmpty(linkedProjectPath)) {
      throw new ConfigurationException(ExternalSystemBundle.message("error.project.undefined"));
    }
    else if (currentProject != null) {
      ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
      assert manager != null;
      AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().fun(currentProject);
      if (settings.getLinkedProjectSettings(linkedProjectPath) != null) {
        throw new ConfigurationException(ExternalSystemBundle.message("error.project.already.registered"));
      }
    }

    //noinspection ConstantConditions
    myProjectSettings.setExternalProjectPath(ExternalSystemApiUtil.normalizePath(linkedProjectPath));

    myProjectSettingsControl.validate(myProjectSettings);
    myProjectSettingsControl.apply(myProjectSettings);

    if (mySystemSettingsControl != null) {
      mySystemSettingsControl.validate(mySystemSettings);
      mySystemSettingsControl.apply(mySystemSettings);
    }
  }
}
