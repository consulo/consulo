// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.*;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.internal.CodeEditorInternalHelper;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ActionImpl(id = "EditorTextEnd")
public class TextEndAction extends TextComponentEditorAction {
    private static class Handler extends EditorActionHandler {
        @Override
        public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            editor.getCaretModel().removeSecondaryCarets();
            int offset = editor.getDocument().getTextLength();
            if (editor instanceof RealEditor) {
                editor.getCaretModel().moveToLogicalPosition(editor.offsetToLogicalPosition(offset).leanForward(true));
            }
            else {
                editor.getCaretModel().moveToOffset(offset);
            }
            editor.getSelectionModel().removeSelection();

            ScrollingModel scrollingModel = editor.getScrollingModel();
            scrollingModel.disableAnimation();
            scrollingModel.scrollToCaret(ScrollType.CENTER);
            scrollingModel.enableAnimation();

            Project project = dataContext.getData(Project.KEY);
            if (project != null) {
                CodeEditorInternalHelper.getInstance().includeCurrentCommandAsNavigation(project);
            }
        }
    }

    public TextEndAction() {
        super(CodeEditorLocalize.actionTextEndText(), new Handler());
    }
}
