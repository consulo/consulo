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
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite;
import com.intellij.openapi.fileEditor.impl.DesktopEditorsSplitters;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 27-Oct-17
 */
public interface EditorWindow {
  Key<EditorWindow> DATA_KEY = Key.create("editorWindow");

  @NotNull
  FileEditorManagerEx getManager();

  void closeAllExcept(VirtualFile selectedFile);

  @Nullable
  EditorWindow split(final int orientation, boolean forceSplit, @Nullable VirtualFile virtualFile, boolean focusNew);

  void unsplit(boolean setCurrent);

  boolean isDisposed();

  @NotNull
  EditorWithProviderComposite[] getEditors();

  @NotNull
  EditorWindow[] findSiblings();

  @NotNull
  VirtualFile[] getFiles();

  @Nullable
  EditorWithProviderComposite findFileComposite(final VirtualFile file);

  @Nullable
  EditorWithProviderComposite getSelectedEditor();

  DesktopEditorsSplitters getOwner();

  VirtualFile getSelectedFile();

  int getTabCount();

  boolean inSplitter();

  void closeFile(final VirtualFile file);

  void closeFile(final VirtualFile file, final boolean disposeIfNeeded);

  void closeFile(@NotNull final VirtualFile file, final boolean disposeIfNeeded, final boolean transferFocus);

  void clear();

  int findFileIndex(final VirtualFile fileToFind);

  void setTabsPlacement(int placement);

  boolean isFilePinned(final VirtualFile file);

  void setFilePinned(final VirtualFile file, final boolean pinned);

  boolean isFileOpen(VirtualFile virtualFile);

  void changeOrientation();

  void unsplitAll();

  default void setEditor(@Nullable final EditorWithProviderComposite editor, final boolean focusEditor) {
    setEditor(editor, true, focusEditor);
  }

  void setEditor(@Nullable final EditorWithProviderComposite editor, final boolean selectEditor, final boolean focusEditor);

  void setAsCurrentWindow(boolean value);

  boolean isValid();

  void restoreClosedTab();

  boolean hasClosedTabs();

  void requestFocus(boolean force);
}
