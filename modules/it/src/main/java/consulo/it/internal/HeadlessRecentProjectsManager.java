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
import consulo.project.Project;
import consulo.project.ProjectGroup;
import consulo.project.internal.RecentProjectsManager;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Mock {@code RecentProjectsManager} for the integration-test harness. The production impl
 * ({@code RecentProjectsManagerImpl}) lives in {@code ide-impl}; here recent-project state is kept
 * only in memory. Bound only under the {@link ComponentProfiles#INTEGRATION_TEST} profile.
 * <p>
 * Note: {@link #setLastProjectCreationLocation} is called from the terminal step of the project-open
 * flow — without a binding, that call throws inside a {@code CompletableFuture.whenComplete}, which
 * silently drops the completion and hangs the open future.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessRecentProjectsManager implements RecentProjectsManager {
    private volatile @Nullable String myLastProjectCreationLocation;
    private volatile @Nullable String myLastProjectPath;
    private volatile int myRecentProjectsLimit = DEFAULT_RECENT_PROJECTS_LIMIT;

    @Override
    public @Nullable String getLastProjectCreationLocation() {
        return myLastProjectCreationLocation;
    }

    @Override
    public void setLastProjectCreationLocation(@Nullable String lastProjectLocation) {
        myLastProjectCreationLocation = lastProjectLocation;
    }

    @Override
    public void updateProjectModuleExtensions(Project project) {
    }

    @Override
    public void updateLastProjectPath() {
    }

    @Override
    public String getLastProjectPath() {
        return myLastProjectPath;
    }

    @Override
    public void removePath(@Nullable String path) {
    }

    @Override
    public List<ProjectGroup> getGroups() {
        return List.of();
    }

    @Override
    public void addGroup(ProjectGroup group) {
    }

    @Override
    public void removeGroup(ProjectGroup group) {
    }

    @Override
    public boolean hasRecentPaths() {
        return false;
    }

    @Override
    public int getRecentProjectsLimit() {
        return myRecentProjectsLimit;
    }

    @Override
    public void setRecentProjectsLimit(int limit) {
        myRecentProjectsLimit = limit;
    }
}
