/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 * @since 2006-12-15
 */
public class EditAction extends AnAction {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e.getData(Project.KEY);
        List<VirtualFile> files = e.getRequiredData(VcsDataKeys.MODIFIED_WITHOUT_EDITING_DATA_KEY);
        editFilesAndShowErrors(project, files);
    }

    public static void editFilesAndShowErrors(Project project, List<VirtualFile> files) {
        final List<VcsException> exceptions = new ArrayList<>();
        editFiles(project, files, exceptions);
        if (!exceptions.isEmpty()) {
            AbstractVcsHelper.getInstance(project).showErrors(exceptions, VcsLocalize.editErrors());
        }
    }

    public static void editFiles(final Project project, final List<VirtualFile> files, final List<VcsException> exceptions) {
        ChangesUtil.processVirtualFilesByVcs(project, files, (vcs, items) -> {
            final EditFileProvider provider = vcs.getEditFileProvider();
            if (provider != null) {
                try {
                    provider.editFiles(VfsUtil.toVirtualFileArray(items));
                }
                catch (VcsException e1) {
                    exceptions.add(e1);
                }
                for (VirtualFile file : items) {
                    VcsDirtyScopeManager.getInstance(project).fileDirty(file);
                    FileStatusManager.getInstance(project).fileStatusChanged(file);
                }
            }
        });
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        List<VirtualFile> files = e.getData(VcsDataKeys.MODIFIED_WITHOUT_EDITING_DATA_KEY);
        boolean enabled = files != null && !files.isEmpty();
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}