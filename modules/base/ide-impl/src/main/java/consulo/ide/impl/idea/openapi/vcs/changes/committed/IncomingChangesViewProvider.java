/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.ChangesViewContentProvider;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.RepositoryLocation;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author yole
 */
public class IncomingChangesViewProvider implements ChangesViewContentProvider {
  private final Project myProject;
  private final MessageBus myBus;
  private CommittedChangesTreeBrowser myBrowser;
  private MessageBusConnection myConnection;
  private Consumer<List<CommittedChangeList>> myListConsumer;

  public IncomingChangesViewProvider(final Project project) {
    myProject = project;
    myBus = project.getMessageBus();
    myListConsumer = lists -> UIUtil.invokeLaterIfNeeded(new Runnable() {
      @Override
      public void run() {
        myBrowser.getEmptyText().setText(VcsBundle.message("incoming.changes.empty.message"));
        myBrowser.setItems(lists, CommittedChangesBrowserUseCase.INCOMING);
      }
    });
  }

  public JComponent initContent() {
    myBrowser = new CommittedChangesTreeBrowser(myProject, Collections.<CommittedChangeList>emptyList());
    myBrowser.getEmptyText().setText(VcsBundle.message("incoming.changes.not.loaded.message"));
    ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction("IncomingChangesToolbar");
    final ActionToolbar toolbar = myBrowser.createGroupFilterToolbar(myProject, group, null, Collections.<AnAction>emptyList());
    myBrowser.setToolBar(toolbar.getComponent());
    myBrowser.setTableContextMenu(group, Collections.<AnAction>emptyList());
    myConnection = myBus.connect();
    myConnection.subscribe(CommittedChangesListener.class, new MyCommittedChangesListener());
    loadChangesToBrowser(false, true);

    JPanel contentPane = new JPanel(new BorderLayout());
    contentPane.add(myBrowser, BorderLayout.CENTER);
    return contentPane;
  }

  public void disposeContent() {
    myConnection.disconnect();
    Disposer.dispose(myBrowser);
    myBrowser = null;
  }

  private void updateModel(final boolean inBackground, final boolean refresh) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        if (myProject.isDisposed()) return;
        if (myBrowser != null) {
          loadChangesToBrowser(inBackground, refresh);
        }
      }
    });
  }

  private void loadChangesToBrowser(final boolean inBackground, final boolean refresh) {
    final CommittedChangesCache cache = CommittedChangesCache.getInstance(myProject);
    cache.hasCachesForAnyRoot(notEmpty -> {
      if (Boolean.TRUE.equals(notEmpty)) {
        final List<CommittedChangeList> list = cache.getCachedIncomingChanges();
        if (list != null) {
          myBrowser.getEmptyText().setText(VcsBundle.message("incoming.changes.empty.message"));
          myBrowser.setItems(list, CommittedChangesBrowserUseCase.INCOMING);
        } else if (refresh) {
          cache.loadIncomingChangesAsync(myListConsumer, inBackground);
        } else {
          myBrowser.getEmptyText().setText(VcsBundle.message("incoming.changes.empty.message"));
          myBrowser.setItems(Collections.<CommittedChangeList>emptyList(), CommittedChangesBrowserUseCase.INCOMING);
        }
      }
    });
  }

  private class MyCommittedChangesListener extends CommittedChangesAdapter {
    public void changesLoaded(final RepositoryLocation location, final List<CommittedChangeList> changes) {
      updateModel(true, true);
    }

    public void incomingChangesUpdated(final List<CommittedChangeList> receivedChanges) {
      updateModel(true, true);                                                              
    }

    @Override
    public void presentationChanged() {
      updateModel(true, false);
    }

    @Override
    public void changesCleared() {
      myBrowser.getEmptyText().setText(VcsBundle.message("incoming.changes.empty.message"));
      myBrowser.setItems(Collections.<CommittedChangeList>emptyList(), CommittedChangesBrowserUseCase.INCOMING);
    }

    public void refreshErrorStatusChanged(@Nullable final VcsException lastError) {
      if (lastError != null) {
        VcsBalloonProblemNotifier.showOverChangesView(myProject, lastError.getMessage(), NotificationType.ERROR);
      }
    }
  }
}
