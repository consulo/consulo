// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.InlayPresentation;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

public class InsetPresentation extends StaticDelegatePresentation {
    private final int left;
    private final int right;
    private final int top;
    private final int down;
    private boolean isPresentationUnderCursor = false;

    public InsetPresentation(InlayPresentation presentation, int left, int right, int top, int down) {
        super(presentation);
        this.left = left;
        this.right = right;
        this.top = top;
        this.down = down;
    }

    @Override
    public int getWidth() {
        return presentation.getWidth() + left + right;
    }

    @Override
    public int getHeight() {
        return presentation.getHeight() + top + down;
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        g.translate(left, top);
        try {
            presentation.paint(g, attributes);
        }
        finally {
            g.translate(-left, -top);
        }
    }

    private void handleMouse(Point original, BiConsumer<InlayPresentation, Point> action) {
        int x = original.x;
        int y = original.y;
        boolean cursorIsOutOfBounds =
            x < left || x >= left + presentation.getWidth() ||
                y < top || y >= top + presentation.getHeight();

        if (cursorIsOutOfBounds) {
            if (isPresentationUnderCursor) {
                presentation.mouseExited();
                isPresentationUnderCursor = false;
            }
            return;
        }

        Point translated = new Point(x - left, y - top);
        action.accept(presentation, translated);
        isPresentationUnderCursor = true;
    }

    @Override
    public void mouseClicked(MouseEvent event, Point translated) {
        handleMouse(translated, (p, point) -> p.mouseClicked(event, point));
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        handleMouse(translated, (p, point) -> p.mouseMoved(event, point));
    }

    @Override
    public void mouseExited() {
        presentation.mouseExited();
        isPresentationUnderCursor = false;
    }
}
