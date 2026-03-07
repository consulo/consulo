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

import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.InternalEditorKeys;
import consulo.dataContext.DataSnapshot;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.text.TextEditorProvider;
import jakarta.annotation.Nonnull;

public final class FileEditorRule {
  static FileEditor getData(@Nonnull DataSnapshot dataProvider) {
    Editor editor = dataProvider.get(Editor.KEY);
    if (editor == null) {
      return null;
    }

    Boolean aBoolean = editor.getUserData(InternalEditorKeys.SUPPLEMENTARY_KEY);
    if (aBoolean != null && aBoolean) {
      return null;
    }

    return TextEditorProvider.getInstance().getTextEditor(editor);
  }
}
