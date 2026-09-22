// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.codeEditor.Editor;
import consulo.codeEditor.RealEditor;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.cursor.Cursor;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Predicate;

public class WithCursorOnHoverPresentation extends StaticDelegatePresentation {
    public final Cursor cursor;
    private final Editor editor;
    private Predicate<MouseEvent> onHoverPredicate = e -> true;

    public WithCursorOnHoverPresentation(
        InlayPresentation presentation,
        Cursor cursor,
        Editor editor
    ) {
        super(presentation);
        this.cursor = cursor;
        this.editor = editor;
    }

    public WithCursorOnHoverPresentation(
        InlayPresentation presentation,
        Cursor cursor,
        Editor editor,
        Predicate<MouseEvent> onHoverPredicate
    ) {
        this(presentation, cursor, editor);
        this.onHoverPredicate = onHoverPredicate;
    }

    @Override
    @RequiredUIAccess
    public void mouseMoved(MouseEvent event, Point translated) {
        super.mouseMoved(event, translated);
        if (editor instanceof RealEditor realEditor) {
            realEditor.setCustomCursor(
                WithCursorOnHoverPresentation.class,
                onHoverPredicate.test(event) ? cursor : null
            );
        }
    }

    @Override
    @RequiredUIAccess
    public void mouseExited() {
        super.mouseExited();
        if (editor instanceof RealEditor realEditor) {
            realEditor.setCustomCursor(WithCursorOnHoverPresentation.class, null);
        }
    }
}
