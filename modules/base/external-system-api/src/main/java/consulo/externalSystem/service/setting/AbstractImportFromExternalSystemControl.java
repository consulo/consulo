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
package consulo.externalSystem.service.setting;

import consulo.configurable.ConfigurationException;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.execution.ExternalSystemSettingsControl;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.setting.ExternalSystemSettingsListener;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * A control which knows how to manage settings of external project being imported.
 *
 * @author Denis Zhdanov
 * @since 4/30/13 2:33 PM
 */
public abstract class AbstractImportFromExternalSystemControl<ProjectSettings extends ExternalProjectSettings, L extends ExternalSystemSettingsListener<ProjectSettings>, SystemSettings extends AbstractExternalSystemSettings<SystemSettings, ProjectSettings, L>> {
    
    private final SystemSettings mySystemSettings;
    
    private final ProjectSettings myProjectSettings;

    
    private final ExternalSystemSettingsControl<ProjectSettings> myProjectSettingsControl;
    
    private final ProjectSystemId myExternalSystemId;
    private final @Nullable ExternalSystemSettingsControl<SystemSettings> mySystemSettingsControl;

    protected AbstractImportFromExternalSystemControl(
        ProjectSystemId externalSystemId,
        SystemSettings systemSettings,
        ProjectSettings projectSettings
    ) {
        myExternalSystemId = externalSystemId;
        mySystemSettings = systemSettings;
        myProjectSettings = projectSettings;
        myProjectSettingsControl = createProjectSettingsControl(projectSettings);
        mySystemSettingsControl = createSystemSettingsControl(systemSettings);
    }

    public abstract void onLinkedProjectPathChange(String path);

    /**
     * Creates a control for managing given project settings.
     *
     * @param settings target external project settings
     * @return control for managing given project settings
     */
    protected abstract ExternalSystemSettingsControl<ProjectSettings> createProjectSettingsControl(ProjectSettings settings);

    /**
     * Creates a control for managing given system-level settings (if any).
     *
     * @param settings target system settings
     * @return a control for managing given system-level settings;
     * <code>null</code> if current external system doesn't have system-level settings (only project-level settings)
     */
    protected abstract @Nullable ExternalSystemSettingsControl<SystemSettings> createSystemSettingsControl(SystemSettings settings);

    
    public ExternalSystemSettingsControl<ProjectSettings> getProjectSettingsControl() {
        return myProjectSettingsControl;
    }

    public @Nullable ExternalSystemSettingsControl<SystemSettings> getSystemSettingsControl() {
        return mySystemSettingsControl;
    }

    
    public SystemSettings getSystemSettings() {
        return mySystemSettings;
    }

    
    public ProjectSettings getProjectSettings() {
        return myProjectSettings;
    }

    public void apply(String linkedProjectPath, @Nullable Project currentProject) throws ConfigurationException {
        if (StringUtil.isEmpty(linkedProjectPath)) {
            throw new ConfigurationException(ExternalSystemLocalize.errorProjectUndefined());
        }
        else if (currentProject != null) {
            ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
            assert manager != null;
            AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().apply(currentProject);
            if (settings.getLinkedProjectSettings(linkedProjectPath) != null) {
                throw new ConfigurationException(ExternalSystemLocalize.errorProjectAlreadyRegistered());
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
