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

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorWithProviderComposite;
import consulo.fileEditor.impl.EditorsSplitters;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class WebEditorsSplitters implements EditorsSplitters {
  private FileEditorManagerEx myEditorManager;

  public WebEditorsSplitters(FileEditorManagerEx editorManager) {
    myEditorManager = editorManager;
  }

  @Override
  public void readExternal(Element element) {

  }

  @Override
  public void writeExternal(Element element) {

  }

  @Override
  public void openFiles() {

  }

  @Override
  public int getSplitCount() {
    return 0;
  }

  @Override
  public void startListeningFocus() {

  }

  @Override
  public void clear() {

  }

  @Nonnull
  @Override
  public EditorWindow getOrCreateCurrentWindow(VirtualFile file) {
    return new WebEditorWindow(myEditorManager, this);
  }

  @Override
  public void setCurrentWindow(EditorWindow window, boolean requestFocus) {

  }

  @Nullable
  @Override
  public EditorWindow getCurrentWindow() {
    return null;
  }

  @Override
  public void updateFileIcon(VirtualFile virtualFile) {

  }

  @Override
  public void updateFileName(VirtualFile virtualFile) {

  }

  @Override
  public void updateFileColor(VirtualFile virtualFile) {

  }

  @Override
  public void updateFileBackgroundColor(VirtualFile virtualFile) {

  }

  @Override
  public VirtualFile[] getOpenFiles() {
    return new VirtualFile[0];
  }

  @Override
  public AccessToken increaseChange() {
    return null;
  }

  @Override
  public boolean isInsideChange() {
    return false;
  }

  @Override
  public void closeFile(VirtualFile file, boolean moveFocus) {

  }

  @Nonnull
  @Override
  public VirtualFile[] getSelectedFiles() {
    return new VirtualFile[0];
  }

  @Nonnull
  @Override
  public FileEditor[] getSelectedEditors() {
    return new FileEditor[0];
  }

  @Nonnull
  @Override
  public List<EditorWithProviderComposite> findEditorComposites(@Nonnull VirtualFile file) {
    return Collections.emptyList();
  }

  @Override
  public EditorWithProviderComposite[] getEditorsComposites() {
    return new EditorWithProviderComposite[0];
  }

  @Override
  public EditorWindow[] getWindows() {
    return new EditorWindow[0];
  }

  @Override
  public EditorWindow[] getOrderedWindows() {
    return new EditorWindow[0];
  }

  @Nullable
  @Override
  public VirtualFile getCurrentFile() {
    return null;
  }
}
