// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollingModel;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.awt.Point;
import java.awt.datatransfer.StringSelection;

@ExtensionImpl
public final class DocRenderCopyHandler extends EditorActionHandler implements ExtensionEditorActionHandler {
    private EditorActionHandler myOriginalHandler;

    @Override
    protected void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        if (!editor.getSelectionModel().hasSelection(true)) {
            DocRenderer.EditorInlineHtmlPane pane = DocRenderSelectionManager.getPaneWithSelection(editor);
            if (pane != null) {
                String text = pane.getSelectedText();
                if (!StringUtil.isEmpty(text)) {
                    Point selectionPositionInEditor = pane.getSelectionPositionInEditor();
                    if (selectionPositionInEditor != null) {
                        CopyPasteManager.getInstance().setContents(new StringSelection(text));

                        ScrollingModel scrollingModel = editor.getScrollingModel();
                        if (!scrollingModel.getVisibleAreaOnScrollingFinished().contains(selectionPositionInEditor)) {
                            scrollingModel.scroll(0, selectionPositionInEditor.y - scrollingModel.getVisibleArea().height / 3);
                        }

                        return;
                    }
                }
            }
        }
        myOriginalHandler.execute(editor, caret, dataContext);
    }

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Override
    public String getActionId() {
        return IdeActions.ACTION_COPY;
    }
}
