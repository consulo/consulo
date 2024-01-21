/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.DeprecationInfo;
import consulo.application.AccessRule;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.util.function.ThrowableComputable;
import consulo.component.ProcessCanceledException;
import consulo.fileEditor.*;
import consulo.fileEditor.event.FileEditorManagerBeforeListener;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.impl.internal.FileEditorProviderManagerImpl;
import consulo.ide.impl.fileEditor.FileEditorsSplittersBase;
import consulo.ide.impl.idea.openapi.fileEditor.ex.IdeDocumentHistory;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.dock.DockManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2023-12-24
 */
@Deprecated
@DeprecationInfo("This is service contains sync methods, which must be removed in future")
public abstract class DeprecatedFileEditorManagerImpl extends FileEditorManagerImpl {
  public DeprecatedFileEditorManagerImpl(@Nonnull Application application,
                                         @Nonnull ApplicationConcurrency applicationConcurrency,
                                         @Nonnull Project project, DockManager dockManager) {
    super(application, applicationConcurrency, project, dockManager);
  }


  @Override
  @Nonnull
  @RequiredUIAccess
  public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull final VirtualFile file,
                                                                        boolean focusEditor,
                                                                        final boolean searchForSplitter) {
    if (!file.isValid()) {
      throw new IllegalArgumentException("file is not valid: " + file);
    }
    assertDispatchThread();

    if (isOpenInNewWindow()) {
      return openFileInNewWindow(file);
    }

    FileEditorWindow wndToOpenIn = null;
    if (searchForSplitter) {
      Set<FileEditorsSplitters> all = getAllSplitters();
      FileEditorsSplitters active = getActiveSplittersSync();
      if (active.getCurrentWindow() != null && active.getCurrentWindow().isFileOpen(file)) {
        wndToOpenIn = active.getCurrentWindow();
      }
      else {
        for (FileEditorsSplitters splitters : all) {
          final FileEditorWindow window = splitters.getCurrentWindow();
          if (window == null) continue;

          if (window.isFileOpen(file)) {
            wndToOpenIn = window;
            break;
          }
        }
      }
    }
    else {
      wndToOpenIn = getSplitters().getCurrentWindow();
    }

    FileEditorsSplitters splitters = getSplitters();

    if (wndToOpenIn == null) {
      wndToOpenIn = splitters.getOrCreateCurrentWindow(file);
    }

    UIAccess uiAccess = UIAccess.current();
    openAssociatedFile(uiAccess, file, wndToOpenIn, splitters);
    return openFileImpl2(uiAccess, wndToOpenIn, file, focusEditor);
  }

  @Deprecated
  private void openAssociatedFile(UIAccess uiAccess,
                                  VirtualFile file,
                                  FileEditorWindow wndToOpenIn,
                                  @Nonnull FileEditorsSplitters splitters) {
    FileEditorWindow[] windows = splitters.getWindows();

    if (file != null && windows.length == 2) {
      for (FileEditorAssociateFinder finder : FileEditorAssociateFinder.EP_NAME.getExtensionList(myProject)) {
        VirtualFile associatedFile = finder.getAssociatedFileToOpen(file);

        if (associatedFile != null) {
          FileEditorWindow currentWindow = splitters.getCurrentWindow();
          int idx = windows[0] == wndToOpenIn ? 1 : 0;
          openFileImpl2(uiAccess, windows[idx], associatedFile, false);

          if (currentWindow != null) {
            splitters.setCurrentWindow(currentWindow, false);
          }

          break;
        }
      }
    }
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull VirtualFile file,
                                                                        boolean focusEditor,
                                                                        @Nonnull FileEditorWindow window) {
    if (!file.isValid()) {
      throw new IllegalArgumentException("file is not valid: " + file);
    }
    assertDispatchThread();

    return openFileImpl2(UIAccess.get(), window, file, focusEditor);
  }

  @Override
  @Nonnull
  public Pair<FileEditor[], FileEditorProvider[]> openFileImpl2(@Nonnull UIAccess uiAccess,
                                                                @Nonnull final FileEditorWindow window,
                                                                @Nonnull final VirtualFile file,
                                                                final boolean focusEditor) {
    final Ref<Pair<FileEditor[], FileEditorProvider[]>> result = new Ref<>();
    CommandProcessor.getInstance()
                    .executeCommand(myProject, () -> result.set(openFileImpl3(uiAccess, window, file, focusEditor, null, true)), "", null);
    return result.get();
  }

  /**
   * @param file  to be opened. Unlike openFile method, file can be
   *              invalid. For example, all file were invalidate and they are being
   *              removed one by one. If we have removed one invalid file, then another
   *              invalid file become selected. That's why we do not require that
   *              passed file is valid.
   * @param entry map between FileEditorProvider and FileEditorState. If this parameter
   */
  @Nonnull
  public Pair<FileEditor[], FileEditorProvider[]> openFileImpl3(@Nonnull UIAccess uiAccess,
                                                                @Nonnull final FileEditorWindow window,
                                                                @Nonnull final VirtualFile file,
                                                                final boolean focusEditor,
                                                                @Nullable final HistoryEntry entry,
                                                                boolean current) {
    return openFileImpl4(uiAccess, window, file, entry, current, focusEditor, null, -1);
  }

  @Deprecated
  public Pair<FileEditor[], FileEditorProvider[]> openFileImpl4(@Nonnull UIAccess uiAccess,
                                                                @Nonnull final FileEditorWindow window,
                                                                @Nonnull final VirtualFile file,
                                                                @Nullable final HistoryEntry entry,
                                                                final boolean current,
                                                                final boolean focusEditor,
                                                                final Boolean pin,
                                                                final int index) {
    return openFileImpl4(uiAccess,
                         window,
                         file,
                         entry,
                         new FileEditorOpenOptions().withCurrentTab(current).withFocusEditor(focusEditor).withPin(pin).withIndex(index));
  }

  /**
   * This method can be invoked from background thread. Of course, UI for returned editors should be accessed from EDT in any case.
   */
  @Nonnull
  public Pair<FileEditor[], FileEditorProvider[]> openFileImpl4(@Nonnull UIAccess uiAccess,
                                                                @Nonnull final FileEditorWindow window,
                                                                @Nonnull final VirtualFile file,
                                                                @Nullable final HistoryEntry entry,
                                                                FileEditorOpenOptions options) {
    assert UIAccess.isUIThread() || !ApplicationManager.getApplication()
                                                       .isReadAccessAllowed() : "must not open files under read action since we are doing a lot of invokeAndWaits here";

    int index = options.getIndex();
    boolean current = options.isCurrentTab();
    boolean focusEditor = options.isFocusEditor();
    Boolean pin = options.getPin();

    final Ref<FileEditorWithProviderComposite> compositeRef = new Ref<>();
    if (!options.isReopeningEditorsOnStartup()) {
      uiAccess.giveAndWaitIfNeed(() -> compositeRef.set(window.findFileComposite(file)));
    }

    final FileEditorProvider[] newProviders;
    final AsyncFileEditorProvider.Builder[] builders;
    if (compositeRef.isNull()) {
      // File is not opened yet. In this case we have to create editors
      // and select the created EditorComposite.
      newProviders = FileEditorProviderManager.getInstance(myProject).getProviders(myProject, file);
      if (newProviders.length == 0) {
        return Pair.create(EMPTY_EDITOR_ARRAY, EMPTY_PROVIDER_ARRAY);
      }

      builders = new AsyncFileEditorProvider.Builder[newProviders.length];
      for (int i = 0; i < newProviders.length; i++) {
        try {
          final FileEditorProvider provider = newProviders[i];
          LOG.assertTrue(provider != null, "Provider for file " + file + " is null. All providers: " + Arrays.asList(newProviders));
          ThrowableComputable<AsyncFileEditorProvider.Builder, RuntimeException> action = () -> {
            if (myProject.isDisposed() || !file.isValid()) {
              return null;
            }
            LOG.assertTrue(provider.accept(myProject, file), "Provider " + provider + " doesn't accept file " + file);
            return provider instanceof AsyncFileEditorProvider ? ((AsyncFileEditorProvider)provider).createEditorAsync(myProject,
                                                                                                                       file) : null;
          };
          builders[i] = AccessRule.read(action);
        }
        catch (ProcessCanceledException e) {
          throw e;
        }
        catch (Exception | AssertionError e) {
          LOG.error(e);
        }
      }
    }
    else {
      newProviders = null;
      builders = null;
    }
    Runnable runnable = () -> {
      if (myProject.isDisposed() || !file.isValid()) {
        return;
      }

      compositeRef.set(window.findFileComposite(file));
      boolean newEditor = compositeRef.isNull();
      if (newEditor) {
        getProject().getMessageBus().syncPublisher(FileEditorManagerBeforeListener.class).beforeFileOpened(this, file);

        FileEditor[] newEditors = new FileEditor[newProviders.length];
        for (int i = 0; i < newProviders.length; i++) {
          try {
            final FileEditorProvider provider = newProviders[i];
            final FileEditor editor = builders[i] == null ? provider.createEditor(myProject, file) : builders[i].build();
            LOG.assertTrue(editor.isValid(),
                           "Invalid editor created by provider " + (provider == null ? null : provider.getClass().getName()));
            newEditors[i] = editor;
            // Register PropertyChangeListener into editor
            editor.addPropertyChangeListener(myEditorPropertyChangeListener);
            editor.putUserData(DUMB_AWARE, DumbService.isDumbAware(provider));
          }
          catch (ProcessCanceledException e) {
            throw e;
          }
          catch (Exception | AssertionError e) {
            LOG.error(e);
          }
        }

        // Now we have to create EditorComposite and insert it into the TabbedEditorComponent.
        // After that we have to select opened editor.
        FileEditorWithProviderComposite composite = createComposite(file, newEditors);
        if (composite == null) return;

        if (index >= 0) {
          composite.getFile().putUserData(FileEditorWindow.INITIAL_INDEX_KEY, index);
        }

        compositeRef.set(composite);
      }

      final FileEditorWithProviderComposite composite = compositeRef.get();
      FileEditor[] editors = composite.getEditors();

      window.setEditor(composite, current, focusEditor);

      for (int i = 0; i < editors.length; i++) {
        restoreEditorState(file, editors[i], entry, newEditor);
      }

      // Restore selected editor
      final FileEditorProvider selectedProvider;
      if (entry == null) {
        selectedProvider =
          ((FileEditorProviderManagerImpl)FileEditorProviderManager.getInstance(myProject)).getSelectedFileEditorProvider(
            EditorHistoryManagerImpl.getInstance(myProject),
            file,
            editors);
      }
      else {
        selectedProvider = entry.getSelectedProvider();
      }
      if (selectedProvider != null) {
        for (int i = editors.length - 1; i >= 0; i--) {
          final FileEditor editor = editors[i];
          if (editor.getProvider().equals(selectedProvider)) {
            composite.setSelectedEditor(i);
            break;
          }
        }
      }

      // Notify editors about selection changes
      window.getOwner().setCurrentWindow(window, focusEditor);
      if (window.getOwner() instanceof FileEditorsSplittersBase) {
        ((FileEditorsSplittersBase)window.getOwner()).afterFileOpen(file);
      }
      addSelectionRecord(file, window);

      composite.getSelectedEditor().selectNotify();

      // Transfer focus into editor
      if (focusEditor) {
        //myFirstIsActive = myTabbedContainer1.equals(tabbedContainer);
        window.setAsCurrentWindow(true);

        ToolWindowManager.getInstance(myProject).activateEditorComponent();

        window.getOwner().toFront();
      }

      if (newEditor) {
        notifyPublisher(() -> {
          if (isFileOpen(file)) {
            getProject().getMessageBus().syncPublisher(FileEditorManagerListener.class).fileOpened(this, file);
          }
        });
        ourOpenFilesSetModificationCount.incrementAndGet();
      }

      //[jeka] this is a hack to support back-forward navigation
      // previously here was incorrect call to fireSelectionChanged() with a side-effect
      ((IdeDocumentHistoryImpl)IdeDocumentHistory.getInstance(myProject)).onSelectionChanged();

      // Update frame and tab title
      updateFileName(file);

      // Make back/forward work
      IdeDocumentHistory.getInstance(myProject).includeCurrentCommandAsNavigation();

      if (pin != null) {
        window.setFilePinned(file, pin);
      }
    };

    uiAccess.giveAndWaitIfNeed(runnable);

    FileEditorWithProviderComposite composite = compositeRef.get();
    return Pair.create(composite == null ? EMPTY_EDITOR_ARRAY : composite.getEditors(),
                       composite == null ? EMPTY_PROVIDER_ARRAY : composite.getProviders());
  }
}
