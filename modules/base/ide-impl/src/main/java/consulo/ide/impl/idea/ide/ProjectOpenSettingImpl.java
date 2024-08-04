/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ServiceImpl;
import consulo.project.internal.ProjectOpenSetting;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2024-08-04
 */
@ServiceImpl
@Singleton
public class ProjectOpenSettingImpl implements ProjectOpenSetting {
    private final GeneralSettings myGeneralSettings;

    @Inject
    public ProjectOpenSettingImpl(GeneralSettings generalSettings) {
        myGeneralSettings = generalSettings;
    }

    @Override
    public int getConfirmOpenNewProject() {
        return myGeneralSettings.getConfirmOpenNewProject();
    }

    @Override
    public void setConfirmOpenNewProject(@OpenNewProjectOption int confirmOpenNewProject) {
        myGeneralSettings.setConfirmOpenNewProject(confirmOpenNewProject);
    }
}
