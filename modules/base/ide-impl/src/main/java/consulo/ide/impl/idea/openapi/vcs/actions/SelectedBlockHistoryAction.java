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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.vcs.history.VcsHistoryProviderBackgroundableProxy;
import consulo.ide.impl.idea.openapi.vcs.history.impl.VcsSelectionHistoryDialog;
import consulo.ide.impl.idea.openapi.vcs.impl.BackgroundableActionEnabledHandler;
import consulo.ide.impl.idea.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import consulo.ide.impl.idea.openapi.vcs.impl.VcsBackgroundableActions;
import consulo.ide.impl.idea.vcsUtil.VcsSelectionUtil;
import consulo.project.Project;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.Messages;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.action.VcsContext;
import consulo.versionControlSystem.history.VcsHistoryProvider;
import consulo.versionControlSystem.history.VcsSelection;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;

public class SelectedBlockHistoryAction extends AbstractVcsAction {

  protected boolean isEnabled(VcsContext context) {
    Project project = context.getProject();
    if (project == null) return false;

    VcsSelection selection = VcsSelectionUtil.getSelection(context);
    if (selection == null) return false;

    VirtualFile file = FileDocumentManager.getInstance().getFile(selection.getDocument());
    if (file == null) return false;

    final ProjectLevelVcsManagerImpl vcsManager = (ProjectLevelVcsManagerImpl)ProjectLevelVcsManager.getInstance(project);
    final BackgroundableActionEnabledHandler handler = vcsManager.getBackgroundableActionHandler(VcsBackgroundableActions.HISTORY_FOR_SELECTION);
    if (handler.isInProgress(VcsBackgroundableActions.keyFrom(file))) return false;

    AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
    if (activeVcs == null) return false;

    VcsHistoryProvider provider = activeVcs.getVcsBlockHistoryProvider();
    if (provider == null) return false;

    if (!AbstractVcs.fileInVcsByFileStatus(project, VcsUtil.getFilePath(file))) return false;
    return true;
  }

  @Override
  public void actionPerformed(@Nonnull final VcsContext context) {
    try {
      final Project project = context.getProject();
      assert project != null;

      final VcsSelection selection = VcsSelectionUtil.getSelection(context);
      assert selection != null;

      final VirtualFile file = FileDocumentManager.getInstance().getFile(selection.getDocument());
      assert file != null;

      final AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
      assert activeVcs != null;

      final VcsHistoryProvider provider = activeVcs.getVcsBlockHistoryProvider();
      assert provider != null;

      final int selectionStart = selection.getSelectionStartLineNumber();
      final int selectionEnd = selection.getSelectionEndLineNumber();

      new VcsHistoryProviderBackgroundableProxy(activeVcs, provider, activeVcs.getDiffProvider()).
              createSessionFor(activeVcs.getKeyInstanceMethod(), VcsUtil.getFilePath(file), session -> {
                if (session == null) return;
                final VcsSelectionHistoryDialog vcsHistoryDialog =
                        new VcsSelectionHistoryDialog(project, file, selection.getDocument(), provider, session, activeVcs, Math.min(selectionStart, selectionEnd),
                                                      Math.max(selectionStart, selectionEnd), selection.getDialogTitle());

                vcsHistoryDialog.show();
              }, VcsBackgroundableActions.HISTORY_FOR_SELECTION, false, null);
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

  protected static void reportError(Exception exception) {
    Messages.showMessageDialog(exception.getLocalizedMessage(), VcsBundle.message("message.title.could.not.load.file.history"), Messages.getErrorIcon());
  }
}
