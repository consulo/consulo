/*
 * Copyright 2013-2025 consulo.io
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
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.project.Project;

@ExtensionImpl
public class BasicUiDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(DataSink sink, DataSnapshot snapshot) {
        // FileEditor from Editor
        sink.lazyValue(
            FileEditor.KEY,
            dataSnapshot -> {
                Editor editor = dataSnapshot.get(Editor.KEY);
                if (editor == null || Boolean.TRUE.equals(editor.getUserData(InternalEditorKeys.SUPPLEMENTARY_KEY))) {
                    return null;
                }
                return TextEditorProvider.getInstance().getTextEditor(editor);
            }
        );

        // ProjectFileDirectory
        sink.lazyValue(Project.PROJECT_FILE_DIRECTORY, ProjectFileDirectoryRule::getData);

        // FileText
        sink.lazyValue(PlatformDataKeys.FILE_TEXT, FileTextRule::getData);
    }
}
