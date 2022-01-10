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
package com.intellij.openapi.fileEditor.ex;

import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.EditorDataProvider;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.EditorComposite;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class FileEditorManagerEx extends FileEditorManager implements BusyObject {
  protected final List<EditorDataProvider> myDataProviders = new ArrayList<EditorDataProvider>();

  public static FileEditorManagerEx getInstanceEx(Project project) {
    return (FileEditorManagerEx)getInstance(project);
  }

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
  public abstract EditorWindow getCurrentWindow();

  @Nonnull
  public abstract AsyncResult<EditorWindow> getActiveWindow();

  public abstract void setCurrentWindow(EditorWindow window);

  /**
   * Closes editors for the file opened in particular window.
   *
   * @param file file to be closed. Cannot be null.
   */
  public abstract void closeFile(@Nonnull VirtualFile file, @Nonnull EditorWindow window);

  public abstract void unsplitWindow();

  public abstract void unsplitAllWindow();

  public abstract int getWindowSplitCount();

  public abstract boolean hasSplitOrUndockedWindows();

  @Nonnull
  public abstract EditorWindow[] getWindows();

  /**
   * @return arrays of all files (including <code>file</code> itself) that belong
   * to the same tabbed container. The method returns empty array if <code>file</code>
   * is not open. The returned files have the same order as they have in the
   * tabbed container.
   */
  @Nonnull
  public abstract VirtualFile[] getSiblings(@Nonnull VirtualFile file);

  public abstract void createSplitter(int orientation, @Nullable EditorWindow window);

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
  public abstract EditorsSplitters getSplitters();

  @Override
  @Nonnull
  public FileEditor[] openFile(@Nonnull final VirtualFile file, final boolean focusEditor) {
    return openFileWithProviders(file, focusEditor, false).getFirst();
  }

  @Nonnull
  @Override
  public FileEditor[] openFile(@Nonnull VirtualFile file, boolean focusEditor, boolean searchForOpen) {
    return openFileWithProviders(file, focusEditor, searchForOpen).getFirst();
  }

  @Nonnull
  public abstract Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull VirtualFile file, boolean focusEditor, boolean searchForSplitter);

  @Nonnull
  @RequiredUIAccess
  public abstract Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull VirtualFile file, boolean focusEditor, @Nonnull EditorWindow window);

  public abstract boolean isChanged(@Nonnull EditorComposite editor);

  public abstract EditorWindow getNextWindow(@Nonnull final EditorWindow window);

  public abstract EditorWindow getPrevWindow(@Nonnull final EditorWindow window);

  public abstract boolean isInsideChange();

  @Override
  @Nullable
  public final Object getData(@Nonnull Key dataId, @Nonnull Editor editor, @Nonnull Caret caret) {
    for (final EditorDataProvider dataProvider : myDataProviders) {
      final Object o = dataProvider.getData(dataId, editor, caret);
      if (o != null) return o;
    }
    return null;
  }

  @Override
  public void registerExtraEditorDataProvider(@Nonnull final EditorDataProvider provider, Disposable parentDisposable) {
    myDataProviders.add(provider);
    if (parentDisposable != null) {
      Disposer.register(parentDisposable, () -> myDataProviders.remove(provider));
    }
  }

  public void refreshIcons() {
    if (this instanceof FileEditorManagerImpl) {
      final FileEditorManagerImpl mgr = (FileEditorManagerImpl)this;
      Set<EditorsSplitters> splitters = mgr.getAllSplitters();
      for (EditorsSplitters each : splitters) {
        for (VirtualFile file : mgr.getOpenFiles()) {
          each.updateFileIcon(file);
        }
      }
    }
  }

  public abstract EditorsSplitters getSplittersFor(java.awt.Component c);

  @Nonnull
  public abstract ActionCallback notifyPublisher(@Nonnull Runnable runnable);
}
