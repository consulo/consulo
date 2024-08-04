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
package consulo.project.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.MagicConstant;

/**
 * @author VISTALL
 * @since 2024-08-04
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ProjectOpenSetting {
    @Nonnull
    @Deprecated
    static ProjectOpenSetting getInstance() {
        return Application.get().getInstance(ProjectOpenSetting.class);
    }

    int OPEN_PROJECT_ASK = -1;
    int OPEN_PROJECT_NEW_WINDOW = 0;
    int OPEN_PROJECT_SAME_WINDOW = 1;

    @MagicConstant(intValues = {OPEN_PROJECT_ASK, OPEN_PROJECT_NEW_WINDOW, OPEN_PROJECT_SAME_WINDOW})
    @interface OpenNewProjectOption {
    }

    /**
     * @return <ul>
     * <li>{@link #OPEN_PROJECT_NEW_WINDOW} if new project should be opened in new window
     * <li>{@link #OPEN_PROJECT_SAME_WINDOW} if new project should be opened in same window
     * <li>{@link #OPEN_PROJECT_ASK} if a confirmation dialog should be shown
     * </ul>
     */
    @OpenNewProjectOption
    int getConfirmOpenNewProject();

    void setConfirmOpenNewProject(@OpenNewProjectOption int confirmOpenNewProject);
}
