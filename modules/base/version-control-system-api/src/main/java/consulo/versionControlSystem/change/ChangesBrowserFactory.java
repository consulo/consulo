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
package consulo.versionControlSystem.change;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-09-06
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface ChangesBrowserFactory {
    ChangesBrowser<Change> createChangeBrowser(Project project,
                                               List<? extends ChangeList> changeLists,
                                               List<Change> changes,
                                               ChangeList initialListSelection,
                                               boolean capableOfExcludingChanges,
                                               boolean highlightProblems,
                                               @Nullable Runnable inclusionListener,
                                               ChangesBrowser.MyUseCase useCase,
                                               @Nullable VirtualFile toSelect);

    ChangesBrowser<Change> createChangeBrowserWithRollback(@Nonnull Project project, @Nonnull List<Change> changes);

    default RepositoryChangesBrowser createRepositoryChangeBrowser(Project project,
                                                                   Consumer<DefaultActionGroup> toolbarExtender,
                                                                   List<CommittedChangeList> changeLists) {
        return createRepositoryChangeBrowser(project, toolbarExtender, changeLists, List.of(), null);
    }

    default RepositoryChangesBrowser createRepositoryChangeBrowser(Project project,
                                                                   Consumer<DefaultActionGroup> toolbarExtender,
                                                                   List<? extends ChangeList> changeLists,
                                                                   List<Change> changes,
                                                                   ChangeList initialListSelection) {
        return createRepositoryChangeBrowser(project, toolbarExtender, changeLists, changes, initialListSelection, null);
    }

    RepositoryChangesBrowser createRepositoryChangeBrowser(Project project,
                                                           Consumer<DefaultActionGroup> toolbarExtender,
                                                           List<? extends ChangeList> changeLists,
                                                           List<Change> changes,
                                                           ChangeList initialListSelection,
                                                           VirtualFile toSelect);
}
