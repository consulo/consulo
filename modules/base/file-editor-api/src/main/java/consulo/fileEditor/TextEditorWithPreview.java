/*
 * Copyright 2013-2024 consulo.io
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

import consulo.codeEditor.Editor;
import consulo.navigation.Navigatable;

/**
 * @author VISTALL
 * @since 2024-09-13
 */
public interface TextEditorWithPreview extends TextEditor {
    TextEditor getTextEditor();

    FileEditor getPreviewEditor();

    void switchToPreview();

    @Override
    default Editor getEditor() {
        return getTextEditor().getEditor();
    }

    @Override
    default boolean canNavigateTo(Navigatable navigatable) {
        return getTextEditor().canNavigateTo(navigatable);
    }

    @Override
    default void navigateTo(Navigatable navigatable) {
        getTextEditor().navigateTo(navigatable);
    }
}
