/*
 * Copyright 2013-2017 consulo.io
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
package consulo.externalSystem.importing;

import consulo.configurable.ConfigurationException;
import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.setting.ExternalSystemSettingsConfigurableFactory;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.module.creation.importing.ModuleImportContext;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * Holds everything which belongs to a single import: the settings being edited and the resolved external project. The import
 * provider is an extension shared by every import, so none of this may live there.
 *
 * @author VISTALL
 * @since 2017-01-30
 */
public class ExternalModuleImportContext extends ModuleImportContext {
    private final AbstractExternalModuleImportProvider myImportProvider;

    private final ExternalSystemSettingsConfigurableFactory<?, ?> myConfigurableFactory;

    private final ExternalProjectSettings myProjectSettings;

    private final AbstractExternalSystemSettings<?, ?, ?> mySystemSettings;

    private @Nullable DataNode<ProjectData> myExternalProjectNode;

    public ExternalModuleImportContext(@Nullable Project project, AbstractExternalModuleImportProvider importProvider) {
        super(project);
        myImportProvider = importProvider;
        myConfigurableFactory = ExternalSystemApiUtil.getSettingsConfigurableFactoryStrict(importProvider.getExternalSystemId());
        myProjectSettings = myConfigurableFactory.createProjectSettings();
        mySystemSettings = myConfigurableFactory.createSystemSettings();
    }

    public AbstractExternalModuleImportProvider getImportProvider() {
        return myImportProvider;
    }

    public ExternalSystemSettingsConfigurableFactory<?, ?> getConfigurableFactory() {
        return myConfigurableFactory;
    }

    public ExternalProjectSettings getProjectSettings() {
        return myProjectSettings;
    }

    public AbstractExternalSystemSettings<?, ?, ?> getSystemSettings() {
        return mySystemSettings;
    }

    public @Nullable DataNode<ProjectData> getExternalProjectNode() {
        return myExternalProjectNode;
    }

    public void setExternalProjectNode(@Nullable DataNode<ProjectData> externalProjectNode) {
        myExternalProjectNode = externalProjectNode;
    }

    /**
     * Validates the given path and stores it into {@link #getProjectSettings() the project settings}.
     *
     * @param linkedProjectPath path of the external project being imported
     */
    public void applyLinkedProjectPath(String linkedProjectPath) throws ConfigurationException {
        if (StringUtil.isEmpty(linkedProjectPath)) {
            throw new ConfigurationException(ExternalSystemLocalize.errorProjectUndefined());
        }

        Project project = getProject();
        if (project != null) {
            ExternalSystemManager<?, ?, ?, ?, ?> manager =
                ExternalSystemApiUtil.getManager(myImportProvider.getExternalSystemId());
            assert manager != null;
            AbstractExternalSystemSettings<?, ?, ?> settings = manager.getSettingsProvider().apply(project);
            if (settings.getLinkedProjectSettings(linkedProjectPath) != null) {
                throw new ConfigurationException(ExternalSystemLocalize.errorProjectAlreadyRegistered());
            }
        }

        myProjectSettings.setExternalProjectPath(ExternalSystemApiUtil.normalizePath(linkedProjectPath));
    }
}
