// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;

@ExtensionImpl(id = "combinedDiffRight", order = "last")
public class CombinedDiffEditorRightHandler extends CombinedDiffBaseEditorForEachCaretHandler {
    @Override
    public String getActionId() {
        return "EditorRight";
    }

    @RequiredUIAccess
    @Override
    protected void doExecute(CombinedDiffViewer combined, Editor editor, Caret caret, DataContext dc) {
        boolean isOnLastPosition = CombinedDiffCaretUtil.isOnLastVisibleColumn(caret, editor);
        if (CombinedDiffCaretUtil.isOnLastVisibleLine(caret) && combined.canGoNextBlock() && isOnLastPosition) {
            combined.moveCaretToNextBlock();
            return;
        }
        executeOriginal(editor, caret, dc);
        combined.scrollToCaret();
    }
}
