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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.RepositoryChangesBrowserApi;
import consulo.versionControlSystem.internal.RepositoryChangesBrowserFactory;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2025-08-30
 */
@ServiceImpl
@Singleton
public class RepositoryChangesBrowserFactoryImpl implements RepositoryChangesBrowserFactory {
    @Override
    public RepositoryChangesBrowserApi create(Project project,
                                              Consumer<DefaultActionGroup> toolbarExtender,
                                              List<? extends ChangeList> changeLists,
                                              List<Change> changes,
                                              ChangeList initialListSelection,
                                              VirtualFile toSelect) {
        return new RepositoryChangesBrowser(project, changeLists, changes, initialListSelection, toSelect) {
            @Override
            protected void buildToolBar(DefaultActionGroup toolBarGroup) {
                super.buildToolBar(toolBarGroup);

                toolbarExtender.accept(toolBarGroup);
            }
        };
    }
}
