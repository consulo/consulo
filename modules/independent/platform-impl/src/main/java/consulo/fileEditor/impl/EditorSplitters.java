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

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.DesktopEditorsSplitters;
import com.intellij.openapi.fileEditor.impl.EditorWithProviderComposite;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.annotations.DeprecationInfo;
import consulo.ui.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 28-Oct-17
 */
public interface EditorSplitters {
  Key<DesktopEditorsSplitters> KEY = Key.create("EditorsSplitters");

  @Nullable
  EditorWindow getCurrentWindow();

  void updateFileIcon(VirtualFile virtualFile);

  void updateFileName(VirtualFile virtualFile);

  void updateFileColor(VirtualFile virtualFile);

  void updateFileBackgroundColor(VirtualFile virtualFile);

  VirtualFile[] getOpenFiles();

  AccessToken increaseChange();

  void closeFile(VirtualFile file, boolean moveFocus);

  @NotNull
  VirtualFile[] getSelectedFiles();

  @NotNull
  FileEditor[] getSelectedEditors();

  @NotNull
  List<EditorWithProviderComposite> findEditorComposites(@NotNull VirtualFile file);

  EditorWithProviderComposite[] getEditorsComposites();

  EditorWindow[] getWindows();

  @Nullable
  VirtualFile getCurrentFile();

  @NotNull
  default Component getUIComponent() {
    throw new AbstractMethodError();
  }

  @Deprecated
  @DeprecationInfo("See #getUIComponent()")
  @NotNull
  default javax.swing.JComponent getComponent() {
    throw new AbstractMethodError();
  }
}
