/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 02.11.2006
 * Time: 22:12:19
 */
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.application.dumb.DumbAware;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.component.ProcessCanceledException;
import consulo.document.FileDocumentManager;
import consulo.versionControlSystem.impl.internal.change.ui.awt.ChangesListView;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.RollbackChangesDialog;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.RollbackProgressModifier;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.vcsUtil.RollbackUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.collection.Streams;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.*;
import consulo.versionControlSystem.change.*;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.rollback.RollbackEnvironment;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.*;

import static consulo.ui.ex.awt.Messages.getQuestionIcon;
import static consulo.ui.ex.awt.Messages.showYesNoDialog;

public class RollbackAction extends AnAction implements DumbAware {
  public void update(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    final boolean visible = project != null && ProjectLevelVcsManager.getInstance(project).hasActiveVcss();
    e.getPresentation().setEnabledAndVisible(visible);
    if (!visible) return;

    final Change[] leadSelection = e.getData(VcsDataKeys.CHANGE_LEAD_SELECTION);
    boolean isEnabled = (leadSelection != null && leadSelection.length > 0)
      || Boolean.TRUE.equals(e.getData(VcsDataKeys.HAVE_LOCALLY_DELETED))
      || Boolean.TRUE.equals(e.getData(VcsDataKeys.HAVE_MODIFIED_WITHOUT_EDITING))
      || Boolean.TRUE.equals(e.getData(VcsDataKeys.HAVE_SELECTED_CHANGES))
      || hasReversibleFiles(e)
      || currentChangelistNotEmpty(project);
    e.getPresentation().setEnabled(isEnabled);
    String operationName = RollbackUtil.getRollbackOperationName(project);
    e.getPresentation().setText(operationName + "...");
    if (isEnabled) {
      e.getPresentation().setDescription(UIUtil.removeMnemonic(operationName) + " selected changes");
    }
  }

  private static boolean hasReversibleFiles(@Nonnull AnActionEvent e) {
    ChangeListManager manager = ChangeListManager.getInstance(e.getRequiredData(Project.KEY));
    Set<VirtualFile> modifiedWithoutEditing = ContainerUtil.newHashSet(manager.getModifiedWithoutEditing());

    return Streams.notNullize(e.getData(VcsDataKeys.VIRTUAL_FILE_STREAM)).anyMatch(
      file -> manager.haveChangesUnder(file) != ThreeState.NO || manager.isFileAffected(file) || modifiedWithoutEditing.contains(file)
    );
  }

  private static boolean currentChangelistNotEmpty(Project project) {
    ChangeListManager clManager = ChangeListManager.getInstance(project);
    ChangeList list = clManager.getDefaultChangeList();
    return list != null && !list.getChanges().isEmpty();
  }

  @NonNls
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }
    final String title = ActionPlaces.CHANGES_VIEW_TOOLBAR.equals(e.getPlace())
      ? null : "Can not " + RollbackUtil.getRollbackOperationName(project) + " now";
    if (ChangeListManager.getInstance(project).isFreezedWithNotification(title)) {
      return;
    }
    FileDocumentManager.getInstance().saveAllDocuments();

    List<FilePath> missingFiles = e.getData(ChangesListView.MISSING_FILES_DATA_KEY);
    boolean hasChanges = false;
    if (missingFiles != null && !missingFiles.isEmpty()) {
      hasChanges = true;
      new RollbackDeletionAction().actionPerformed(e);
    }

    List<Change> changes = getChanges(project, e);

    final LinkedHashSet<VirtualFile> modifiedWithoutEditing = getModifiedWithoutEditing(e, project);
    if (modifiedWithoutEditing != null && !modifiedWithoutEditing.isEmpty()) {
      hasChanges = true;
      rollbackModifiedWithoutEditing(project, modifiedWithoutEditing);
    }

    if (modifiedWithoutEditing != null) {
      changes = ContainerUtil.filter(changes, change -> !modifiedWithoutEditing.contains(change.getVirtualFile()));
    }

    if (!changes.isEmpty()) {
      RollbackChangesDialog.rollbackChanges(project, changes);
    }
    else if (!hasChanges) {
      LocalChangeList currentChangeList = ChangeListManager.getInstance(project).getDefaultChangeList();
      RollbackChangesDialog.rollbackChanges(project, currentChangeList);
    }
  }

  @Nonnull
  private static List<Change> getChanges(final Project project, final AnActionEvent e) {
    Change[] changes = e.getData(VcsDataKeys.CHANGES);
    if (changes == null) {
      final VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
      if (files != null) {
        final ChangeListManager clManager = ChangeListManager.getInstance(project);
        final List<Change> changesList = new ArrayList<>();
        for (VirtualFile vf : files) {
          changesList.addAll(clManager.getChangesIn(vf));
        }
        if (!changesList.isEmpty()) {
          changes = changesList.toArray(new Change[changesList.size()]);
        }
      }
    }
    if (changes != null && changes.length > 0) {
      return ContainerUtil.newArrayList(changes);
    }
    return Collections.emptyList();
  }

  @Nullable
  private static LinkedHashSet<VirtualFile> getModifiedWithoutEditing(final AnActionEvent e, Project project) {
    final List<VirtualFile> modifiedWithoutEditing = e.getData(VcsDataKeys.MODIFIED_WITHOUT_EDITING_DATA_KEY);
    if (modifiedWithoutEditing != null && modifiedWithoutEditing.size() > 0) {
      return new LinkedHashSet<>(modifiedWithoutEditing);
    }

    final VirtualFile[] virtualFiles = e.getData(VirtualFile.KEY_OF_ARRAY);
    if (virtualFiles != null && virtualFiles.length > 0) {
      LinkedHashSet<VirtualFile> result = new LinkedHashSet<>(Arrays.asList(virtualFiles));
      result.retainAll(ChangeListManager.getInstance(project).getModifiedWithoutEditing());
      return result;
    }

    return null;
  }

  @NonNls
  private static void rollbackModifiedWithoutEditing(final Project project, final LinkedHashSet<VirtualFile> modifiedWithoutEditing) {
    final String operationName = StringUtil.decapitalize(UIUtil.removeMnemonic(RollbackUtil.getRollbackOperationName(project)));
    LocalizeValue message = (modifiedWithoutEditing.size() == 1)
      ? VcsLocalize.rollbackModifiedWithoutEditingConfirmSingle(
      operationName,
      modifiedWithoutEditing.iterator().next().getPresentableUrl()
    )
      : VcsLocalize.rollbackModifiedWithoutEditingConfirmMultiple(
      operationName,
      modifiedWithoutEditing.size()
    );
    int rc = showYesNoDialog(project, message.get(), VcsLocalize.changesActionRollbackTitle(operationName).get(), getQuestionIcon());
    if (rc != Messages.YES) {
      return;
    }
    final List<VcsException> exceptions = new ArrayList<>();

    final ProgressManager progressManager = ProgressManager.getInstance();
    final Runnable action = () -> {
      final ProgressIndicator indicator = progressManager.getProgressIndicator();
      try {
        ChangesUtil.processVirtualFilesByVcs(project, modifiedWithoutEditing, (vcs, items) -> {
          final RollbackEnvironment rollbackEnvironment = vcs.getRollbackEnvironment();
          if (rollbackEnvironment != null) {
            if (indicator != null) {
              indicator.setText(
                vcs.getDisplayName() + ": performing " +
                  UIUtil.removeMnemonic(rollbackEnvironment.getRollbackOperationName()).toLowerCase() + "..."
              );
              indicator.setIndeterminate(false);
            }
            rollbackEnvironment.rollbackModifiedWithoutCheckout(items, exceptions, new RollbackProgressModifier(items.size(), indicator));
            if (indicator != null) {
              indicator.setText2Value(LocalizeValue.empty());
            }
          }
        });
      }
      catch (ProcessCanceledException e) {
        // for files refresh
      }
      if (!exceptions.isEmpty()) {
        AbstractVcsHelper.getInstance(project).showErrors(
          exceptions,
          VcsLocalize.rollbackModifiedWithoutCheckoutErrorTab(operationName).get()
        );
      }

      VfsUtil.markDirty(true, false, VfsUtilCore.toVirtualFileArray(modifiedWithoutEditing));

      VirtualFileManager.getInstance().asyncRefresh(() -> {
        for (VirtualFile virtualFile : modifiedWithoutEditing) {
          VcsDirtyScopeManager.getInstance(project).fileDirty(virtualFile);
        }
      });
    };
    progressManager.runProcessWithProgressSynchronously(action, operationName, true, project);
  }
}
