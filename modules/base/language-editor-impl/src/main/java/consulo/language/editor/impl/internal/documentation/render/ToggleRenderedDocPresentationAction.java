// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.platform.base.localize.ActionLocalize;
import org.jspecify.annotations.Nullable;

@ActionImpl(id = "ToggleRenderedDocPresentation")
public class ToggleRenderedDocPresentationAction extends EditorAction {
    public ToggleRenderedDocPresentationAction() {
        super(
            ActionLocalize.actionTogglerendereddocpresentationText(),
            ActionLocalize.actionTogglerendereddocpresentationDescription(),
            new Handler()
        );
    }

    private static final class Handler extends EditorActionHandler {
        @Override
        protected boolean isEnabledForCaret(Editor editor, Caret caret, DataContext dataContext) {
            return getItem(editor) != null;
        }

        @Override
        protected void doExecute(Editor editor, @Nullable Caret caret, DataContext dataContext) {
            DocRenderItem item = getItem(editor);
            if (item != null) {
                item.toggle();
            }
        }

        private static DocRenderItem getItem(Editor editor) {
            return DocRenderItemManager.getInstance().getItemAroundOffset(editor, editor.getCaretModel().getOffset());
        }
    }
}
