// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.toolbar.floating;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKind;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.light.LightVirtualFileBase;

public final class FloatingToolbarUtil {
    private FloatingToolbarUtil() {
    }

    public static boolean isInsideMainEditor(DataContext dataContext) {
        Editor editor = dataContext.getData(Editor.KEY);
        if (editor == null) {
            return false;
        }
        return editor.getEditorKind() != EditorKind.DIFF && isFileEditor(editor);
    }

    private static boolean isFileEditor(Editor editor) {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        VirtualFile virtualFile = documentManager.getFile(editor.getDocument());
        if (virtualFile instanceof LightVirtualFileBase) {
            return false;
        }
        return virtualFile != null && virtualFile.isValid();
    }
}
