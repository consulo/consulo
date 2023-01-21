/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.dvcs.actions;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.versionControlSystem.distributed.DvcsUtil;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vcs.VcsDataKeys;
import consulo.versionControlSystem.VcsNotifier;
import consulo.ide.impl.idea.openapi.vcs.history.VcsDiffUtil;
import consulo.util.lang.ObjectUtil;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.editor.CommonDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.JBList;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.distributed.repository.AbstractRepositoryManager;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Compares selected file/folder with itself in another branch.
 */
public abstract class DvcsCompareWithBranchAction<T extends Repository> extends DumbAwareAction {

  private static final Logger LOG = Logger.getInstance(DvcsCompareWithBranchAction.class);

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent event) {
    Project project = event.getRequiredData(CommonDataKeys.PROJECT);
    VirtualFile file = getAffectedFile(event);
    T repository = ObjectUtil.assertNotNull(getRepositoryManager(project).getRepositoryForFile(file));
    assert !repository.isFresh();
    String currentBranchName = repository.getCurrentBranchName();
    String presentableRevisionName = currentBranchName;
    if (currentBranchName == null) {
      String currentRevision = ObjectUtil.assertNotNull(repository.getCurrentRevision());
      presentableRevisionName = DvcsUtil.getShortHash(currentRevision);
    }
    List<String> branchNames = getBranchNamesExceptCurrent(repository);

    JBList list = new JBList(branchNames);
    new PopupChooserBuilder<>(list).setTitle("Select branch to compare")
            .setItemChoosenCallback(new OnBranchChooseRunnable(project, file, presentableRevisionName, list)).setAutoselectOnMouseMove(true)
            .setFilteringEnabled(o -> o.toString()).createPopup().showCenteredInCurrentWindow(project);
  }

  @Nonnull
  protected abstract List<String> getBranchNamesExceptCurrent(@Nonnull T repository);

  private static VirtualFile getAffectedFile(@Nonnull AnActionEvent event) {
    final VirtualFile[] vFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    assert vFiles != null && vFiles.length == 1 && vFiles[0] != null : "Illegal virtual files selected: " + Arrays.toString(vFiles);
    return vFiles[0];
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getData(CommonDataKeys.PROJECT);
    VirtualFile file = VcsUtil.getIfSingle(e.getData(VcsDataKeys.VIRTUAL_FILE_STREAM));

    presentation.setVisible(project != null);
    presentation.setEnabled(project != null && file != null && isEnabled(getRepositoryManager(project).getRepositoryForFile(file)));
  }

  private boolean isEnabled(@javax.annotation.Nullable T repository) {
    return repository != null && !repository.isFresh() && !noBranchesToCompare(repository);
  }

  @Nonnull
  protected abstract AbstractRepositoryManager<T> getRepositoryManager(@Nonnull Project project);

  protected abstract boolean noBranchesToCompare(@Nonnull T repository);

  @Nonnull
  protected abstract Collection<Change> getDiffChanges(@Nonnull Project project, @Nonnull VirtualFile file, @Nonnull String branchToCompare)
          throws VcsException;

  private class OnBranchChooseRunnable implements Runnable {
    private final Project myProject;
    private final VirtualFile myFile;
    private final String myHead;
    private final JList myList;

    private OnBranchChooseRunnable(@Nonnull Project project, @Nonnull VirtualFile file, @Nonnull String head, @Nonnull JList list) {
      myProject = project;
      myFile = file;
      myHead = head;
      myList = list;
    }

    @Override
    public void run() {
      Object selectedValue = myList.getSelectedValue();
      if (selectedValue == null) {
        LOG.error("Selected value is unexpectedly null");
        return;
      }
      showDiffWithBranchUnderModalProgress(myProject, myFile, myHead, selectedValue.toString());
    }
  }

  private void showDiffWithBranchUnderModalProgress(@Nonnull final Project project,
                                                    @Nonnull final VirtualFile file,
                                                    @Nonnull final String head,
                                                    @Nonnull final String compare) {
    new Task.Backgroundable(project, "Collecting Changes...", true) {
      private Collection<Change> changes;

      @Override
      public void run(@Nonnull ProgressIndicator indicator) {
        try {
          changes = getDiffChanges(project, file, compare);
        }
        catch (VcsException e) {
          VcsNotifier.getInstance(project).notifyImportantWarning("Couldn't compare with branch", String.format(
                  "Couldn't compare " + DvcsUtil.fileOrFolder(file) + " [%s] with branch [%s];\n %s", file, compare, e.getMessage()));
        }
      }

      @RequiredUIAccess
      @Override
      public void onSuccess() {
        //if changes null -> then exception occurred before
        if (changes != null) {
          VcsDiffUtil.showDiffFor(project, changes, VcsDiffUtil.getRevisionTitle(compare, false), VcsDiffUtil.getRevisionTitle(head, true),
                                  VcsUtil.getFilePath(file));
        }
      }
    }.queue();
  }

  protected static String fileDoesntExistInBranchError(@Nonnull VirtualFile file, @Nonnull String branchToCompare) {
    return String
            .format("%s <code>%s</code> doesn't exist in branch <code>%s</code>", StringUtil.capitalize(DvcsUtil.fileOrFolder(file)), file.getPresentableUrl(),
                    branchToCompare);
  }
}
