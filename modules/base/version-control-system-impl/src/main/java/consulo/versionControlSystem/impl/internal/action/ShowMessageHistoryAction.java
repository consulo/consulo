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
package consulo.versionControlSystem.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.CommitMessage;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.checkin.CheckinProjectPanel;
import consulo.versionControlSystem.icon.VersionControlSystemIconGroup;
import consulo.versionControlSystem.impl.internal.ui.awt.TempContentChooser;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.Refreshable;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * Action showing the history of recently used commit messages. Source code of this class is provided
 * as a sample of using the {@link CheckinProjectPanel} API. Actions to be shown in the commit dialog
 * should be added to the <code>Vcs.MessageActionGroup</code> action group.
 *
 * @author lesya
 * @since 5.1
 */
@ActionImpl(id = "Vcs.ShowMessageHistory")
public class ShowMessageHistoryAction extends AnAction implements DumbAware {
    public ShowMessageHistoryAction() {
        super(
            VcsLocalize.actionShowMessageHistoryText(),
            VcsLocalize.actionShowMessageHistoryDescription(),
            VersionControlSystemIconGroup.history()
        );
        setEnabledInModalContext(true);
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        super.update(e);

        Project project = e.getData(Project.KEY);
        Object panel = e.getData(CheckinProjectPanel.PANEL_KEY);
        if (!(panel instanceof CommitMessage)) {
            panel = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        }

        if (project == null || panel == null) {
            e.getPresentation().setEnabledAndVisible(false);
        }
        else {
            e.getPresentation().setVisible(true);
            List<String> recentMessages = VcsConfiguration.getInstance(project).getRecentMessages();
            e.getPresentation().setEnabled(!recentMessages.isEmpty());
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final Project project = e.getRequiredData(Project.KEY);
        Refreshable panel = e.getData(CheckinProjectPanel.PANEL_KEY);
        CommitMessage commitMessage = panel instanceof CommitMessage cmtMsgI ? cmtMsgI : e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);

        if (commitMessage != null) {
            final VcsConfiguration configuration = VcsConfiguration.getInstance(project);

            if (!configuration.getRecentMessages().isEmpty()) {
                TempContentChooser<String> contentChooser =
                    new TempContentChooser<>(project, VcsLocalize.dialogTitleChooseCommitMessageFromHistory().get(), false) {
                        @Override
                        protected void removeContentAt(String content) {
                            configuration.removeMessage(content);
                        }

                        @Override
                        protected String getStringRepresentationFor(String content) {
                            return content;
                        }

                        @Override
                        protected List<String> getContents() {
                            List<String> recentMessages = configuration.getRecentMessages();
                            Collections.reverse(recentMessages);
                            return recentMessages;
                        }
                    };

                contentChooser.show();

                if (contentChooser.isOK()) {
                    int selectedIndex = contentChooser.getSelectedIndex();

                    if (selectedIndex >= 0) {
                        commitMessage.setCommitMessage(contentChooser.getAllContents().get(selectedIndex));
                    }
                }
            }
        }
    }
}
