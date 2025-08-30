/*
 * Copyright 2013-2025 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-08-30
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface RepositoryChangesBrowserFactory {
    default RepositoryChangesBrowserApi create(Project project,
                                               Consumer<DefaultActionGroup> toolbarExtender,
                                               List<CommittedChangeList> changeLists) {
        return create(project, toolbarExtender, changeLists, List.of(), null);
    }

    default RepositoryChangesBrowserApi create(Project project,
                                               Consumer<DefaultActionGroup> toolbarExtender,
                                               List<? extends ChangeList> changeLists,
                                               List<Change> changes,
                                               ChangeList initialListSelection) {
        return create(project, toolbarExtender, changeLists, changes, initialListSelection, null);
    }

    RepositoryChangesBrowserApi create(Project project,
                                       Consumer<DefaultActionGroup> toolbarExtender,
                                       List<? extends ChangeList> changeLists,
                                       List<Change> changes,
                                       ChangeList initialListSelection,
                                       VirtualFile toSelect);
}
