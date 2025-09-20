// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.internal.largeFileEditor;

import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.LargeFileEditor;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class LargeEditorActionUtil {
    @Nullable
    public static LargeFileEditor tryGetLargeFileEditorManager(AnActionEvent e) {
        FileEditor fileEditor = getFileEditor(e);
        if (fileEditor instanceof LargeFileEditor) {
            return (LargeFileEditor) fileEditor;
        }

        Editor editor = getEditor(e);
        return editor == null ? null : tryGetLargeFileEditorManagerFromEditor(editor);
    }

    @Nullable
    public static LargeFileEditor tryGetLargeFileEditorManagerFromEditor(@Nonnull Editor editor) {
        return editor.getUserData(LargeFileEditor.LARGE_FILE_EDITOR_KEY);
    }

    private static FileEditor getFileEditor(AnActionEvent e) {
        return e.getData(FileEditor.KEY);
    }

    private static Editor getEditor(AnActionEvent e) {
        return e.getData(Editor.KEY);
    }
}