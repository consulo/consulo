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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.util.DateFormatUtil;
import consulo.component.messagebus.MessageBusConnection;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.CachingCommittedChangesProvider;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.commited.CommittedChangesListener;
import consulo.versionControlSystem.impl.internal.change.commited.CommittedChangesCache;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * @author yole
 * todo: use EditorNotifications
 */
@Singleton
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
@ServiceImpl
public class OutdatedVersionNotifier {
  private static final Logger LOG = Logger.getInstance(OutdatedVersionNotifier.class);

  private final Provider<FileEditorManager> myFileEditorManager;
  private final CommittedChangesCache myCache;
  private final Project myProject;
  private static final Key<OutdatedRevisionPanel> PANEL_KEY = Key.create("OutdatedRevisionPanel");
  private volatile boolean myIncomingChangesRequested;

  @Inject
  public OutdatedVersionNotifier(Provider<FileEditorManager> fileEditorManager,
                                 CommittedChangesCache cache,
                                 Project project) {
    myFileEditorManager = fileEditorManager;
    myCache = cache;
    myProject = project;
    MessageBusConnection busConnection = project.getMessageBus().connect();
    busConnection.subscribe(CommittedChangesListener.class, new CommittedChangesAdapter() {
      @Override
      public void incomingChangesUpdated(@Nullable final List<CommittedChangeList> receivedChanges) {
        if (myCache.getCachedIncomingChanges() == null) {
          requestLoadIncomingChanges();
        }
        else {
          updateAllEditorsLater();
        }
      }

      @Override
      public void changesCleared() {
        updateAllEditorsLater();
      }
    });
    busConnection.subscribe(FileEditorManagerListener.class, new MyFileEditorManagerListener());
  }

  private void requestLoadIncomingChanges() {
    debug("Requesting load of incoming changes");
    if (!myIncomingChangesRequested) {
      myIncomingChangesRequested = true;
      myCache.loadIncomingChangesAsync(committedChangeLists -> {
        myIncomingChangesRequested = false;
        updateAllEditorsLater();
      }, true);
    }
  }

  private static void debug(@NonNls String message) {
    LOG.debug(message);
  }

  private void updateAllEditorsLater() {
    debug("Queueing update of editors");
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        updateAllEditors();
      }
    }, myProject.getDisposed());
  }

  private void updateAllEditors() {
    if (myCache.getCachedIncomingChanges() == null) {
      requestLoadIncomingChanges();
      return;
    }
    debug("Updating editors");
    final VirtualFile[] files = myFileEditorManager.get().getOpenFiles();
    for (VirtualFile file : files) {
      final Pair<CommittedChangeList, Change> pair = myCache.getIncomingChangeList(file);
      final FileEditor[] fileEditors = myFileEditorManager.get().getEditors(file);
      for (FileEditor editor : fileEditors) {
        final OutdatedRevisionPanel oldPanel = editor.getUserData(PANEL_KEY);
        if (pair != null) {
          if (oldPanel != null) {
            oldPanel.setChangeList(pair.first, pair.second);
          }
          else {
            initPanel(pair.first, pair.second, editor);
          }
        }
        else if (oldPanel != null) {
          myFileEditorManager.get().removeTopComponent(editor, oldPanel);
          editor.putUserData(PANEL_KEY, null);
        }
      }
    }
  }

  private void initPanel(final CommittedChangeList list, final Change c, final FileEditor editor) {
    if (!isIncomingChangesSupported(list)) {
      return;
    }
    final OutdatedRevisionPanel component = new OutdatedRevisionPanel(list, c);
    editor.putUserData(PANEL_KEY, component);
    myFileEditorManager.get().addTopComponent(editor, component);
  }

  private class MyFileEditorManagerListener implements FileEditorManagerListener {
    @Override
    public void fileOpened(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
      if (myCache.getCachedIncomingChanges() == null) {
        requestLoadIncomingChanges();
      }
      else {
        final Pair<CommittedChangeList, Change> pair = myCache.getIncomingChangeList(file);
        if (pair != null) {
          final FileEditor[] fileEditors = source.getEditors(file);
          for (FileEditor editor : fileEditors) {
            initPanel(pair.first, pair.second, editor);
          }
        }
      }
    }
  }

  private static class OutdatedRevisionPanel extends EditorNotificationPanel {
    private CommittedChangeList myChangeList;

    public OutdatedRevisionPanel(CommittedChangeList changeList, final Change c) {
      super();
      createActionLabel(VcsBundle.message("outdated.version.show.diff.action"), "Compare.LastVersion");
      createActionLabel(VcsBundle.message("outdated.version.update.project.action"), "Vcs.UpdateProject");
      myChangeList = changeList;
      updateLabelText(c);
    }

    private void updateLabelText(final Change c) {
      String comment = myChangeList.getComment();
      int pos = comment.indexOf("\n");
      if (pos >= 0) {
        comment = comment.substring(0, pos).trim() + "...";
      }
      final String formattedDate = DateFormatUtil.formatPrettyDateTime(myChangeList.getCommitDate());
      final boolean dateIsPretty = !formattedDate.contains("/");
      final String key = c.getType() == Change.Type.DELETED ? "outdated.version.text.deleted" :
        (dateIsPretty ? "outdated.version.pretty.date.text" : "outdated.version.text");
      myLabel.setText(VcsBundle.message(key, myChangeList.getCommitterName(), formattedDate, comment));
    }

    public void setChangeList(final CommittedChangeList changeList, final Change c) {
      myChangeList = changeList;
      updateLabelText(c);
    }
  }

  private static boolean isIncomingChangesSupported(@Nonnull CommittedChangeList list) {
    CachingCommittedChangesProvider provider = list.getVcs().getCachingCommittedChangesProvider();
    return provider != null && provider.supportsIncomingChanges();
  }
}
