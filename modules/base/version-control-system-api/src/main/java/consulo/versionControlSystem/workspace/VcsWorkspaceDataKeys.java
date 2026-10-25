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

import consulo.util.dataholder.Key;

import java.util.List;

/**
 * The data the workspace view provides to its actions.
 */
public final class VcsWorkspaceDataKeys {
    public static final Key<VcsWorkspaceProvider> PROVIDER = Key.create("VcsWorkspaceDataKeys.PROVIDER");
    public static final Key<List<VcsWorkspace>> SELECTED_WORKSPACES = Key.create("VcsWorkspaceDataKeys.SELECTED_WORKSPACES");
    /**
     * The repository of the selection, or the only repository when nothing is selected; absent when the selection spans repositories.
     */
    public static final Key<VcsWorkspaceGroup> SELECTED_GROUP = Key.create("VcsWorkspaceDataKeys.SELECTED_GROUP");

    private VcsWorkspaceDataKeys() {
    }
}
