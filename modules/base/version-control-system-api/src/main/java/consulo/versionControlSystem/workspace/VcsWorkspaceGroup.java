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

import consulo.ui.color.ColorValue;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The workspaces of one repository.
 */
public interface VcsWorkspaceGroup {
    VirtualFile getRoot();

    default String getName() {
        return getRoot().getName();
    }

    /**
     * The colour that marks this repository's rows when the view shows more than one repository.
     */
    default @Nullable ColorValue getColor() {
        return null;
    }

    List<? extends VcsWorkspace> getWorkspaces();
}
