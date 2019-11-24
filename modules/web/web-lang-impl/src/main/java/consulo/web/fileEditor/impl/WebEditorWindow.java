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

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorWindowBase;
import consulo.fileEditor.impl.EditorWithProviderComposite;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.fileTypes.impl.VfsIconUtil;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.Tab;
import consulo.ui.layout.TabbedLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class WebEditorWindow extends EditorWindowBase implements EditorWindow {
  private FileEditorManagerImpl myManager;
  private EditorsSplitters myEditorsSplitters;

  private TabbedLayout myTabbedLayout = TabbedLayout.create();

  private Map<EditorWithProviderComposite, Tab> myEditors = new LinkedHashMap<>();

  @RequiredUIAccess
  public WebEditorWindow(FileEditorManagerImpl manager, EditorsSplitters editorsSplitters) {
    myManager = manager;
    myEditorsSplitters = editorsSplitters;
  }

  @Override
  @Nonnull
  public Component getUIComponent() {
    return myTabbedLayout;
  }

  @Override
  public int getTabCount() {
    return myEditors.size();
  }

  @Override
  protected EditorWithProviderComposite getEditorAt(int i) {
    return myEditors.keySet().toArray(new EditorWithProviderComposite[myEditors.size()])[i];
  }

  @Override
  protected void setTitleAt(int index, String text) {
    EditorWithProviderComposite editorAt = getEditorAt(index);
    Tab tab = myEditors.get(editorAt);
    tab.clearText();
    tab.append(text);
  }

  @Override
  protected void setBackgroundColorAt(int index, Color color) {

  }

  @Override
  protected void setToolTipTextAt(int index, String text) {

  }

  @Nonnull
  @Override
  public FileEditorManagerImpl getManager() {
    return myManager;
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
  public EditorWindow[] findSiblings() {
    return new EditorWindow[0];
  }

  @Nullable
  @Override
  public EditorWithProviderComposite getSelectedEditor() {
    return null;
  }

  @Nonnull
  @Override
  public EditorsSplitters getOwner() {
    return myEditorsSplitters;
  }

  @Override
  public VirtualFile getSelectedFile() {
    return null;
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
    myEditors.clear();
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
    for (EditorWithProviderComposite editor : myEditors.keySet()) {
      if (editor.getFile().equals(virtualFile)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void changeOrientation() {

  }

  @Override
  public void unsplitAll() {

  }

  @RequiredUIAccess
  @Override
  public void setEditor(@Nullable EditorWithProviderComposite editor, boolean selectEditor, boolean focusEditor) {
    if (editor == null) {

    }
    else {
      EditorWithProviderComposite fileComposite = findFileComposite(editor.getFile());
      if (fileComposite == null) {
        Tab tab = myTabbedLayout.addTab(editor.getFile().getName(), editor.getUIComponent());
        tab.setCloseHandler((thisTab, component) -> {
          myEditors.remove(editor);
        });
        tab.setIcon(VfsIconUtil.getIcon(editor.getFile(), 0, myManager.getProject()));
        myEditors.put(editor, tab);
      }
      else {
        Tab tab = myEditors.get(fileComposite);
        assert tab != null;
        tab.select();
      }
    }
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
