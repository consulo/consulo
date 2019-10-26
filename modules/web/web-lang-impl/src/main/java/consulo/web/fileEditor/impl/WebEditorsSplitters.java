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
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorWithProviderComposite;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.fileEditor.impl.EditorsSplittersBase;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.layout.WrappedLayout;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class WebEditorsSplitters extends EditorsSplittersBase implements EditorsSplitters {
  private EditorWindow myCurrentWindow;

  private WrappedLayout myLayout;

  public WebEditorsSplitters(FileEditorManagerImpl editorManager) {
    super(editorManager);

    myLayout = WrappedLayout.create();
  }

  @Nonnull
  @Override
  public Component getUIComponent() {
    return myLayout;
  }

  @Override
  public void readExternal(Element element) {

  }

  @Override
  public void writeExternal(Element element) {

  }

  @Override
  public void openFiles(@Nonnull UIAccess uiAccess) {

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
  @RequiredUIAccess
  public EditorWindow getOrCreateCurrentWindow(VirtualFile file) {
    if (myCurrentWindow != null) {
      return myCurrentWindow;
    }
    myCurrentWindow = new WebEditorWindow(myManager, this);
    myLayout.set(myCurrentWindow.getUIComponent());
    return myCurrentWindow;
  }

  @Override
  public void setCurrentWindow(EditorWindow window, boolean requestFocus) {
    myCurrentWindow = window;

    if(window != null) {
      myLayout.set(window.getUIComponent());
    }
  }

  @Nullable
  @Override
  public EditorWindow getCurrentWindow() {
    return myCurrentWindow;
  }

  @Override
  public void updateFileIcon(VirtualFile virtualFile) {

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

  @RequiredUIAccess
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
    if (myCurrentWindow != null) {
      return new EditorWindow[]{myCurrentWindow};
    }
    return EditorWindow.EMPTY_ARRAY;
  }

  @Override
  public EditorWindow[] getOrderedWindows() {
    if (myCurrentWindow != null) {
      return new EditorWindow[]{myCurrentWindow};
    }
    return EditorWindow.EMPTY_ARRAY;
  }

  @Nullable
  @Override
  public VirtualFile getCurrentFile() {
    return null;
  }

  @Override
  public boolean isShowing() {
    return true;
  }
}
