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
package consulo.ide.impl.fileEditor;

import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ui.Component;
import consulo.ui.ex.ComponentContainer;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class UnifiedFileEditorWithProviderComposite implements FileEditorWithProviderComposite {
  private VirtualFile myFile;
  private FileEditor[] myEditors;
  private FileEditorManagerEx myFileEditorManager;

  private final Component[] myComponents;

  public UnifiedFileEditorWithProviderComposite(VirtualFile file,
                                                FileEditor[] editors,
                                                FileEditorManagerEx fileEditorManager) {
    myFile = file;
    myEditors = editors;
    myFileEditorManager = fileEditorManager;

    myComponents = new Component[editors.length];
    for (int i = 0; i < editors.length; i++) {
      FileEditor editor = editors[i];

      myComponents[i] = editor.getUIComponent();
    }
  }

  @Override
  public void addEditor(@Nonnull FileEditor editor) {

  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }

  @Override
  public FileEditor getSelectedEditor() {
    return myEditors[0];
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

  @Override
  public void setSelectedEditor(int index) {

  }

  @Override
  public List<JComponent> getTopComponents(@Nonnull FileEditor editor) {
    return List.of();
  }

  @Nonnull
  @Override
  public Disposable addTopComponent(@Nonnull FileEditor editor, @Nonnull ComponentContainer component) {
    return () -> {};
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
