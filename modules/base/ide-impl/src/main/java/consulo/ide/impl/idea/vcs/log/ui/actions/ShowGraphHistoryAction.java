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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.application.util.registry.Registry;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.vcs.log.data.VcsLogDataImpl;
import consulo.ide.impl.idea.vcs.log.data.VcsLogStructureFilterImpl;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogContentProvider;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogManager;
import consulo.ide.impl.idea.vcs.log.impl.VcsProjectLog;
import consulo.versionControlSystem.util.VcsUtil;
import jakarta.annotation.Nonnull;

import java.util.Collections;

public class ShowGraphHistoryAction extends DumbAwareAction {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        VirtualFile file = e.getRequiredData(VirtualFile.KEY);
        VcsLogManager logManager = VcsProjectLog.getInstance(project).getLogManager();
        assert logManager != null;
        VcsLogStructureFilterImpl fileFilter = new VcsLogStructureFilterImpl(Collections.singleton(VcsUtil.getFilePath(file)));
        VcsLogContentProvider.openLogTab(logManager, project, file.getName(), fileFilter);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (!Registry.is("vcs.log.graph.history")) {
            presentation.setEnabledAndVisible(false);
        }
        else {
            VirtualFile file = e.getData(VirtualFile.KEY);
            Project project = e.getData(Project.KEY);
            if (file == null || project == null) {
                presentation.setEnabledAndVisible(false);
            }
            else {
                VirtualFile root = ProjectLevelVcsManager.getInstance(project).getVcsRootFor(file);
                VcsLogDataImpl dataManager = VcsProjectLog.getInstance(project).getDataManager();
                if (root == null || dataManager == null) {
                    presentation.setEnabledAndVisible(false);
                }
                else {
                    presentation.setVisible(dataManager.getRoots().contains(root));
                    presentation.setEnabled(dataManager.getIndex().isIndexed(root));
                }
            }
        }
    }
}
