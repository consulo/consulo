// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.diff;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.Set;

final class IconVisibilityController implements EditorMouseListener, EditorMouseMotionListener {

    private final @Nonnull Set<RangeHighlighterEx> highlighters;

    IconVisibilityController(@Nonnull Set<RangeHighlighterEx> highlighters) {
        this.highlighters = highlighters;
    }

    @Override
    @RequiredUIAccess
    public void mouseMoved(@Nonnull EditorMouseEvent e) {
        doUpdate(e.getEditor(), e.getLogicalPosition().line);
    }

    @Override
    public void mouseExited(@Nonnull EditorMouseEvent e) {
        doUpdate(e.getEditor(), -1);
    }

    private void doUpdate(@Nonnull Editor editor, int line) {
        for (RangeHighlighterEx highlighter : highlighters) {
            if (!(highlighter.getGutterIconRenderer() instanceof AddCommentGutterIconRenderer renderer)) {
                continue;
            }
            boolean visible = renderer.getLine() == line;
            boolean needUpdate = renderer.isIconVisible() != visible;
            if (needUpdate) {
                renderer.setIconVisible(visible);
                JComponent gutter = (JComponent) editor.getGutter();
                int y = editor.logicalPositionToXY(new LogicalPosition(renderer.getLine(), 0)).y;
                gutter.repaint(0, y, gutter.getWidth(), y + editor.getLineHeight());
            }
        }
    }
}
