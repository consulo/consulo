/*
 * Copyright 2013-2018 consulo.io
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
package consulo.web.fileEditor.impl;

import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorWithProviderComposite;
import consulo.fileEditor.impl.EditorsSplitters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class WebEditorWindow implements EditorWindow {
  private FileEditorManagerEx myManager;
  private EditorsSplitters myEditorsSplitters;

  public WebEditorWindow(FileEditorManagerEx manager, EditorsSplitters editorsSplitters) {
    myManager = manager;
    myEditorsSplitters = editorsSplitters;
  }

  @Nonnull
  @Override
  public FileEditorManagerEx getManager() {
    return myManager;
  }

  @Override
  public void closeAllExcept(VirtualFile selectedFile) {

  }

  @Nullable
  @Override
  public EditorWindow split(int orientation, boolean forceSplit, @Nullable VirtualFile virtualFile, boolean focusNew) {
    return null;
  }

  @Override
  public void unsplit(boolean setCurrent) {

  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Nonnull
  @Override
  public EditorWithProviderComposite[] getEditors() {
    return new EditorWithProviderComposite[0];
  }

  @Nonnull
  @Override
  public EditorWindow[] findSiblings() {
    return new EditorWindow[0];
  }

  @Nonnull
  @Override
  public VirtualFile[] getFiles() {
    return new VirtualFile[0];
  }

  @Nullable
  @Override
  public EditorWithProviderComposite findFileComposite(VirtualFile file) {
    return null;
  }

  @Nullable
  @Override
  public EditorWithProviderComposite getSelectedEditor() {
    return null;
  }

  @Override
  public EditorsSplitters getOwner() {
    return myEditorsSplitters;
  }

  @Override
  public VirtualFile getSelectedFile() {
    return null;
  }

  @Override
  public int getTabCount() {
    return 0;
  }

  @Override
  public boolean inSplitter() {
    return false;
  }

  @Override
  public void closeFile(VirtualFile file) {

  }

  @Override
  public void closeFile(VirtualFile file, boolean disposeIfNeeded) {

  }

  @Override
  public void closeFile(@Nonnull VirtualFile file, boolean disposeIfNeeded, boolean transferFocus) {

  }

  @Override
  public void clear() {

  }

  @Override
  public int findFileIndex(VirtualFile fileToFind) {
    return 0;
  }

  @Override
  public void setTabsPlacement(int placement) {

  }

  @Override
  public boolean isFilePinned(VirtualFile file) {
    return false;
  }

  @Override
  public void setFilePinned(VirtualFile file, boolean pinned) {

  }

  @Override
  public boolean isFileOpen(VirtualFile virtualFile) {
    return false;
  }

  @Override
  public void changeOrientation() {

  }

  @Override
  public void unsplitAll() {

  }

  @Override
  public void setEditor(@Nullable EditorWithProviderComposite editor, boolean selectEditor, boolean focusEditor) {
    System.out.println("test");
  }

  @Override
  public void setAsCurrentWindow(boolean value) {

  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void restoreClosedTab() {

  }

  @Override
  public boolean hasClosedTabs() {
    return false;
  }

  @Override
  public void requestFocus(boolean force) {

  }
}
