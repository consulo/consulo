// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.openapi.diff.impl.combined.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.diff.impl.internal.combined.CombinedDiffBaseEditorWithSelectionHandler;
import consulo.diff.impl.internal.combined.CombinedDiffViewer;
import consulo.diff.impl.internal.combined.search.CombinedDiffSearchProvider;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import org.jspecify.annotations.Nullable;

@ExtensionImpl(id = "combinedDiffFind", order = "first")
public class CombinedDiffSearchEditorActionHandler extends CombinedDiffBaseEditorWithSelectionHandler {
    @Override
    public String getActionId() {
        return IdeActions.ACTION_FIND;
    }

    @RequiredUIAccess
    @Override
    protected void doExecute(CombinedDiffViewer combined, Editor editor, @Nullable Caret caret, DataContext dc) {
        Project project = dc.getData(Project.KEY);
        if (project == null) {
            return;
        }
        project.getInstance(CombinedDiffSearchProvider.class).installSearch(combined);
    }
}
