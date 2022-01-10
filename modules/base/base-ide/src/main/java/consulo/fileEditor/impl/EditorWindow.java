/*
 * Copyright 2013-2017 consulo.io
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
package consulo.fileEditor.impl;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import consulo.util.dataholder.Key;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 27-Oct-17
 */
public interface EditorWindow {
  EditorWindow[] EMPTY_ARRAY = new EditorWindow[0];

  Key<EditorWindow> DATA_KEY = Key.create("editorWindow");

  @Nonnull
  FileEditorManagerEx getManager();

  void closeAllExcept(VirtualFile selectedFile);

  @Nullable
  EditorWindow split(final int orientation, boolean forceSplit, @Nullable VirtualFile virtualFile, boolean focusNew);

  void unsplit(boolean setCurrent);

  boolean isDisposed();

  @Nonnull
  EditorWithProviderComposite[] getEditors();

  @Nonnull
  EditorWindow[] findSiblings();

  @Nonnull
  VirtualFile[] getFiles();

  @Nullable
  EditorWithProviderComposite findFileComposite(final VirtualFile file);

  @Nullable
  EditorWithProviderComposite getSelectedEditor();

  @Nonnull
  EditorsSplitters getOwner();

  VirtualFile getSelectedFile();

  int getTabCount();

  boolean inSplitter();

  default void closeFile(final VirtualFile file) {
    closeFile(file, true);
  }

  default void closeFile(final VirtualFile file, final boolean disposeIfNeeded) {
    closeFile(file, disposeIfNeeded, true);
  }

  void closeFile(@Nonnull final VirtualFile file, final boolean disposeIfNeeded, final boolean transferFocus);

  void clear();

  int findFileIndex(final VirtualFile fileToFind);

  void setTabsPlacement(int placement);

  boolean isFilePinned(final VirtualFile file);

  void setFilePinned(final VirtualFile file, final boolean pinned);

  boolean isFileOpen(VirtualFile virtualFile);

  void changeOrientation();

  void unsplitAll();

  @RequiredUIAccess
  default void setEditor(@Nullable final EditorWithProviderComposite editor, final boolean focusEditor) {
    setEditor(editor, true, focusEditor);
  }

  @RequiredUIAccess
  void setEditor(@Nullable final EditorWithProviderComposite editor, final boolean selectEditor, final boolean focusEditor);

  void setAsCurrentWindow(boolean value);

  boolean isValid();

  void restoreClosedTab();

  boolean hasClosedTabs();

  void requestFocus(boolean force);

  @Nonnull
  default Component getUIComponent() {
    throw new UnsupportedOperationException("Unsupported platform");
  }
}
