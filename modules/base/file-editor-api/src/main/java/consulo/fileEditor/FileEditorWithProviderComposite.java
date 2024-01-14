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
package consulo.fileEditor;

import jakarta.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public interface FileEditorWithProviderComposite extends FileEditorComposite {
  @Nonnull
  @Deprecated
  default FileEditorProvider[] getProviders() {
    FileEditor[] editors = getEditors();
    FileEditorProvider[] fileEditorProviders = new FileEditorProvider[editors.length];
    for (int i = 0; i < fileEditorProviders.length; i++) {
      fileEditorProviders[i] = editors[i].getProvider();
    }
    return fileEditorProviders;
  }

  void addEditor(@Nonnull FileEditor editor);

  JComponent getPreferredFocusedComponent();

  default boolean isModified() {
    final FileEditor[] editors = getEditors();
    for (FileEditor editor : editors) {
      if (editor.isModified()) {
        return true;
      }
    }
    return false;
  }
}
