// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.ui.annotation.RequiredUIAccess;

@ExtensionImpl(id = "combinedDiffPageDown", order = "last")
public class CombinedDiffEditorPageDownHandler extends CombinedDiffBaseEditorForEachCaretHandler {
    @Override
    public String getActionId() {
        return "EditorPageDown";
    }

    @RequiredUIAccess
    @Override
    protected void doExecute(CombinedDiffViewer combined, Editor editor, Caret caret, DataContext dc) {
        combined.moveCaretPageDown();
    }
}
