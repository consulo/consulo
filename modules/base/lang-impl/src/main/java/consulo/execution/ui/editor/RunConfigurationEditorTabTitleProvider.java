/*
 * Copyright 2013-2021 consulo.io
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
package consulo.execution.ui.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.EditorTabTitleProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorWithProviderComposite;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 19/12/2021
 */
public class RunConfigurationEditorTabTitleProvider implements EditorTabTitleProvider {
  @Nullable
  @Override
  public String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
    return null;
  }

  @Nullable
  @Override
  public String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file, @Nullable EditorWindow editorWindow) {
    EditorWithProviderComposite composite = editorWindow == null ? null : editorWindow.getSelectedEditor();
    if (composite == null) {
      return null;
    }

    FileEditor selectedEditor = composite.getSelectedEditor();
    if (selectedEditor instanceof RunConfigurationFileEditor && selectedEditor.isModified()) {
      return "*Run Configurations";
    }
    return null;
  }
}
