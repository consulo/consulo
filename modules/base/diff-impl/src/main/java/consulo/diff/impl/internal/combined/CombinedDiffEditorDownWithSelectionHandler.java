// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

@ExtensionImpl(id = "combinedDiffDownWithSelection", order = "last")
public class CombinedDiffEditorDownWithSelectionHandler extends CombinedDiffBaseEditorWithSelectionHandler {
    @Override
    public String getActionId() {
        return "EditorDownWithSelection";
    }

    @RequiredUIAccess
    @Override
    protected void doExecute(CombinedDiffViewer combined, Editor editor, @Nullable Caret caret, DataContext dc) {
        Caret currentCaret = editor.getCaretModel().getCurrentCaret();
        if (CombinedDiffCaretUtil.isOnLastVisibleLine(currentCaret) && CombinedDiffCaretUtil.isOnLastVisibleColumn(currentCaret, editor)
            && combined.canGoNextBlock()) {
            combined.moveCaretToNextBlock();
            return;
        }
        executeOriginal(editor, caret, dc);
        combined.scrollToCaret();
    }
}
