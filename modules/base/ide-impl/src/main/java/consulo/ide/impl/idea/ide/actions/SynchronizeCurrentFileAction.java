// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions;

import consulo.ide.IdeBundle;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.util.collection.JBIterable;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;

import java.util.List;

public class SynchronizeCurrentFileAction extends AnAction implements DumbAware {
    @Override
    public void update(@Nonnull AnActionEvent e) {
        List<VirtualFile> files = getFiles(e).take(2).toList();
        e.getPresentation().setEnabledAndVisible(e.hasData(Project.KEY) && !files.isEmpty());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        List<VirtualFile> files = getFiles(e).toList();
        if (project == null || files.isEmpty()) {
            return;
        }

        for (VirtualFile file : files) {
            if (file.isDirectory()) {
                file.getChildren();
            }
            if (file instanceof NewVirtualFile newVirtualFile) {
                newVirtualFile.markClean();
                newVirtualFile.markDirtyRecursively();
            }
        }

        RefreshQueue.getInstance().refresh(true, true, () -> postRefresh(project, files), files);
    }

    private static void postRefresh(Project project, List<? extends VirtualFile> files) {
        VcsDirtyScopeManager dirtyScopeManager = VcsDirtyScopeManager.getInstance(project);
        for (VirtualFile f : files) {
            if (f.isDirectory()) {
                dirtyScopeManager.dirDirtyRecursively(f);
            }
            else {
                dirtyScopeManager.fileDirty(f);
            }
        }
    }

    @Nonnull
    private static JBIterable<VirtualFile> getFiles(@Nonnull AnActionEvent e) {
        return JBIterable.of(e.getData(VirtualFile.KEY_OF_ARRAY)).filter(VirtualFile::isInLocalFileSystem);
    }
}