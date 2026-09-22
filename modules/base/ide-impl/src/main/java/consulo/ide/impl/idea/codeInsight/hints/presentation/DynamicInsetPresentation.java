// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.idea.codeInsight.hints.InsetValueProvider;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.GraphicsUtil;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

public class DynamicInsetPresentation extends StaticDelegatePresentation {
    private final InsetValueProvider valueProvider;
    private boolean isPresentationUnderCursor = false;

    public DynamicInsetPresentation(InlayPresentation presentation, InsetValueProvider valueProvider) {
        super(presentation);
        this.valueProvider = valueProvider;
    }

    private int getLeft() {
        return valueProvider.getLeft();
    }

    private int getRight() {
        return valueProvider.getRight();
    }

    private int getTop() {
        return valueProvider.getTop();
    }

    private int getDown() {
        return valueProvider.getDown();
    }

    @Override
    @RequiredUIAccess
    public int getWidth() {
        return presentation.getWidth() + getLeft() + getRight();
    }

    @Override
    @RequiredUIAccess
    public int getHeight() {
        return presentation.getHeight() + getTop() + getDown();
    }

    @Override
    @RequiredUIAccess
    public void paint(Graphics2D g, TextAttributes attributes) {
        try (var ignored = GraphicsUtil.withTranslated(g, getLeft(), getTop())) {
            presentation.paint(g, attributes);
        }
    }

    @RequiredUIAccess
    private void handleMouse(Point original, @RequiredUIAccess BiConsumer<InlayPresentation, Point> action) {
        int x = original.x;
        int y = original.y;
        int left = getLeft();
        int top = getTop();
        int width = presentation.getWidth();
        int height = presentation.getHeight();
        boolean cursorIsOutOfBounds = x < left || left + width <= x
            || y < top || top + height <= y;
        if (cursorIsOutOfBounds) {
            if (isPresentationUnderCursor) {
                presentation.mouseExited();
                isPresentationUnderCursor = false;
            }
            return;
        }
        Point translated = new Point(x - left, y - top);
        action.accept(presentation, translated);
    }

    @Override
    @RequiredUIAccess
    public void mouseClicked(MouseEvent event, Point translated) {
        handleMouse(translated, (p, pt) -> p.mouseClicked(event, pt));
    }

    @Override
    @RequiredUIAccess
    public void mouseMoved(MouseEvent event, Point translated) {
        handleMouse(translated, (p, pt) -> p.mouseMoved(event, pt));
    }

    @Override
    @RequiredUIAccess
    public void mouseExited() {
        presentation.mouseExited();
        isPresentationUnderCursor = false;
    }
}
