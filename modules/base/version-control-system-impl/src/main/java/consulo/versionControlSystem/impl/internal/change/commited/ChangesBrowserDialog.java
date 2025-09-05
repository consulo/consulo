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

package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.application.Application;
import consulo.application.CommonBundle;
import consulo.application.util.function.AsynchConsumer;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.impl.internal.change.ui.awt.AdjustComponentWhenShown;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author max
 */
public class ChangesBrowserDialog extends DialogWrapper {
  private Project myProject;
  private CommittedChangesTableModel myChanges;
  private Mode myMode;
  private CommittedChangesBrowser myCommittedChangesBrowser;
  private AsynchConsumer<List<CommittedChangeList>> myAppender;
  private final Consumer<ChangesBrowserDialog> myInitRunnable;

  public enum Mode { Simple, Browse, Choose }

  public ChangesBrowserDialog(Project project, CommittedChangesTableModel changes, Mode mode, Consumer<ChangesBrowserDialog> initRunnable) {
    super(project, true);
    myInitRunnable = initRunnable;
    initDialog(project, changes, mode);
  }

  public ChangesBrowserDialog(Project project, Component parent, CommittedChangesTableModel changes, Mode mode, Consumer<ChangesBrowserDialog> initRunnable) {
    super(parent, true);
    myInitRunnable = initRunnable;
    initDialog(project, changes, mode);
  }

  private void initDialog(Project project, CommittedChangesTableModel changes, Mode mode) {
    myProject = project;
    myChanges = changes;
    myMode = mode;
    setTitle(VcsBundle.message("dialog.title.changes.browser"));
    setCancelButtonText(CommonBundle.getCloseButtonText());
      ModalityState currentState = Application.get().getCurrentModalityState();
    if ((mode != Mode.Choose) && (ModalityState.nonModal().equals(currentState))) {
      setModal(false);
    }
    myAppender = new AsynchConsumer<List<CommittedChangeList>>() {

      @Override
      public void finished() {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            if (ChangesBrowserDialog.this.isShowing()) {
              myCommittedChangesBrowser.stopLoading();
            }
          }
        });
      }

      @Override
      public void accept(final List<CommittedChangeList> committedChangeLists) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            if (ChangesBrowserDialog.this.isShowing()) {
              boolean selectFirst = (myChanges.getRowCount() == 0) && (!committedChangeLists.isEmpty());
              myChanges.addRows(committedChangeLists);
              if (selectFirst) {
                myCommittedChangesBrowser.selectFirstIfAny();
              }
            }
          }
        });
      }
    };

    init();

    if (myInitRunnable != null) {
      new AdjustComponentWhenShown() {
        @Override
        protected boolean init() {
          myInitRunnable.accept(ChangesBrowserDialog.this);
          return true;
        }
      }.install(myCommittedChangesBrowser);
    }
  }

  public AsynchConsumer<List<CommittedChangeList>> getAppender() {
    return myAppender;
  }

  public void startLoading() {
    myCommittedChangesBrowser.startLoading();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "VCS.ChangesBrowserDialog";
  }

  @Override
  protected JComponent createCenterPanel() {
    myCommittedChangesBrowser = new CommittedChangesBrowser(myProject, myChanges);
    return myCommittedChangesBrowser;
  }

  @Override
  protected void dispose() {
    super.dispose();
    myCommittedChangesBrowser.dispose();
  }

  @Override
  protected void createDefaultActions() {
    super.createDefaultActions();
    if (myMode == Mode.Browse) {
      getOKAction().putValue(Action.NAME, VcsBundle.message("button.search.again"));
    }
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    if (myMode == Mode.Simple) {
      return new Action[] { getCancelAction() };
    }
    return super.createActions();
  }

  public CommittedChangeList getSelectedChangeList() {
    return myCommittedChangesBrowser.getSelectedChangeList();
  }
}
