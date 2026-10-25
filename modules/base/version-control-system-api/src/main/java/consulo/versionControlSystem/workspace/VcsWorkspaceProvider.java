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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The source of one VCS's workspace view. The VCS names the feature ("Worktrees", "Workspaces", ...), supplies the
 * workspaces and its own actions; the platform shows them in a tab of the version control tool window, see
 * {@link VcsWorkspaceContentFactory}.
 */
public interface VcsWorkspaceProvider {
    /**
     * The id of the tab.
     */
    String getId();

    /**
     * The name of the feature, used as the tab name.
     */
    LocalizeValue getFeatureName();

    default int getOrderWeight() {
        return 70;
    }

    boolean isTabAvailable();

    /**
     * Called when the user closes the tab.
     */
    default void tabClosedByUser() {
    }

    /**
     * The current workspaces, grouped by repository. Called on a background thread, must only read in-memory state.
     */
    List<? extends VcsWorkspaceGroup> getGroups();

    /**
     * {@code listener} is called, on any thread, when {@link #getGroups()} may return something else.
     */
    void addChangeListener(Runnable listener, Disposable parentDisposable);

    /**
     * The place of the view's toolbar and context menu actions.
     */
    String getActionPlace();

    ActionGroup getToolbarActions();

    ActionGroup getPopupActions();

    /**
     * Opens the workspace on a double click.
     */
    @RequiredUIAccess
    void open(VcsWorkspace workspace);

    LocalizeValue getEmptyText();

    default List<VcsWorkspaceLink> getEmptyLinks() {
        return List.of();
    }

    default @Nullable String getHelpId() {
        return null;
    }
}
