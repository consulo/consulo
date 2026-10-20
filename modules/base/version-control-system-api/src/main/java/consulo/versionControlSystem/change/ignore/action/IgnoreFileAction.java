// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.change.ignore.action;

import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.IgnoredBeanFactory;
import consulo.versionControlSystem.change.IgnoredFileBean;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.change.ignore.IgnoreFileUtil;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class IgnoreFileAction extends DumbAwareAction {
    private final VirtualFile myIgnoreFile;

    public IgnoreFileAction(VirtualFile ignoreFile) {
        myIgnoreFile = ignoreFile;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        AbstractVcs vcs = VcsUtil.getVcsFor(project, myIgnoreFile);
        if (vcs == null) {
            return;
        }
        VirtualFile ignoreFileRoot = myIgnoreFile.getParent();
        if (ignoreFileRoot == null) {
            return;
        }
        List<IgnoredFileBean> ignored = getIgnoredFileBeans(e, ignoreFileRoot, vcs);
        if (ignored.isEmpty()) {
            return;
        }
        writeIgnoreFileEntries(project, myIgnoreFile, ignored, null, null);
    }

    public static void writeIgnoreFileEntries(
        Project project,
        VirtualFile ignoreFile,
        List<IgnoredFileBean> ignored,
        @Nullable AbstractVcs vcs,
        @Nullable VirtualFile ignoreEntryRoot
    ) {
        IgnoreFileUtil.addNewElements(project, ignoreFile, ignored, vcs != null ? vcs.getKeyInstanceMethod() : null, ignoreEntryRoot);
        VcsDirtyScopeManager.getInstance(project).markEverythingDirty();
        OpenFileDescriptorFactory.getInstance(project).newBuilder(ignoreFile).build().navigate(true);
    }

    static List<IgnoredFileBean> getIgnoredFileBeans(AnActionEvent e, VirtualFile ignoreFileRoot, AbstractVcs vcs) {
        Project project = e.getRequiredData(Project.KEY);
        List<VirtualFile> selectedFiles = getSelectedFiles(e);
        return selectedFiles.stream()
            .filter(it -> VirtualFileUtil.isAncestor(ignoreFileRoot, it, false))
            .filter(it -> vcs.equals(VcsUtil.getVcsFor(project, it)))
            .map(it -> IgnoredBeanFactory.ignoreFile(it, project))
            .toList();
    }

    public static List<VirtualFile> getSelectedFiles(AnActionEvent e) {
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        return files != null ? List.of(files) : List.of();
    }
}
