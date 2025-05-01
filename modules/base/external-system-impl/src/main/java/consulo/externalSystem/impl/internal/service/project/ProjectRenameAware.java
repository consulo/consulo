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
package consulo.externalSystem.impl.internal.service.project;

import consulo.externalSystem.ExternalSystemManager;
import consulo.externalSystem.impl.internal.service.ExternalSystemFacadeManager;
import consulo.externalSystem.setting.AbstractExternalSystemSettings;
import consulo.externalSystem.setting.ExternalSystemSettingsListenerAdapter;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * We need to avoid memory leaks on ide project rename. This class is responsible for that.
 *
 * @author Denis Zhdanov
 * @since 2013-07-19
 */
public class ProjectRenameAware {
    public static void beAware(@Nonnull Project project) {
        ExternalSystemFacadeManager facadeManager = project.getApplication().getInstance(ExternalSystemFacadeManager.class);
        project.getApplication().getExtensionPoint(ExternalSystemManager.class).forEach(manager -> {
            AbstractExternalSystemSettings<?, ?, ?> settings =
                ((ExternalSystemManager<?, ?, ?, ?, ?>)manager).getSettingsProvider().apply(project);
            //noinspection unchecked
            settings.subscribe(new ExternalSystemSettingsListenerAdapter() {
                @Override
                public void onProjectRenamed(@Nonnull String oldName, @Nonnull String newName) {
                    facadeManager.onProjectRename(oldName, newName);
                }
            });
        });
    }
}
