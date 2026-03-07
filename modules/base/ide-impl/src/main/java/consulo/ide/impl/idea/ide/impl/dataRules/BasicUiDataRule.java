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
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class BasicUiDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(@Nonnull DataSink sink, @Nonnull DataSnapshot snapshot) {
        // FileEditor from Editor
        Editor editor = snapshot.get(Editor.KEY);
        if (editor != null) {
            Boolean supplementary = editor.getUserData(InternalEditorKeys.SUPPLEMENTARY_KEY);
            if (supplementary == null || !supplementary) {
                FileEditor fileEditor = snapshot.get(FileEditor.KEY);
                if (fileEditor == null) {
                    sink.set(FileEditor.KEY, TextEditorProvider.getInstance().getTextEditor(editor));
                }
            }
        }

        // NavigatableArray from selected items or single Navigatable
        Object[] items = snapshot.get(PlatformDataKeys.SELECTED_ITEMS);
        if (items != null) {
            java.util.List<Navigatable> navigatables = new java.util.ArrayList<>();
            for (Object item : items) {
                if (item instanceof Navigatable nav) {
                    navigatables.add(nav);
                }
            }
            if (!navigatables.isEmpty()) {
                sink.set(Navigatable.KEY_OF_ARRAY, navigatables.toArray(new Navigatable[0]));
            }
        }
        else {
            Navigatable navigatable = snapshot.get(Navigatable.KEY);
            if (navigatable != null) {
                sink.set(Navigatable.KEY_OF_ARRAY, new Navigatable[]{navigatable});
            }
        }

        // ProjectFileDirectory
        sink.lazyValue(Project.PROJECT_FILE_DIRECTORY, ProjectFileDirectoryRule::getData);

        // FileText
        sink.lazyValue(PlatformDataKeys.FILE_TEXT, FileTextRule::getData);
    }
}
