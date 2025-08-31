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
package consulo.ide.impl.idea.openapi.vcs.changes.shelf;

import consulo.application.CommonBundle;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.versionControlSystem.VcsBundle;
import consulo.ide.impl.idea.openapi.vcs.changes.*;
import consulo.project.util.WaitForProgressToShow;
import consulo.logging.Logger;
import consulo.versionControlSystem.change.*;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collection;

/**
 * @author yole
 * @since 2006-11-23
 */
public class ShelveChangesCommitExecutor extends LocalCommitExecutor {
  private static final Logger LOG = Logger.getInstance(ShelveChangesCommitExecutor.class);

  private final Project myProject;

  public ShelveChangesCommitExecutor(Project project) {
    myProject = project;
  }

  @Nls
  public String getActionText() {
    return VcsBundle.message("shelve.changes.action");
  }

  @Nonnull
  public CommitSession createCommitSession() {
    return new ShelveChangesCommitSession();
  }

  @Override
  public String getHelpId() {
    return "reference.dialogs.vcs.shelve";
  }

  private class ShelveChangesCommitSession implements CommitSession, CommitSessionContextAware {

    @Nullable
    public JComponent getAdditionalConfigurationUI() {
      return null;
    }

    @Override
    public void setContext(CommitContext context) {
    }

    @Nullable
    public JComponent getAdditionalConfigurationUI(Collection<Change> changes, String commitMessage) {
      return null;
    }

    public boolean canExecute(Collection<Change> changes, String commitMessage) {
      return changes.size() > 0;
    }

    public void execute(Collection<Change> changes, String commitMessage) {
      if (changes.size() > 0 && !ChangesUtil.hasFileChanges(changes)) {
        WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
          public void run() {
            Messages
                    .showErrorDialog(myProject, VcsBundle.message("shelve.changes.only.directories"), VcsBundle.message("shelve.changes.action"));
          }
        }, null, myProject);
        return;
      }
      try {
        ShelvedChangeList list = ShelveChangesManager.getInstance(myProject).shelveChanges(changes, commitMessage, true);
        ShelvedChangesViewManager.getInstance(myProject).activateView(list);

        Change[] changesArray = changes.toArray(new Change[changes.size()]);
        // todo better under lock   
        ChangeList changeList = ChangesUtil.getChangeListIfOnlyOne(myProject, changesArray);
        if (changeList instanceof LocalChangeList) {
          LocalChangeList localChangeList = (LocalChangeList) changeList;
          if (localChangeList.getChanges().size() == changes.size() && !localChangeList.isReadOnly() && (! localChangeList.isDefault())) {
            ChangeListManager.getInstance(myProject).removeChangeList(localChangeList.getName());
          }
        }
      }
      catch (final Exception ex) {
        LOG.info(ex);
        WaitForProgressToShow.runOrInvokeLaterAboveProgress(new Runnable() {
          public void run() {
            Messages.showErrorDialog(myProject, VcsBundle.message("create.patch.error.title", ex.getMessage()), CommonBundle.getErrorTitle());
          }
        }, IdeaModalityState.nonModal(), myProject);
      }
    }

    public void executionCanceled() {
    }

    @Override
    public String getHelpId() {
      return null;
    }
  }
}
