/*
 * Copyright 2013-2026 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * Supplies the user interface over the settings of one external system, and the settings values it edits.
 * <p/>
 * Both the settings page and the import wizard resolve this by {@link #getSystemId() system id}, so an external system describes its
 * settings user interface once, apart from the logic of importing and resolving its projects.
 *
 * @author VISTALL
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExternalSystemSettingsConfigurableFactory<
    ProjectSettings extends ExternalProjectSettings,
    SystemSettings extends AbstractExternalSystemSettings<SystemSettings, ProjectSettings, ?>
> {
    /**
     * @return id of the external system whose settings are described by this factory
     */
    ProjectSystemId getSystemId();

    /**
     * @return a new project settings value, with whatever defaults the external system wants a fresh import to start from
     */
    ProjectSettings createProjectSettings();

    /**
     * @return a new system settings value, detached from the one stored in any ide project
     */
    SystemSettings createSystemSettings();

    /**
     * Creates a configurable over the given project settings. The configurable writes into the given object.
     *
     * @param settings project settings to edit
     * @param place    where the settings are being shown
     */
    @RequiredUIAccess
    AbstractExternalProjectSettingsConfigurable<ProjectSettings> createProjectSettingsConfigurable(
        ProjectSettings settings,
        ExternalSystemSettingsPlace place
    );

    /**
     * Creates a configurable over the given system-level settings, if the external system has any.
     *
     * @param settings system settings to edit
     * @param place    where the settings are being shown
     * @return {@code null} when the external system only has project-level settings
     */
    @RequiredUIAccess
    default @Nullable ExternalSystemSettingsConfigurable<SystemSettings> createSystemSettingsConfigurable(
        SystemSettings settings,
        ExternalSystemSettingsPlace place
    ) {
        return null;
    }
}
