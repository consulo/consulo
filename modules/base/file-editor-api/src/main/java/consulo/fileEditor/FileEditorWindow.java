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
package consulo.fileEditor;

import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 27-Oct-17
 */
public interface FileEditorWindow {
  FileEditorWindow[] EMPTY_ARRAY = new FileEditorWindow[0];

  Key<FileEditorWindow> DATA_KEY = Key.create("editorWindow");

  @Nonnull
  FileEditorManager getManager();

  void closeAllExcept(VirtualFile selectedFile);

  @Nullable
  FileEditorWindow split(final int orientation, boolean forceSplit, @Nullable VirtualFile virtualFile, boolean focusNew);

  void unsplit(boolean setCurrent);

  boolean isDisposed();

  @Nonnull
  FileEditorWithProviderComposite[] getEditors();

  @Nonnull
  FileEditorWindow[] findSiblings();

  @Nonnull
  VirtualFile[] getFiles();

  @Nullable
  FileEditorWithProviderComposite findFileComposite(final VirtualFile file);

  @Nullable
  FileEditorWithProviderComposite getSelectedEditor();

  @Nonnull
  FileEditorsSplitters getOwner();

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
  default void setEditor(@Nullable final FileEditorWithProviderComposite editor, final boolean focusEditor) {
    setEditor(editor, true, focusEditor);
  }

  @RequiredUIAccess
  void setEditor(@Nullable final FileEditorWithProviderComposite editor, final boolean selectEditor, final boolean focusEditor);

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
