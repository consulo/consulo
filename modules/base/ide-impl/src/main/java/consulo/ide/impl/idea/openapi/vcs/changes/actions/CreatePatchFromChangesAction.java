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

import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressManager;
import consulo.ide.impl.idea.openapi.vcs.changes.CommitSessionContextAware;
import consulo.ide.impl.idea.openapi.vcs.changes.patch.CreatePatchCommitExecutor;
import consulo.ide.impl.idea.openapi.vcs.changes.shelf.ShelvedChangeList;
import consulo.ide.impl.idea.openapi.vcs.changes.shelf.ShelvedChangesViewManager;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.SessionDialog;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class CreatePatchFromChangesAction extends AnAction implements DumbAware {
  public CreatePatchFromChangesAction() {
    super(
      VcsLocalize.actionNameCreatePatchForSelectedRevisions(),
      VcsLocalize.actionDescriptionCreatePatchForSelectedRevisions(),
      PlatformIconGroup.filetypesPatch()
    );
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    final Change[] changes = e.getData(VcsDataKeys.CHANGES);
    if ((changes == null) || (changes.length == 0)) return;
    String commitMessage = null;
    ShelvedChangeList[] shelvedChangeLists = e.getData(ShelvedChangesViewManager.SHELVED_CHANGELIST_KEY);
    if (shelvedChangeLists != null && shelvedChangeLists.length > 0) {
      commitMessage = shelvedChangeLists [0].DESCRIPTION;
    }
    else {
      ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
      if (changeLists != null && changeLists.length > 0) {
        commitMessage = changeLists [0].getComment();
      }
    }
    if (commitMessage == null) {
      commitMessage = e.getData(VcsDataKeys.PRESET_COMMIT_MESSAGE);
    }
    if (commitMessage == null) {
      commitMessage = "";
    }
    List<Change> changeCollection = new ArrayList<>();
    Collections.addAll(changeCollection, changes);
    createPatch(project, commitMessage, changeCollection);
  }

  public static void createPatch(Project project, String commitMessage, List<Change> changeCollection) {
    project = project == null ? ProjectManager.getInstance().getDefaultProject() : project;
    final CreatePatchCommitExecutor executor = new CreatePatchCommitExecutor(project);
    CommitSession commitSession = executor.createCommitSession();
    if (commitSession instanceof CommitSessionContextAware commitSessionContextAware) {
      commitSessionContextAware.setContext(new CommitContext());
    }
    DialogWrapper sessionDialog = new SessionDialog(executor.getActionText(), project, commitSession, changeCollection, commitMessage);
    sessionDialog.show();
    if (!sessionDialog.isOK()) {
      return;
    }
    preloadContent(project, changeCollection);

    commitSession.execute(changeCollection, commitMessage);
  }

  private static void preloadContent(final Project project, final List<Change> changes) {
    // to avoid multiple progress dialogs, preload content under one progress
    ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
      @Override
      public void run() {
        for (Change change: changes) {
          checkLoadContent(change.getBeforeRevision());
          checkLoadContent(change.getAfterRevision());
        }
      }

      private void checkLoadContent(final ContentRevision revision) {
        ProgressManager.checkCanceled();
        if (revision != null && !(revision instanceof BinaryContentRevision)) {
          try {
            revision.getContent();
          }
          catch (VcsException e1) {
            // ignore at the moment
          }
        }
      }
    }, VcsLocalize.createPatchLoadingContentProgress().get(), true, project);
  }

  @Override
  @RequiredUIAccess
  public void update(final AnActionEvent e) {
    final Boolean haveSelectedChanges = e.getData(VcsDataKeys.HAVE_SELECTED_CHANGES);
    Change[] changes;
    ChangeList[] data1 = e.getData(VcsDataKeys.CHANGE_LISTS);
    ShelvedChangeList[] data2 = e.getData(ShelvedChangesViewManager.SHELVED_CHANGELIST_KEY);
    ShelvedChangeList[] data3 = e.getData(ShelvedChangesViewManager.SHELVED_RECYCLED_CHANGELIST_KEY);

    int sum = data1 == null ? 0 : data1.length;
    sum += data2 == null ? 0 : data2.length;
    sum += data3 == null ? 0 : data3.length;

    e.getPresentation().setEnabled(Boolean.TRUE.equals(haveSelectedChanges) && (sum == 1) &&
                                   ((changes = e.getData(VcsDataKeys.CHANGES)) != null && changes.length > 0));
  }
}
