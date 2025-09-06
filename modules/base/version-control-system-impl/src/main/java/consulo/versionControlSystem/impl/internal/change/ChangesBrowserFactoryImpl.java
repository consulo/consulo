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
package consulo.versionControlSystem.impl.internal.change;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.impl.internal.change.commited.InternalRepositoryChangesBrowser;
import consulo.versionControlSystem.impl.internal.ui.awt.InternalChangesBrowser;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-09-06
 */
@ServiceImpl
@Singleton
public class ChangesBrowserFactoryImpl implements ChangesBrowserFactory {
    @Override
    public ChangesBrowser<Change> createChangeBrowser(Project project, List<? extends ChangeList> changeLists, List<Change> changes, ChangeList initialListSelection, boolean capableOfExcludingChanges, boolean highlightProblems, @Nullable Runnable inclusionListener, ChangesBrowser.MyUseCase useCase, @Nullable VirtualFile toSelect) {
        return new InternalChangesBrowser(project, changeLists, changes, initialListSelection, capableOfExcludingChanges, highlightProblems, inclusionListener, useCase, toSelect);
    }

    @Override
    public RepositoryChangesBrowser createRepositoryChangeBrowser(Project project,
                                                                  Consumer<DefaultActionGroup> toolbarExtender,
                                                                  List<? extends ChangeList> changeLists,
                                                                  List<Change> changes,
                                                                  ChangeList initialListSelection,
                                                                  VirtualFile toSelect) {
        return new InternalRepositoryChangesBrowser(project, changeLists, changes, initialListSelection, toSelect) {
            @Override
            protected void buildToolBar(DefaultActionGroup toolBarGroup) {
                super.buildToolBar(toolBarGroup);

                toolbarExtender.accept(toolBarGroup);
            }
        };
    }
}
