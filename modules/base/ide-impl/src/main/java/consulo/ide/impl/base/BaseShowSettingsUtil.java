/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.base;

import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.internal.ConfigurableExtensionPointUtil;
import consulo.configurable.internal.ConfigurableWrapper;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.localization.LocalizationKey;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2019-01-06
 */
public abstract class BaseShowSettingsUtil extends ShowSettingsUtil {
    public static Configurable SKIP_SELECTION_CONFIGURATION = new Configurable() {
        @RequiredUIAccess
        @Override
        public boolean isModified() {
            return false;
        }

        @Nonnull
        @Override
        public LocalizeValue getDisplayName() {
            return LocalizeValue.empty();
        }

        @RequiredUIAccess
        @Override
        public void apply() throws ConfigurationException {
        }
    };

    public static String createDimensionKey(@Nonnull Configurable configurable) {
        LocalizationKey localizeKey = configurable.getDisplayName().getKey().orElseThrow();
        return "#" + localizeKey.getLocalizationId() + "@" + localizeKey.getKey();
    }

    @Nonnull
    public static Configurable[] buildConfigurables(@Nullable Project project) {
        if (project == null) {
            project = ProjectManager.getInstance().getDefaultProject();
        }

        Project tempProject = project;

        List<ApplicationConfigurable> applicationConfigurables = tempProject.getApplication().getExtensionPoint(ApplicationConfigurable.class).getExtensionList();
        List<ProjectConfigurable> projectConfigurables = tempProject.getExtensionPoint(ProjectConfigurable.class).getExtensionList();


        List<Configurable> mergedConfigurables = ContainerUtil.concat(applicationConfigurables, projectConfigurables);
        List<Configurable> result = ConfigurableExtensionPointUtil.buildConfigurablesList(mergedConfigurables, configurable -> !tempProject.isDefault() || !ConfigurableWrapper.isNonDefaultProject(configurable));

        return ContainerUtil.toArray(result, Configurable.ARRAY_FACTORY);
    }
}