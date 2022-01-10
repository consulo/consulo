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
package consulo.fileEditor.impl;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider;
import com.intellij.openapi.fileEditor.impl.HistoryEntry;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.Component;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class UnifiedEditorWithProviderComposite implements EditorWithProviderComposite {
  private VirtualFile myFile;
  private FileEditor[] myEditors;
  private FileEditorProvider[] myProviders;
  private FileEditorManagerEx myFileEditorManager;

  private final Component[] myComponents;

  public UnifiedEditorWithProviderComposite(VirtualFile file, FileEditor[] editors, FileEditorProvider[] providers, FileEditorManagerEx fileEditorManager) {
    myFile = file;
    myEditors = editors;
    myProviders = providers;
    myFileEditorManager = fileEditorManager;

    myComponents = new Component[editors.length];
    for (int i = 0; i < editors.length; i++) {
      FileEditor editor = editors[i];

      myComponents[i] = editor.getUIComponent();
    }
  }

  @Nonnull
  @Override
  public FileEditorProvider[] getProviders() {
    return myProviders;
  }

  @Nonnull
  @Override
  public HistoryEntry currentStateAsHistoryEntry() {
    return null;
  }

  @Override
  public void addEditor(@Nonnull FileEditor editor, FileEditorProvider provider) {

  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }

  @Override
  public FileEditorWithProvider getSelectedEditorWithProvider() {
    return new FileEditorWithProvider(myEditors[0], myProviders[0]);
  }

  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  @Nonnull
  @Override
  public FileEditor[] getEditors() {
    return myEditors;
  }

  @Nonnull
  @Override
  public FileEditor getSelectedEditor() {
    return myEditors[0];
  }

  @Override
  public void setSelectedEditor(int index) {

  }

  @Override
  public List<JComponent> getTopComponents(@Nonnull FileEditor editor) {
    return Collections.emptyList();
  }

  @Override
  public List<JComponent> getBottomComponents(@Nonnull FileEditor editor) {
    return Collections.emptyList();
  }

  @Override
  public void addTopComponent(FileEditor editor, JComponent component) {

  }

  @Override
  public void removeTopComponent(FileEditor editor, JComponent component) {

  }

  @Override
  public void addBottomComponent(FileEditor editor, JComponent component) {

  }

  @Override
  public void removeBottomComponent(FileEditor editor, JComponent component) {

  }

  @Override
  public boolean isPinned() {
    return false;
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public void dispose() {

  }

  @Override
  public Component getUIComponent() {
    return myComponents[0];
  }
}
