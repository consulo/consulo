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
package consulo.versionControlSystem.workspace;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.versionControlSystem.FilePath;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * One local working copy of a repository: a Git worktree, a Jujutsu workspace, a Mercurial share and so on.
 * Implementations are value objects - a provider builds new instances on every refresh, so {@link #equals} must compare values.
 */
public interface VcsWorkspace {
    FilePath getPath();

    default String getName() {
        return getPath().getName();
    }

    /**
     * What the workspace has checked out: a branch, a bookmark, a change, or a "detached" text.
     */
    LocalizeValue getRevisionText();

    default @Nullable Image getRevisionIcon() {
        return PlatformIconGroup.vcsBranch();
    }

    /**
     * The workspace of the current project.
     */
    boolean isCurrent();

    /**
     * The original working copy the other workspaces were created from.
     */
    default boolean isMain() {
        return false;
    }

    /**
     * The workspace is still being created.
     */
    default boolean isPending() {
        return false;
    }

    /**
     * The workspace is known to the repository but cannot be used, e.g. its directory is gone.
     */
    default boolean isAvailable() {
        return true;
    }

    default List<VcsWorkspaceBadge> getNameBadges() {
        return List.of();
    }

    default List<VcsWorkspaceBadge> getRevisionBadges() {
        return List.of();
    }

    default LocalizeValue getTooltip() {
        return LocalizeValue.of(getPath().getPresentableUrl());
    }
}
