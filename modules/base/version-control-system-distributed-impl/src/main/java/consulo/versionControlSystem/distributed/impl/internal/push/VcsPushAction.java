/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.distributed.repository.VcsRepositoryManager;
import consulo.versionControlSystem.icon.VersionControlSystemIconGroup;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

@ActionImpl(id = "Vcs.Push")
public class VcsPushAction extends DumbAwareAction {
    public VcsPushAction() {
        super(ActionLocalize.actionVcsPushText(), ActionLocalize.actionVcsPushText(), VersionControlSystemIconGroup.checkin());
    }

    @Nonnull
    private static Collection<Repository> collectRepositories(
        @Nonnull VcsRepositoryManager vcsRepositoryManager,
        @Nullable VirtualFile[] files
    ) {
        if (files == null) {
            return Collections.emptyList();
        }
        Collection<Repository> repositories = new HashSet<>();
        for (VirtualFile file : files) {
            Repository repo = vcsRepositoryManager.getRepositoryForFile(file);
            if (repo != null) {
                repositories.add(repo);
            }
        }
        return repositories;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        VcsRepositoryManager manager = project.getInstance(VcsRepositoryManager.class);
        Collection<Repository> repositories = e.hasData(Editor.KEY)
            ? Collections.emptyList()
            : collectRepositories(manager, e.getData(VirtualFile.KEY_OF_ARRAY));
        VirtualFile selectedFile = DvcsUtil.getSelectedFile(project);
        new VcsPushDialog(
            project,
            DvcsUtil.sortRepositories(repositories),
            selectedFile != null ? manager.getRepositoryForFile(selectedFile) : null
        ).show();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        e.getPresentation().setEnabledAndVisible(
            project != null && !project.getInstance(VcsRepositoryManager.class).getRepositories().isEmpty()
        );
    }
}
