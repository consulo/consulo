/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import consulo.project.Project;
import consulo.project.ProjectGroup;
import consulo.ui.ex.action.AnAction;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.MagicConstant;

import java.util.Collections;
import java.util.List;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class RecentProjectsManager {
    public static final int RECENT_ACTIONS_LIMIT_ACTION_ITEMS = 1 << 1;
    public static final int RECENT_ACTIONS_USE_GROUPS_WELCOME_MENU = 1 << 2;
    public static final int RECENT_ACTIONS_USE_GROUPS_CONTEXT_MENU = 1 << 3;

    @Nonnull
    public static RecentProjectsManager getInstance() {
        return Application.get().getInstance(RecentProjectsManager.class);
    }

    @Nullable
    public abstract String getLastProjectCreationLocation();

    public abstract void setLastProjectCreationLocation(@Nullable String lastProjectLocation);

    public abstract void updateProjectModuleExtensions(@Nonnull Project project);

    public abstract void updateLastProjectPath();

    public abstract String getLastProjectPath();

    public abstract void removePath(@Nullable String path);

    @Nonnull
    @Deprecated
    public final AnAction[] getRecentProjectsActions(boolean forMenu) {
        int flags = 0;
        flags = BitUtil.set(flags, RECENT_ACTIONS_LIMIT_ACTION_ITEMS, forMenu);
        return getRecentProjectsActions(flags);
    }

    @Nonnull
    @Deprecated
    public final AnAction[] getRecentProjectsActions(boolean forMenu, boolean useGroups) {
        int flags = 0;
        flags = BitUtil.set(flags, RECENT_ACTIONS_LIMIT_ACTION_ITEMS, forMenu);
        flags = BitUtil.set(flags, RECENT_ACTIONS_USE_GROUPS_WELCOME_MENU, useGroups);
        return getRecentProjectsActions(flags);
    }

    @Nonnull
    public AnAction[] getRecentProjectsActions(@MagicConstant(flags = {
        RECENT_ACTIONS_LIMIT_ACTION_ITEMS,
        RECENT_ACTIONS_USE_GROUPS_WELCOME_MENU,
        RECENT_ACTIONS_USE_GROUPS_CONTEXT_MENU
    }) int flags) {
        return AnAction.EMPTY_ARRAY;
    }

    public List<ProjectGroup> getGroups() {
        return Collections.emptyList();
    }

    public void addGroup(ProjectGroup group) {
    }

    public void removeGroup(ProjectGroup group) {
    }

    public boolean hasPath(String path) {
        return false;
    }
}