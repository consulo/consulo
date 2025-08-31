/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.versionControlSystem.internal;

import consulo.project.Project;
import consulo.ui.ex.content.ContentManager;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsInitObject;
import jakarta.annotation.Nonnull;

import java.util.List;

public abstract class ProjectLevelVcsManagerEx extends ProjectLevelVcsManager {
    public static ProjectLevelVcsManagerEx getInstanceEx(Project project) {
        return (ProjectLevelVcsManagerEx) project.getComponent(ProjectLevelVcsManager.class);
    }

    public abstract ContentManager getContentManager();

    @Nonnull
    public abstract VcsShowConfirmationOptionImpl getConfirmation(VcsConfiguration.StandardConfirmation option);

    public abstract List<VcsShowOptionsSettingImpl> getAllOptions();

    public abstract List<VcsShowConfirmationOptionImpl> getAllConfirmations();

    public abstract void notifyDirectoryMappingChanged();

    public abstract void fireDirectoryMappingsChanged();

    public abstract String haveDefaultMapping();

    public abstract void addInitializationRequest(VcsInitObject vcsInitObject, Runnable runnable);

    public boolean isBackgroundTaskRunning(@Nonnull Object... keys) {
        return false;
    }

    public void startBackgroundTask(@Nonnull Object... keys) {
    }

    public void stopBackgroundTask(@Nonnull Object... keys) {
    }
}
