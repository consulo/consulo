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

import consulo.annotation.component.ActionImpl;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.application.dumb.DumbAware;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.action.VcsContextFactory;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CommittedChangesFilterDialog;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.versionBrowser.ChangeBrowserSettings;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
@ActionImpl(id = "ChangesView.Browse")
public class BrowseChangesAction extends AnAction implements DumbAware {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        VirtualFile vFile = e.getRequiredData(VirtualFile.KEY);
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(vFile);
        assert vcs != null;
        CommittedChangesProvider provider = vcs.getCommittedChangesProvider();
        assert provider != null;
        ChangeBrowserSettings settings = provider.createDefaultSettings();
        CommittedChangesFilterDialog dlg = new CommittedChangesFilterDialog(project, provider.createFilterUI(true), settings);
        dlg.show();
        if (!dlg.isOK()) {
            return;
        }

        int maxCount = 0;
        if (!settings.isAnyFilterSpecified()) {
            int rc = Messages.showYesNoCancelDialog(
                project,
                VcsLocalize.browseChangesNoFilterPrompt().get(),
                VcsLocalize.browseChangesTitle().get(),
                VcsLocalize.browseChangesShowRecentButton().get(),
                VcsLocalize.browseChangesShowAllButton().get(),
                CommonLocalize.buttonCancel().get(),
                UIUtil.getQuestionIcon()
            );
            if (rc == 2) {
                return;
            }
            if (rc == 0) {
                maxCount = 50;
            }
        }

        AbstractVcsHelper.getInstance(project).openCommittedChangesTab(vcs, vFile, settings, maxCount, LocalizeValue.empty());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabled(isActionEnabled(e));
    }

    private static boolean isActionEnabled(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        VirtualFile vFile = e.getData(VirtualFile.KEY);
        if (project == null || vFile == null) {
            return false;
        }
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(vFile);
        if (vcs == null || vcs.getCommittedChangesProvider() == null || !vcs.allowsRemoteCalls(vFile)) {
            return false;
        }
        FilePath filePath = VcsContextFactory.SERVICE.getInstance().createFilePathOn(vFile);
        return AbstractVcs.fileInVcsByFileStatus(project, filePath);
    }
}