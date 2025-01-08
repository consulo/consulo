/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.impl.dataRules;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.InternalEditorKeys;
import consulo.dataContext.DataProvider;
import consulo.dataContext.GetDataRule;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class FileEditorRule implements GetDataRule<FileEditor> {
  @Nonnull
  @Override
  public Key<FileEditor> getKey() {
    return FileEditor.KEY;
  }

  @Override
  public FileEditor getData(@Nonnull DataProvider dataProvider) {
    final Editor editor = dataProvider.getDataUnchecked(Editor.KEY);
    if (editor == null) {
      return null;
    }

    final Boolean aBoolean = editor.getUserData(InternalEditorKeys.SUPPLEMENTARY_KEY);
    if (aBoolean != null && aBoolean) {
      return null;
    }

    return TextEditorProvider.getInstance().getTextEditor(editor);
  }
}
