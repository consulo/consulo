// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;

@ExtensionImpl(id = "combinedDiffLeft", order = "last")
public class CombinedDiffEditorLeftHandler extends CombinedDiffBaseEditorForEachCaretHandler {
    @Override
    public String getActionId() {
        return "EditorLeft";
    }

    @RequiredUIAccess
    @Override
    protected void doExecute(CombinedDiffViewer combined, Editor editor, Caret caret, DataContext dc) {
        if (CombinedDiffCaretUtil.isOnFirstVisibleLine(caret) && CombinedDiffCaretUtil.isOnFirstVisibleColumn(caret) && combined.canGoPrevBlock()) {
            combined.moveCaretToPrevBlock();
            return;
        }
        executeOriginal(editor, caret, dc);
        combined.scrollToCaret();
    }
}
