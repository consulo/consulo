// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.impl.internal.action;

import consulo.application.util.registry.Registry;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.internal.EditorInternalUtil;
import consulo.dataContext.DataContext;
import jakarta.annotation.Nonnull;

final class MoveCaretUpOrDownHandler extends EditorActionHandler.ForEachCaret {
    enum Direction {
        UP,
        DOWN
    }

    private final @Nonnull Direction myDirection;

    MoveCaretUpOrDownHandler(@Nonnull Direction direction) {
        myDirection = direction;
    }

    @Override
    public void doExecute(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
        Runnable runnable = () -> {
            if (caret.hasSelection() && (!(editor instanceof EditorEx) || !((EditorEx) editor).isStickySelection()) &&
                !Registry.is("editor.action.caretMovement.UpDownIgnoreSelectionBoundaries", false)) {
                int targetOffset = myDirection == Direction.DOWN ? caret.getSelectionEnd()
                    : caret.getSelectionStart();
                caret.moveToOffset(targetOffset);
            }

            int lineShift = myDirection == Direction.DOWN ? 1 : -1;
            caret.moveCaretRelatively(0, lineShift, false, caret == editor.getCaretModel().getPrimaryCaret());
        };

        EditorInternalUtil.runWithAnimationDisabled(editor, runnable);
    }

    @Override
    public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
        return !editor.isOneLineMode();
    }
}
