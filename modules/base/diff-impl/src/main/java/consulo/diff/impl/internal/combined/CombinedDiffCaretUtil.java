// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.document.Document;

final class CombinedDiffCaretUtil {
    private CombinedDiffCaretUtil() {
    }

    static boolean isOnFirstVisibleLine(Caret caret) {
        return caret.getVisualPosition().line == 0;
    }

    static boolean isOnFirstVisibleColumn(Caret caret) {
        return caret.getVisualPosition().column == 0;
    }

    static boolean isOnLastVisibleLine(Caret caret) {
        Editor editor = caret.getEditor();
        int lastVisualLine = editor.offsetToVisualPosition(editor.getDocument().getTextLength()).line;
        return caret.getVisualPosition().line == lastVisualLine;
    }

    static boolean isOnLastVisibleColumn(Caret caret, Editor editor) {
        Document document = editor.getDocument();
        int offset = caret.getOffset();
        return offset == document.getLineEndOffset(document.getLineNumber(offset));
    }
}
