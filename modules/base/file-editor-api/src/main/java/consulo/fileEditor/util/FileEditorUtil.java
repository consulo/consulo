/*
 * Copyright 2013-2026 consulo.io
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
package consulo.fileEditor.util;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.internal.RealTextEditor;
import consulo.fileEditor.text.TextEditorProvider;
import org.jspecify.annotations.Nullable;

/**
 * @author UNV
 * @since 2026-09-18
 */
public final class FileEditorUtil {
    /**
     * @return true if the editor is in fact an ordinary file editor;
     * false if the editor is part of EditorTextField, CommitMessage and etc.
     */
    public static boolean isRealFileEditor(@Nullable Editor editor) {
        return editor != null && TextEditorProvider.getInstance().getTextEditor(editor) instanceof RealTextEditor;
    }

    public static @Nullable EditorEx getEditorEx(@Nullable FileEditor fileEditor) {
        return fileEditor instanceof TextEditor textEditor && textEditor.getEditor() instanceof EditorEx editorEx ? editorEx : null;
    }
}
