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

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ComponentContainer;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class FileEditorManager {
  public static final Key<Boolean> CLOSING_TO_REOPEN = Key.create("CLOSING_TO_REOPEN");

  public static FileEditorManager getInstance(Project project) {
    return project.getComponent(FileEditorManager.class);
  }

  /**
   * @param file file to open. Parameter cannot be null. File should be valid.
   * @return array of opened editors
   */
  public FileEditor[] openFile(VirtualFile file, boolean focusEditor) {
    return openFileWithProviders(file, focusEditor, false).getFirst();
  }

  /**
   * Opens a file
   *
   * @param file        file to open
   * @param focusEditor <code>true</code> if need to focus
   * @return array of opened editors
   */
  public FileEditor[] openFile(VirtualFile file, boolean focusEditor, boolean searchForOpen) {
    return openFileWithProviders(file, focusEditor, searchForOpen).getFirst();
  }

  /**
   * Closes all editors opened for the file.
   *
   * @param file file to be closed. Cannot be null.
   */
  public abstract void closeFile(VirtualFile file);

  /**
   * Works as {@link #openFile(VirtualFile, boolean)} but forces opening of text editor.
   * This method ignores {@link FileEditorPolicy#HIDE_DEFAULT_EDITOR} policy.
   *
   * @return opened text editor. The method returns <code>null</code> in case if text editor wasn't opened.
   */
  @Nullable
  public abstract Editor openTextEditor(OpenFileDescriptor descriptor, boolean focusEditor);

  /**
   * @return currently selected text editor. The method returns <code>null</code> in case
   * there is no selected editor at all or selected editor is not a text one.
   */
  @Nullable
  public Editor getSelectedTextEditor() {
    return getSelectedTextEditor(false);
  }

  /**
   * @param requiredUIThread
   */
  @Nullable
  public abstract Editor getSelectedTextEditor(boolean requiredUIThread);

  /**
   * @return <code>true</code> if <code>file</code> is opened, <code>false</code> otherwise
   */
  public abstract boolean isFileOpen(VirtualFile file);

  /**
   * @return all opened files. Order of files in the array corresponds to the order of editor tabs.
   */
  public abstract VirtualFile[] getOpenFiles();

  /**
   * @return files currently selected. The method returns empty array if there are no selected files.
   * If more than one file is selected (split), the file with most recent focused editor is returned first.
   */
  public abstract VirtualFile[] getSelectedFiles();

  /**
   * @return editors currently selected. The method returns empty array if no editors are open.
   */
  public abstract FileEditor[] getSelectedEditors();

  /**
   * @param file cannot be null
   * @return editor which is currently selected in the currently selected file.
   * The method returns <code>null</code> if <code>file</code> is not opened.
   */
  @Nullable
  public abstract FileEditor getSelectedEditor(VirtualFile file);

  /**
   * @param file cannot be null
   * @return current editors for the specified <code>file</code>
   */
  public abstract FileEditor[] getEditors(VirtualFile file);

  /**
   * @param file cannot be null
   * @return all editors for the specified <code>file</code>
   */
  public abstract FileEditor[] getAllEditors(VirtualFile file);

  /**
   * @return all open editors
   */
  @RequiredUIAccess
  public abstract FileEditor[] getAllEditors();

  /**
   * @deprecated use addTopComponent
   */
  public void showEditorAnnotation(FileEditor editor, JComponent annotationComponent) {
    addTopComponent(editor, annotationComponent);
  }

  /**
   * @deprecated use removeTopComponent
   */
  public void removeEditorAnnotation(FileEditor editor, JComponent annotationComponent) {
    removeTopComponent(editor, annotationComponent);
  }

  /**
   * Add component to top of file editor
   * @return disposer for removing it from editor. null mean not added, if editor not found in registry
   */
  @Nullable
  public abstract Disposable addTopComponent(FileEditor editor, ComponentContainer component);

  @Deprecated
  @DeprecationInfo("addTopComponent(FileEditor, ComponentContainer)")
  public abstract void addTopComponent(FileEditor editor, JComponent component);

  public abstract void removeTopComponent(FileEditor editor, JComponent component);

  public abstract void addBottomComponent(FileEditor editor, JComponent component);

  public abstract void removeBottomComponent(FileEditor editor, JComponent component);

  /**
   * @deprecated Use {@link FileEditorManagerListener#FILE_EDITOR_MANAGER} instead
   */
  public abstract void addFileEditorManagerListener(FileEditorManagerListener listener, Disposable parentDisposable);

  @Deprecated
  @DeprecationInfo("Use async version")
  public abstract List<FileEditor> openEditor(OpenFileDescriptor descriptor, boolean focusEditor);

  /**
   * Returns the project with which the file editor manager is associated.
   *
   * @return the project instance.
   * @since 5.0.1
   */
  public abstract Project getProject();

  /**
   * Selects a specified file editor tab for the specified editor.
   *
   * @param file                 a file to switch the file editor tab for. The function does nothing if the file is not currently open in the editor.
   * @param fileEditorProviderId the ID of the file editor to open; matches the return value of
   *                             {@link consulo.ide.impl.idea.openapi.fileEditor.FileEditorProvider#getEditorTypeId()}
   */
  public abstract void setSelectedEditor(VirtualFile file, String fileEditorProviderId);

  /**
   * @return <code>JComponent</code> which represent the place where all editors are located
   */
  public javax.swing.JComponent getComponent() {
    throw new UnsupportedOperationException("Not supported at this platform");
  }

  public Component getUIComponent() {
    throw new UnsupportedOperationException("Not supported at this platform");
  }

  /**
   * @return preferred focused component inside myEditor tabbed container.
   * This method does similar things like {@link FileEditor#getPreferredFocusedComponent()}
   * but it also tracks (and remember) focus movement inside tabbed container.
   * @see consulo.ide.impl.idea.openapi.fileEditor.impl.DesktopEditorComposite#getPreferredFocusedComponent()
   */
  public abstract javax.swing.@Nullable JComponent getPreferredFocusedComponent();

  public abstract Pair<FileEditor[], FileEditorProvider[]> getEditorsWithProviders(VirtualFile file);

  @Nullable
  public abstract VirtualFile getFile(FileEditor editor);

  public abstract void updateFilePresentation(VirtualFile file);

  /**
   * @return current window in splitters
   */
  public abstract FileEditorWindow getCurrentWindow();

  public abstract AsyncResult<FileEditorWindow> getActiveWindow();

  public abstract void setCurrentWindow(FileEditorWindow window);

  /**
   * Closes editors for the file opened in particular window.
   *
   * @param file file to be closed. Cannot be null.
   */
  public abstract void closeFile(VirtualFile file, FileEditorWindow window);

  public abstract void unsplitWindow();

  public abstract void unsplitAllWindow();

  public abstract int getWindowSplitCount();

  public abstract boolean hasSplitOrUndockedWindows();

  public abstract FileEditorWindow[] getWindows();

  /**
   * @return arrays of all files (including <code>file</code> itself) that belong
   * to the same tabbed container. The method returns empty array if <code>file</code>
   * is not open. The returned files have the same order as they have in the
   * tabbed container.
   */
  public abstract VirtualFile[] getSiblings(VirtualFile file);

  public abstract void createSplitter(int orientation, @Nullable FileEditorWindow window);

  public abstract void changeSplitterOrientation();

  public abstract boolean isInSplitter();

  public abstract boolean hasOpenedFile();

  @Nullable
  public abstract VirtualFile getCurrentFile();

  @Nullable
  public abstract FileEditorWithProvider getSelectedEditorWithProvider(VirtualFile file);

  /**
   * Closes all files IN ACTIVE SPLITTER (window).
   *
   * @see DockManager#getContainers()
   * @see DockContainer#closeAll()
   */
  public abstract void closeAllFiles();

  public abstract FileEditorsSplitters getSplitters();

  @Deprecated
  @DeprecationInfo("Use async version")
  public abstract Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(VirtualFile file, boolean focusEditor, boolean searchForSplitter);

  @RequiredUIAccess
  @Deprecated
  @DeprecationInfo("Use async version")
  public abstract Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(VirtualFile file, boolean focusEditor, FileEditorWindow window);

  /**
   * Asynchronously opens a file and returns a future with the result.
   * Unlike {@link #openFileWithProviders}, this method does not block the calling thread.
   *
   * @param file    file to open. Must be valid.
   * @param options options for opening the file
   * @return a future that completes with the opened editors and providers
   */
  public abstract CompletableFuture<FileEditorOpenResult> openFileAsync(VirtualFile file, FileEditorOpenOptions options);

  public abstract boolean isChanged(FileEditorComposite editor);

  public abstract FileEditorWindow getNextWindow(FileEditorWindow window);

  public abstract FileEditorWindow getPrevWindow(FileEditorWindow window);

  public abstract boolean isInsideChange();

  public abstract Set<FileEditorsSplitters> getAllSplitters();

  public void refreshIconsAsync() {
    Set<FileEditorsSplitters> splitters = getAllSplitters();
    for (FileEditorsSplitters each : splitters) {
      for (VirtualFile file : getOpenFiles()) {
        each.updateFileIconAsync(file);
      }
    }
  }

  public abstract FileEditorsSplitters getSplittersFor(java.awt.Component c);

  public abstract ActionCallback notifyPublisher(Runnable runnable);
}
