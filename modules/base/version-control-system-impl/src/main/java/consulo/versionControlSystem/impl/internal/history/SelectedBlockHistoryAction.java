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
package consulo.versionControlSystem.impl.internal.history;

import consulo.annotation.component.ActionImpl;
import consulo.document.FileDocumentManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.action.AbstractVcsAction;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.history.VcsHistoryProvider;
import consulo.versionControlSystem.history.VcsSelection;
import consulo.versionControlSystem.history.VcsSelectionUtil;
import consulo.versionControlSystem.impl.internal.ProjectLevelVcsManagerImpl;
import consulo.versionControlSystem.internal.BackgroundableActionEnabledHandler;
import consulo.versionControlSystem.internal.VcsBackgroundableActions;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "Vcs.ShowHistoryForBlock")
public class SelectedBlockHistoryAction extends AbstractVcsAction {
    public SelectedBlockHistoryAction() {
        super(ActionLocalize.actionVcsShowhistoryforblockText(), ActionLocalize.actionVcsShowhistoryforblockDescription());
    }

    protected boolean isEnabled(VcsContext context) {
        Project project = context.getProject();
        if (project == null) {
            return false;
        }

        VcsSelection selection = VcsSelectionUtil.getSelection(context);
        if (selection == null) {
            return false;
        }

        VirtualFile file = FileDocumentManager.getInstance().getFile(selection.getDocument());
        if (file == null) {
            return false;
        }

        ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(project);
        BackgroundableActionEnabledHandler handler =
            vcsManager.getBackgroundableActionHandler(VcsBackgroundableActions.HISTORY_FOR_SELECTION);
        if (handler.isInProgress(VcsBackgroundableActions.keyFrom(file))) {
            return false;
        }

        AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
        if (activeVcs == null) {
            return false;
        }

        VcsHistoryProvider provider = activeVcs.getVcsBlockHistoryProvider();
        //noinspection SimplifiableIfStatement
        if (provider == null) {
            return false;
        }

        return AbstractVcs.fileInVcsByFileStatus(project, VcsUtil.getFilePath(file));
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull VcsContext context) {
        try {
            Project project = context.getProject();
            assert project != null;

            VcsSelection selection = VcsSelectionUtil.getSelection(context);
            assert selection != null;

            VirtualFile file = FileDocumentManager.getInstance().getFile(selection.getDocument());
            assert file != null;

            AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
            assert activeVcs != null;

            VcsHistoryProvider provider = activeVcs.getVcsBlockHistoryProvider();
            assert provider != null;

            int selectionStart = selection.getSelectionStartLineNumber();
            int selectionEnd = selection.getSelectionEndLineNumber();

            new VcsHistoryProviderBackgroundableProxy(activeVcs, provider, activeVcs.getDiffProvider())
                .createSessionFor(
                    activeVcs.getKeyInstanceMethod(),
                    VcsUtil.getFilePath(file),
                    session -> {
                        if (session == null) {
                            return;
                        }
                        VcsSelectionHistoryDialog vcsHistoryDialog = new VcsSelectionHistoryDialog(
                            project,
                            file,
                            selection.getDocument(),
                            provider,
                            session,
                            activeVcs,
                            Math.min(selectionStart, selectionEnd),
                            Math.max(selectionStart, selectionEnd),
                            selection.getDialogTitle()
                        );

                        vcsHistoryDialog.show();
                    },
                    VcsBackgroundableActions.HISTORY_FOR_SELECTION,
                    false,
                    null
                );
        }
        catch (Exception exception) {
            reportError(exception);
        }
    }

    @Override
    protected void update(@Nonnull VcsContext context, @Nonnull Presentation presentation) {
        presentation.setEnabled(isEnabled(context));
        VcsSelection selection = VcsSelectionUtil.getSelection(context);
        if (selection != null) {
            presentation.setText(selection.getActionName());
        }
    }

    @RequiredUIAccess
    protected static void reportError(Exception exception) {
        Messages.showMessageDialog(
            exception.getLocalizedMessage(),
            VcsLocalize.messageTitleCouldNotLoadFileHistory().get(),
            UIUtil.getErrorIcon()
        );
    }
}
