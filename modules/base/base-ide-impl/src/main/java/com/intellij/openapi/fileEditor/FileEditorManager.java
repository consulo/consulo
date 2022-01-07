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
package com.intellij.openapi.fileEditor;

import consulo.disposer.Disposable;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.util.List;

public abstract class FileEditorManager {

  public static final Key<Boolean> USE_CURRENT_WINDOW = Key.create("OpenFile.searchForOpen");

  public static FileEditorManager getInstance(@Nonnull Project project) {
    return project.getComponent(FileEditorManager.class);
  }

  /**
   * @param file file to open. Parameter cannot be null. File should be valid.
   *
   * @return array of opened editors
   */
  @Nonnull
  public abstract FileEditor[] openFile(@Nonnull VirtualFile file, boolean focusEditor);


  /**
   * Opens a file
   *
   *
   * @param file file to open
   * @param focusEditor <code>true</code> if need to focus
   * @return array of opened editors
   */
  @Nonnull
  public FileEditor[] openFile(@Nonnull VirtualFile file, boolean focusEditor, boolean searchForOpen) {
    throw new UnsupportedOperationException("Not implemented");
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
   *
   * @return editor which is currently selected in the currently selected file.
   * The method returns <code>null</code> if <code>file</code> is not opened.
   */
  @Nullable
  public abstract FileEditor getSelectedEditor(@Nonnull VirtualFile file);

  /**
   * @param file cannot be null
   *
   * @return current editors for the specified <code>file</code>
   */
  @Nonnull
  public abstract FileEditor[] getEditors(@Nonnull VirtualFile file);

  /**
   * @param file cannot be null
   *
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
   * @param file a file to switch the file editor tab for. The function does nothing if the file is not currently open in the editor.
   * @param fileEditorProviderId the ID of the file editor to open; matches the return value of
   * {@link com.intellij.openapi.fileEditor.FileEditorProvider#getEditorTypeId()}
   */
  public abstract void setSelectedEditor(@Nonnull VirtualFile file, @Nonnull String fileEditorProviderId);
}
