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

import consulo.disposer.Disposable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorWithProvider;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.ui.Component;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public interface EditorComposite extends Disposable {
  @Nonnull
  FileEditorWithProvider getSelectedEditorWithProvider();

  VirtualFile getFile();

  @Nonnull
  FileEditor[] getEditors();

  @Nonnull
  FileEditor getSelectedEditor();

  void setSelectedEditor(final int index);

  List<JComponent> getTopComponents(@Nonnull FileEditor editor);

  List<JComponent> getBottomComponents(@Nonnull FileEditor editor);

  void addTopComponent(FileEditor editor, JComponent component);

  void removeTopComponent(FileEditor editor, JComponent component);

  void addBottomComponent(FileEditor editor, JComponent component);

  void removeBottomComponent(FileEditor editor, JComponent component);

  boolean isPinned();

  boolean isDisposed();

  default Component getUIComponent() {
    throw new UnsupportedOperationException("Unsupported platform");
  }
}
