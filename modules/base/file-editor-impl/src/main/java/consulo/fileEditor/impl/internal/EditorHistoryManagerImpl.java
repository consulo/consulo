/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.fileEditor.impl.internal;

import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponentWithUIState;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.fileEditor.*;
import consulo.fileEditor.event.FileEditorManagerAdapter;
import consulo.fileEditor.event.FileEditorManagerBeforeListener;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.language.psi.PsiDocumentManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.startup.StartupManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@State(name = "EditorHistoryManager", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
@Singleton
@ServiceImpl
public final class EditorHistoryManagerImpl implements PersistentStateComponentWithUIState<Element, HistoryEntry[]>, Disposable, EditorHistoryManager {
  private static final Logger LOG = Logger.getInstance(EditorHistoryManagerImpl.class);

  private final Project myProject;

  public static EditorHistoryManagerImpl getInstance(@Nonnull Project project) {
    return (EditorHistoryManagerImpl)project.getInstance(EditorHistoryManager.class);
  }

  /**
   * State corresponding to the most recent file is the last
   */
  private final List<HistoryEntry> myEntriesList = new ArrayList<>();

  @Inject
  EditorHistoryManagerImpl(@Nonnull Project project) {
    myProject = project;

    MessageBusConnection connection = project.getMessageBus().connect();

    connection.subscribe(UISettingsListener.class, this::trimToSize);

    connection.subscribe(FileEditorManagerBeforeListener.class, new FileEditorManagerBeforeListener.Adapter() {
      @Override
      public void beforeFileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
        updateHistoryEntry(file, false);
      }
    });
    connection.subscribe(FileEditorManagerListener.class, new MyEditorManagerListener());
  }

  private synchronized void addEntry(HistoryEntry entry) {
    myEntriesList.add(entry);
  }

  private synchronized void removeEntry(HistoryEntry entry) {
    boolean removed = myEntriesList.remove(entry);
    if (removed) entry.destroy();
  }

  private synchronized void moveOnTop(HistoryEntry entry) {
    myEntriesList.remove(entry);
    myEntriesList.add(entry);
  }

  /**
   * Makes file most recent one
   */
  @RequiredUIAccess
  private void fileOpenedImpl(@Nonnull final VirtualFile file,
                              @Nullable final FileEditor fallbackEditor,
                              @Nullable FileEditorProvider fallbackProvider) {
    UIAccess.assertIsUIThread();
    // don't add files that cannot be found via VFM (light & etc.)
    if (VirtualFileManager.getInstance().findFileByUrl(file.getUrl()) == null) return;

    final FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(myProject);

    final Pair<FileEditor[], FileEditorProvider[]> editorsWithProviders = editorManager.getEditorsWithProviders(file);
    FileEditor[] editors = editorsWithProviders.getFirst();
    FileEditorProvider[] oldProviders = editorsWithProviders.getSecond();
    if (editors.length <= 0 && fallbackEditor != null) {
      editors = new FileEditor[]{fallbackEditor};
    }
    if (oldProviders.length <= 0 && fallbackProvider != null) {
      oldProviders = new FileEditorProvider[]{fallbackProvider};
    }
    if (editors.length <= 0) {
      LOG.error("No editors for file " + file.getPresentableUrl());
    }
    FileEditor selectedEditor = editorManager.getSelectedEditor(file);
    if (selectedEditor == null) {
      selectedEditor = fallbackEditor;
    }
    LOG.assertTrue(selectedEditor != null);
    final int selectedProviderIndex = ArrayUtil.find(editors, selectedEditor);
    LOG.assertTrue(selectedProviderIndex != -1, "Can't find " + selectedEditor + " among " + Arrays.asList(editors));

    final HistoryEntry entry = getEntry(file);
    if (entry != null) {
      moveOnTop(entry);
    }
    else {
      final FileEditorState[] states = new FileEditorState[editors.length];
      final FileEditorProvider[] providers = new FileEditorProvider[editors.length];
      for (int i = states.length - 1; i >= 0; i--) {
        FileEditorProvider provider = oldProviders[i];
        LOG.assertTrue(provider != null);
        providers[i] = provider;
        FileEditor editor = editors[i];
        if (editor.isValid()) {
          states[i] = editor.getState(FileEditorStateLevel.FULL);
        }
      }
      addEntry(HistoryEntry.createHeavy(myProject, file, providers, states, providers[selectedProviderIndex]));
      trimToSize(UISettings.getInstance());
    }
  }

  public void updateHistoryEntry(@Nullable final VirtualFile file, final boolean changeEntryOrderOnly) {
    updateHistoryEntry(file, null, null, changeEntryOrderOnly);
  }

  @RequiredUIAccess
  private void updateHistoryEntry(@Nullable final VirtualFile file,
                                  @Nullable final FileEditor fallbackEditor,
                                  @Nullable FileEditorProvider fallbackProvider,
                                  final boolean changeEntryOrderOnly) {
    if (file == null) {
      return;
    }
    final FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(myProject);
    final Pair<FileEditor[], FileEditorProvider[]> editorsWithProviders = editorManager.getEditorsWithProviders(file);
    FileEditor[] editors = editorsWithProviders.getFirst();
    FileEditorProvider[] providers = editorsWithProviders.getSecond();
    if (editors.length <= 0 && fallbackEditor != null) {
      editors = new FileEditor[]{fallbackEditor};
      providers = new FileEditorProvider[]{fallbackProvider};
    }

    if (editors.length == 0) {
      // obviously not opened in any editor at the moment,
      // makes no sense to put the file in the history
      return;
    }
    final HistoryEntry entry = getEntry(file);
    if (entry == null) {
      // Size of entry list can be less than number of opened editors (some entries can be removed)
      if (file.isValid()) {
        // the file could have been deleted, so the isValid() check is essential
        fileOpenedImpl(file, fallbackEditor, fallbackProvider);
      }
      return;
    }

    if (!changeEntryOrderOnly) { // update entry state
      //LOG.assertTrue(editors.length > 0);
      for (int i = editors.length - 1; i >= 0; i--) {
        final FileEditor editor = editors[i];
        final FileEditorProvider provider = providers[i];
        if (!editor.isValid()) {
          // this can happen for example if file extension was changed
          // and this method was called during corresponding myEditor close up
          continue;
        }

        final FileEditorState oldState = entry.getState(provider);
        final FileEditorState newState = editor.getState(FileEditorStateLevel.FULL);
        if (!newState.equals(oldState)) {
          entry.putState(provider, newState);
        }
      }
    }
    final FileEditorWithProvider selectedEditorWithProvider = editorManager.getSelectedEditorWithProvider(file);
    if (selectedEditorWithProvider != null) {
      //LOG.assertTrue(selectedEditorWithProvider != null);
      entry.setSelectedProvider(selectedEditorWithProvider.getProvider());
      LOG.assertTrue(entry.getSelectedProvider() != null);

      if (changeEntryOrderOnly) {
        moveOnTop(entry);
      }
    }
  }

  /**
   * @return array of valid files that are in the history, oldest first. May contain duplicates.
   */
  public synchronized VirtualFile[] getFiles() {
    final List<VirtualFile> result = new ArrayList<>(myEntriesList.size());
    for (HistoryEntry entry : myEntriesList) {
      VirtualFile file = entry.getFile();
      if (file != null) result.add(file);
    }
    return VirtualFileUtil.toVirtualFileArray(result);
  }

  /**
   * @return a set of valid files that are in the history, oldest first.
   */
  public LinkedHashSet<VirtualFile> getFileSet() {
    LinkedHashSet<VirtualFile> result = new LinkedHashSet<>();
    for (VirtualFile file : getFiles()) {
      // if the file occurs several times in the history, only its last occurrence counts
      result.remove(file);
      result.add(file);
    }
    return result;
  }

  /**
   * @return a set of valid files that are in the history, oldest first.
   */
  @Nonnull
  public synchronized List<VirtualFile> getFileList() {
    List<VirtualFile> result = new ArrayList<>();
    for (HistoryEntry entry : myEntriesList) {
      VirtualFile file = entry.getFile();
      if (file != null) {
        result.add(file);
      }
    }
    return result;
  }

  @Override
  public synchronized boolean hasBeenOpen(@Nonnull VirtualFile f) {
    for (HistoryEntry each : myEntriesList) {
      if (Comparing.equal(each.getFile(), f)) return true;
    }
    return false;
  }

  /**
   * Removes specified <code>file</code> from history. The method does
   * nothing if <code>file</code> is not in the history.
   *
   * @throws IllegalArgumentException if <code>file</code>
   *                                  is <code>null</code>
   */
  @Override
  public synchronized void removeFile(@Nonnull final VirtualFile file) {
    final HistoryEntry entry = getEntry(file);
    if (entry != null) {
      removeEntry(entry);
    }
  }

  @Override
  public FileEditorState getState(@Nonnull VirtualFile file, final FileEditorProvider provider) {
    final HistoryEntry entry = getEntry(file);
    return entry != null ? entry.getState(provider) : null;
  }

  /**
   * @return may be null
   */
  @Override
  public FileEditorProvider getSelectedProvider(final VirtualFile file) {
    final HistoryEntry entry = getEntry(file);
    return entry != null ? entry.getSelectedProvider() : null;
  }

  private synchronized HistoryEntry getEntry(@Nonnull VirtualFile file) {
    for (int i = myEntriesList.size() - 1; i >= 0; i--) {
      final HistoryEntry entry = myEntriesList.get(i);
      VirtualFile entryFile = entry.getFile();
      if (file.equals(entryFile)) {
        return entry;
      }
    }
    return null;
  }

  /**
   * If total number of files in history more then <code>UISettings.RECENT_FILES_LIMIT</code>
   * then removes the oldest ones to fit the history to new size.
   *
   * @param uiSettings
   */
  private synchronized void trimToSize(UISettings uiSettings) {
    final int limit = uiSettings.RECENT_FILES_LIMIT + 1;
    while (myEntriesList.size() > limit) {
      HistoryEntry removed = myEntriesList.remove(0);
      removed.destroy();
    }
  }

  @Override
  public void loadState(@Nonnull Element element) {
    // we have to delay xml processing because history entries require EditorStates to be created
    // which is done via corresponding EditorProviders, those are not accessible before their
    // is initComponent() called
    final Element state = element.clone();
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> {
      for (Element e : state.getChildren(HistoryEntry.TAG)) {
        try {
          addEntry(HistoryEntry.createHeavy(myProject, e));
        }
        catch (InvalidDataException | ProcessCanceledException e1) {
          // OK here
        }
        catch (Exception anyException) {
          LOG.error(anyException);
        }
      }
      trimToSize(UISettings.getInstance());
    });
  }

  @RequiredUIAccess
  @Override
  public HistoryEntry[] getStateFromUI() {
    final VirtualFile[] openFiles = FileEditorManager.getInstance(myProject).getOpenFiles();
    for (int i = openFiles.length - 1; i >= 0; i--) {
      final VirtualFile file = openFiles[i];
      // we have to update only files that are in history
      if (getEntry(file) != null) {
        updateHistoryEntry(file, false);
      }
    }
    return myEntriesList.toArray(new HistoryEntry[myEntriesList.size()]);
  }

  @RequiredWriteAction
  @Override
  public Element getState(HistoryEntry[] entries) {
    /* update history before saving
    moved to getStateFromUI

    final VirtualFile[] openFiles = FileEditorManager.getInstance(myProject).getOpenFiles();
    for (int i = openFiles.length - 1; i >= 0; i--) {
      final VirtualFile file = openFiles[i];
      // we have to update only files that are in history
      if (getEntry(file) != null) {
        updateHistoryEntry(file, false);
      }
    }*/

    Element element = new Element("state");
    for (final HistoryEntry entry : entries) {
      entry.writeExternal(element, myProject);
    }
    return element;
  }

  @Override
  public synchronized void dispose() {
    for (HistoryEntry entry : myEntriesList) {
      entry.destroy();
    }
    myEntriesList.clear();
  }

  /**
   * Updates history
   */
  private final class MyEditorManagerListener extends FileEditorManagerAdapter {
    @Override
    @RequiredUIAccess
    public void fileOpened(@Nonnull final FileEditorManager source, @Nonnull final VirtualFile file) {
      fileOpenedImpl(file, null, null);
    }

    @Override
    public void selectionChanged(@Nonnull final FileEditorManagerEvent event) {
      // updateHistoryEntry does commitDocument which is 1) very expensive and 2) cannot be performed from within PSI change listener
      // so defer updating history entry until documents committed to improve responsiveness
      PsiDocumentManager.getInstance(myProject).performWhenAllCommitted(() -> {
        updateHistoryEntry(event.getOldFile(), event.getOldEditor(), event.getOldProvider(), false);
        updateHistoryEntry(event.getNewFile(), true);
      });
    }
  }
}
