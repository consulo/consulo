/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.fileEditor;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.Set;

public abstract class FileEditorManager {
  public static final Key<Boolean> CLOSING_TO_REOPEN = Key.create("CLOSING_TO_REOPEN");

  public static FileEditorManager getInstance(@Nonnull Project project) {
    return project.getComponent(FileEditorManager.class);
  }

  /**
   * @param file file to open. Parameter cannot be null. File should be valid.
   * @return array of opened editors
   */
  @Nonnull
  public FileEditor[] openFile(@Nonnull final VirtualFile file, final boolean focusEditor) {
    return openFileWithProviders(file, focusEditor, false).getFirst();
  }

  /**
   * Opens a file
   *
   * @param file        file to open
   * @param focusEditor <code>true</code> if need to focus
   * @return array of opened editors
   */
  @Nonnull
  public FileEditor[] openFile(@Nonnull VirtualFile file, boolean focusEditor, boolean searchForOpen) {
    return openFileWithProviders(file, focusEditor, searchForOpen).getFirst();
  }

  /**
   * Closes all editors opened for the file.
   *
   * @param file file to be closed. Cannot be null.
   */
  public abstract void closeFile(@Nonnull VirtualFile file);

  /**
   * Works as {@link #openFile(VirtualFile, boolean)} but forces opening of text editor.
   * This method ignores {@link FileEditorPolicy#HIDE_DEFAULT_EDITOR} policy.
   *
   * @return opened text editor. The method returns <code>null</code> in case if text editor wasn't opened.
   */
  @Nullable
  public abstract Editor openTextEditor(@Nonnull OpenFileDescriptor descriptor, boolean focusEditor);

  /**
   * @return currently selected text editor. The method returns <code>null</code> in case
   * there is no selected editor at all or selected editor is not a text one.
   */
  @Nullable
  public abstract Editor getSelectedTextEditor();

  /**
   * @return <code>true</code> if <code>file</code> is opened, <code>false</code> otherwise
   */
  public abstract boolean isFileOpen(@Nonnull VirtualFile file);

  /**
   * @return all opened files. Order of files in the array corresponds to the order of editor tabs.
   */
  @Nonnull
  public abstract VirtualFile[] getOpenFiles();

  /**
   * @return files currently selected. The method returns empty array if there are no selected files.
   * If more than one file is selected (split), the file with most recent focused editor is returned first.
   */
  @Nonnull
  public abstract VirtualFile[] getSelectedFiles();

  /**
   * @return editors currently selected. The method returns empty array if no editors are open.
   */
  @Nonnull
  public abstract FileEditor[] getSelectedEditors();

  /**
   * @param file cannot be null
   * @return editor which is currently selected in the currently selected file.
   * The method returns <code>null</code> if <code>file</code> is not opened.
   */
  @Nullable
  public abstract FileEditor getSelectedEditor(@Nonnull VirtualFile file);

  /**
   * @param file cannot be null
   * @return current editors for the specified <code>file</code>
   */
  @Nonnull
  public abstract FileEditor[] getEditors(@Nonnull VirtualFile file);

  /**
   * @param file cannot be null
   * @return all editors for the specified <code>file</code>
   */
  @Nonnull
  public abstract FileEditor[] getAllEditors(@Nonnull VirtualFile file);

  /**
   * @return all open editors
   */
  @Nonnull
  public abstract FileEditor[] getAllEditors();

  /**
   * @deprecated use addTopComponent
   */
  public abstract void showEditorAnnotation(@Nonnull FileEditor editor, @Nonnull JComponent annotationComponent);

  /**
   * @deprecated use removeTopComponent
   */
  public abstract void removeEditorAnnotation(@Nonnull FileEditor editor, @Nonnull JComponent annotationComponent);

  public abstract void addTopComponent(@Nonnull final FileEditor editor, @Nonnull final JComponent component);

  public abstract void removeTopComponent(@Nonnull final FileEditor editor, @Nonnull final JComponent component);

  public abstract void addBottomComponent(@Nonnull final FileEditor editor, @Nonnull final JComponent component);

  public abstract void removeBottomComponent(@Nonnull final FileEditor editor, @Nonnull final JComponent component);


  /**
   * Adds specified <code>listener</code>
   *
   * @param listener listener to be added
   * @deprecated Use MessageBus instead: see {@link FileEditorManagerListener#FILE_EDITOR_MANAGER}
   */
  public abstract void addFileEditorManagerListener(@Nonnull FileEditorManagerListener listener);

  /**
   * @deprecated Use {@link FileEditorManagerListener#FILE_EDITOR_MANAGER} instead
   */
  public abstract void addFileEditorManagerListener(@Nonnull FileEditorManagerListener listener, @Nonnull Disposable parentDisposable);

  /**
   * Removes specified <code>listener</code>
   *
   * @param listener listener to be removed
   * @deprecated Use {@link FileEditorManagerListener#FILE_EDITOR_MANAGER} instead
   */
  public abstract void removeFileEditorManagerListener(@Nonnull FileEditorManagerListener listener);

  @Nonnull
  public abstract List<FileEditor> openEditor(@Nonnull OpenFileDescriptor descriptor, boolean focusEditor);

  /**
   * Returns the project with which the file editor manager is associated.
   *
   * @return the project instance.
   * @since 5.0.1
   */
  @Nonnull
  public abstract Project getProject();

  public abstract void registerExtraEditorDataProvider(@Nonnull EditorDataProvider provider, Disposable parentDisposable);

  /**
   * Returns data associated with given editor/caret context. Data providers are registered via
   * {@link #registerExtraEditorDataProvider(EditorDataProvider, Disposable)} method.
   */
  @Nullable
  public abstract Object getData(@Nonnull Key<?> dataId, @Nonnull Editor editor, @Nonnull Caret caret);

  /**
   * Selects a specified file editor tab for the specified editor.
   *
   * @param file                 a file to switch the file editor tab for. The function does nothing if the file is not currently open in the editor.
   * @param fileEditorProviderId the ID of the file editor to open; matches the return value of
   *                             {@link com.intellij.openapi.fileEditor.FileEditorProvider#getEditorTypeId()}
   */
  public abstract void setSelectedEditor(@Nonnull VirtualFile file, @Nonnull String fileEditorProviderId);

  /**
   * @return <code>JComponent</code> which represent the place where all editors are located
   */
  @Nonnull
  public javax.swing.JComponent getComponent() {
    throw new UnsupportedOperationException("Not supported at this platform");
  }

  @Nonnull
  public Component getUIComponent() {
    throw new UnsupportedOperationException("Not supported at this platform");
  }

  /**
   * @return preferred focused component inside myEditor tabbed container.
   * This method does similar things like {@link FileEditor#getPreferredFocusedComponent()}
   * but it also tracks (and remember) focus movement inside tabbed container.
   * @see com.intellij.openapi.fileEditor.impl.DesktopEditorComposite#getPreferredFocusedComponent()
   */
  @Nullable
  public abstract javax.swing.JComponent getPreferredFocusedComponent();

  @Nonnull
  public abstract Pair<FileEditor[], FileEditorProvider[]> getEditorsWithProviders(@Nonnull VirtualFile file);

  @Nullable
  public abstract VirtualFile getFile(@Nonnull FileEditor editor);

  public abstract void updateFilePresentation(@Nonnull VirtualFile file);

  /**
   * @return current window in splitters
   */
  public abstract FileEditorWindow getCurrentWindow();

  @Nonnull
  public abstract AsyncResult<FileEditorWindow> getActiveWindow();

  public abstract void setCurrentWindow(FileEditorWindow window);

  /**
   * Closes editors for the file opened in particular window.
   *
   * @param file file to be closed. Cannot be null.
   */
  public abstract void closeFile(@Nonnull VirtualFile file, @Nonnull FileEditorWindow window);

  public abstract void unsplitWindow();

  public abstract void unsplitAllWindow();

  public abstract int getWindowSplitCount();

  public abstract boolean hasSplitOrUndockedWindows();

  @Nonnull
  public abstract FileEditorWindow[] getWindows();

  /**
   * @return arrays of all files (including <code>file</code> itself) that belong
   * to the same tabbed container. The method returns empty array if <code>file</code>
   * is not open. The returned files have the same order as they have in the
   * tabbed container.
   */
  @Nonnull
  public abstract VirtualFile[] getSiblings(@Nonnull VirtualFile file);

  public abstract void createSplitter(int orientation, @Nullable FileEditorWindow window);

  public abstract void changeSplitterOrientation();

  public abstract boolean isInSplitter();

  public abstract boolean hasOpenedFile();

  @Nullable
  public abstract VirtualFile getCurrentFile();

  @Nullable
  public abstract FileEditorWithProvider getSelectedEditorWithProvider(@Nonnull VirtualFile file);

  /**
   * Closes all files IN ACTIVE SPLITTER (window).
   *
   * @see com.intellij.ui.docking.DockManager#getContainers()
   * @see com.intellij.ui.docking.DockContainer#closeAll()
   */
  public abstract void closeAllFiles();

  @Nonnull
  public abstract FileEditorsSplitters getSplitters();

  @Nonnull
  public abstract Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull VirtualFile file, boolean focusEditor, boolean searchForSplitter);

  @Nonnull
  @RequiredUIAccess
  public abstract Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull VirtualFile file, boolean focusEditor, @Nonnull FileEditorWindow window);

  public abstract boolean isChanged(@Nonnull FileEditorComposite editor);

  public abstract FileEditorWindow getNextWindow(@Nonnull final FileEditorWindow window);

  public abstract FileEditorWindow getPrevWindow(@Nonnull final FileEditorWindow window);

  public abstract boolean isInsideChange();

  public abstract Set<FileEditorsSplitters> getAllSplitters();

  public void refreshIcons() {
    Set<FileEditorsSplitters> splitters = getAllSplitters();
    for (FileEditorsSplitters each : splitters) {
      for (VirtualFile file : getOpenFiles()) {
        each.updateFileIcon(file);
      }
    }
  }

  public abstract FileEditorsSplitters getSplittersFor(java.awt.Component c);

  @Nonnull
  public abstract ActionCallback notifyPublisher(@Nonnull Runnable runnable);
}
