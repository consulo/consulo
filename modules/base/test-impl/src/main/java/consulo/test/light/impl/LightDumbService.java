/*
 * Copyright 2013-2023 consulo.io
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
package consulo.test.light.impl;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AccessToken;
import consulo.component.util.ModificationTracker;
import consulo.localize.LocalizeValue;
import consulo.project.DumbModeTask;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ModalityState;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2023-11-08
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.LIGHT_TEST)
public class LightDumbService extends DumbService implements ModificationTracker {
    private long modificationTracker;

    private final Project myProject;

    @Inject
    public LightDumbService(Project project) {
        myProject = project;
    }

    @Override
    public ModificationTracker getModificationTracker() {
        return this;
    }

    @Override
    public boolean isDumb() {
        return false;
    }

    @Override
    public void runWhenSmart(@Nonnull Runnable runnable) {
    }

    @Override
    public void waitForSmartMode() {
    }

    @Override
    public void smartInvokeLater(@Nonnull Runnable runnable) {
    }

    @Override
    public void smartInvokeLater(@Nonnull Runnable runnable, @Nonnull ModalityState modalityState) {
    }

    @Override
    public void queueTask(@Nonnull DumbModeTask task) {
    }

    @Override
    public void cancelTask(@Nonnull DumbModeTask task) {
    }

    @Override
    public void completeJustSubmittedTasks() {
    }

    @Override
    public void showDumbModeNotification(@Nonnull LocalizeValue message) {
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public void setAlternativeResolveEnabled(boolean enabled) {
    }

    @Override
    public boolean isAlternativeResolveEnabled() {
        return false;
    }

    @Nonnull
    @Override
    public AccessToken startHeavyActivityStarted(@Nonnull LocalizeValue activityName) {
        return AccessToken.EMPTY_ACCESS_TOKEN;
    }

    @Override
    public boolean isSuspendedDumbMode() {
        return false;
    }

    @Override
    public long getModificationCount() {
        return modificationTracker;
    }
}
