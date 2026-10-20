// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.versionControlSystem.change.ignore.action;

import consulo.application.util.UserHomeFileUtil;
import consulo.logging.Logger;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.undoRedo.CommandProcessor;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.change.IgnoredFileBean;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class CreateNewIgnoreFileAction extends DumbAwareAction {
    private static final Logger LOG = Logger.getInstance(CreateNewIgnoreFileAction.class);

    private final String myIgnoreFileName;

    private final VirtualFile myIgnoreFileRoot;

    public CreateNewIgnoreFileAction(String ignoreFileName, VirtualFile ignoreFileRoot) {
        myIgnoreFileName = ignoreFileName;
        myIgnoreFileRoot = ignoreFileRoot;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        AbstractVcs ignoreFileRootVcs = VcsUtil.getVcsFor(project, myIgnoreFileRoot);
        if (ignoreFileRootVcs == null) {
            return;
        }
        List<IgnoredFileBean> ignored = IgnoreFileAction.getIgnoredFileBeans(e, myIgnoreFileRoot, ignoreFileRootVcs);
        if (ignored.isEmpty()) {
            return;
        }

        confirmCreateIgnoreFile().whenCompleteAsync((confirmed, throwable) -> {
            if (!Boolean.TRUE.equals(confirmed) || project.isDisposed()) {
                return;
            }

            VirtualFile[] ignoreFile = new VirtualFile[1];
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .inUndoTransparentAction()
                .inWriteAction()
                .run(() -> {
                    try {
                        ignoreFile[0] = myIgnoreFileRoot.createChildData(myIgnoreFileRoot, myIgnoreFileName);
                    }
                    catch (IOException ex) {
                        LOG.error(ex);
                    }
                });

            if (ignoreFile[0] != null) {
                IgnoreFileAction.writeIgnoreFileEntries(project, ignoreFile[0], ignored, null, null);
            }
        }, project.getUIAccess());
    }

    @RequiredUIAccess
    private CompletableFuture<Boolean> confirmCreateIgnoreFile() {
        String location = Objects.requireNonNullElse(
            UserHomeFileUtil.getLocationRelativeToUserHome(myIgnoreFileRoot.getPresentableUrl()),
            myIgnoreFileRoot.getPresentableUrl()
        );
        return MessageBoxBuilder.<Boolean>create()
            .asQuestion()
            .title(VcsLocalize.vcsAddToIgnoreFileCreateIgnoreFileConfirmationTitle(myIgnoreFileName))
            .text(VcsLocalize.vcsAddToIgnoreFileCreateIgnoreFileConfirmationMessage(myIgnoreFileName, location))
            .button(MessageButtonRole.OK, VcsLocalize.vcsAddToIgnoreFileCreateIgnoreFileButton(), Boolean.TRUE)
            .asDefaultButton()
            .button(MessageButtonRole.CANCEL, CommonLocalize.buttonCancel(), Boolean.FALSE)
            .asExitButton()
            .showAsync();
    }
}
