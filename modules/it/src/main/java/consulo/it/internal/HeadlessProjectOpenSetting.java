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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.project.internal.ProjectOpenSetting;
import jakarta.inject.Singleton;

/**
 * Headless {@code ProjectOpenSetting}: the production impl lives in {@code ide-impl}. Always opens a
 * new project in a new window ({@link #OPEN_PROJECT_NEW_WINDOW}), never {@link #OPEN_PROJECT_ASK},
 * so opening a second project does not try to raise a confirmation alert. Bound only under the
 * {@link ComponentProfiles#INTEGRATION_TEST} profile.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessProjectOpenSetting implements ProjectOpenSetting {
    private volatile int myConfirmOpenNewProject = OPEN_PROJECT_NEW_WINDOW;

    @Override
    public int getConfirmOpenNewProject() {
        return myConfirmOpenNewProject;
    }

    @Override
    public void setConfirmOpenNewProject(int confirmOpenNewProject) {
        myConfirmOpenNewProject = confirmOpenNewProject;
    }
}
