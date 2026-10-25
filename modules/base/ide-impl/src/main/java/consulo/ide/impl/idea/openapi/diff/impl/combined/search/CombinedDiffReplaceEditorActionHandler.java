// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.openapi.diff.impl.combined.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.diff.impl.internal.combined.CombinedDiffBaseEditorWithSelectionHandler;
import consulo.diff.impl.internal.combined.CombinedDiffViewer;
import consulo.diff.impl.internal.combined.search.CombinedDiffSearchProvider;
import consulo.diff.internal.DiffImplUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import org.jspecify.annotations.Nullable;

@ExtensionImpl(id = "combinedDiffReplace", order = "first")
public class CombinedDiffReplaceEditorActionHandler extends CombinedDiffBaseEditorWithSelectionHandler {
    @Override
    public String getActionId() {
        return IdeActions.ACTION_REPLACE;
    }

    @RequiredUIAccess
    @Override
    protected void doExecute(CombinedDiffViewer combined, Editor editor, @Nullable Caret caret, DataContext dc) {
        if (DiffImplUtil.isEditable(editor)) {
            //open single editor replace, global replace not supported
            executeOriginal(editor, caret, dc);
            return;
        }

        Project project = dc.getData(Project.KEY);
        if (project == null) {
            return;
        }
        project.getInstance(CombinedDiffSearchProvider.class).installSearch(combined);
    }
}
