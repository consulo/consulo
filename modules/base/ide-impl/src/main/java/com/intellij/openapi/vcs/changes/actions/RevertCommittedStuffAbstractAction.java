/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.vcs.changes.actions;

import consulo.language.editor.CommonDataKeys;
import com.intellij.openapi.diff.impl.patch.BinaryFilePatch;
import com.intellij.openapi.diff.impl.patch.FilePatch;
import com.intellij.openapi.diff.impl.patch.IdeaTextPatchBuilder;
import com.intellij.openapi.diff.impl.patch.formove.PatchApplier;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsBundle;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vcs.changes.ui.ChangeListChooser;
import com.intellij.util.WaitForProgressToShow;
import com.intellij.util.containers.Convertor;
import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.document.FileDocumentManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class RevertCommittedStuffAbstractAction extends AnAction implements DumbAware {
  private final Convertor<AnActionEvent, Change[]> myForUpdateConvertor;
  private final Convertor<AnActionEvent, Change[]> myForPerformConvertor;
  private static final Logger LOG = Logger.getInstance(RevertCommittedStuffAbstractAction.class);

  public RevertCommittedStuffAbstractAction(final Convertor<AnActionEvent, Change[]> forUpdateConvertor,
                                            final Convertor<AnActionEvent, Change[]> forPerformConvertor) {
    myForUpdateConvertor = forUpdateConvertor;
    myForPerformConvertor = forPerformConvertor;
  }

  public void actionPerformed(final AnActionEvent e) {
    final Project project = e.getRequiredData(CommonDataKeys.PROJECT);
    final VirtualFile baseDir = project.getBaseDir();
    assert baseDir != null;
    final Change[] changes = myForPerformConvertor.convert(e);
    if (changes == null || changes.length == 0) return;
    final List<Change> changesList = new ArrayList<Change>();
    Collections.addAll(changesList, changes);
    FileDocumentManager.getInstance().saveAllDocuments();

    String defaultName = null;
    final ChangeList[] changeLists = e.getData(VcsDataKeys.CHANGE_LISTS);
    if (changeLists != null && changeLists.length > 0) {
      defaultName = VcsBundle.message("revert.changes.default.name", changeLists[0].getName());
    }

    final ChangeListChooser chooser = new ChangeListChooser(project, ChangeListManager.getInstance(project).getChangeListsCopy(), null,
                                                      "Select Target Changelist", defaultName);
    chooser.show();
    if (!chooser.isOK()) return;

    final List<FilePatch> patches = new ArrayList<FilePatch>();
    ProgressManager.getInstance().run(new Task.Backgroundable(project, VcsBundle.message("revert.changes.title"), true,
                                                              BackgroundFromStartOption.getInstance()) {
      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        try {
          final List<Change> preprocessed = ChangesPreprocess.preprocessChangesRemoveDeletedForDuplicateMoved(changesList);
          patches.addAll(IdeaTextPatchBuilder.buildPatch(project, preprocessed, baseDir.getPresentableUrl(), true));
        }
        catch (final VcsException ex) {
          WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
            @Override
            public void run() {
              Messages.showErrorDialog(project, "Failed to revert changes: " + ex.getMessage(), VcsBundle.message("revert.changes.title"));
            }
          }, null, (Project)myProject);
          indicator.cancel();
        }
      }

      @RequiredUIAccess
      @Override
      public void onSuccess() {
        new PatchApplier<BinaryFilePatch>(project, baseDir, patches, chooser.getSelectedList(), null, null).execute();
      }
    });
  }

  public void update(final AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final Change[] changes = myForUpdateConvertor.convert(e);
    e.getPresentation().setEnabled(project != null && changes != null && changes.length > 0);
  }
}
