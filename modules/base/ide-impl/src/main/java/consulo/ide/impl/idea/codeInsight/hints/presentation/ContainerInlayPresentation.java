// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.InlayPresentationFactory;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.GraphicsConfig;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class ContainerInlayPresentation extends StaticDelegatePresentation {
    private final InlayPresentationFactory.Padding padding;
    private final InlayPresentationFactory.RoundedCorners roundedCorners;
    private final ColorValue background;
    private final float backgroundAlpha;
    private boolean presentationIsUnderCursor = false;

    public ContainerInlayPresentation(InlayPresentation presentation,
                                      InlayPresentationFactory.Padding padding,
                                      InlayPresentationFactory.RoundedCorners roundedCorners,
                                      ColorValue background,
                                      float backgroundAlpha) {
        super(presentation);
        this.padding = padding;
        this.roundedCorners = roundedCorners;
        this.background = background;
        this.backgroundAlpha = backgroundAlpha;
    }

    @Override
    public int getWidth() {
        return leftInset() + presentation.getWidth() + rightInset();
    }

    @Override
    public int getHeight() {
        return topInset() + presentation.getHeight() + bottomInset();
    }

    @Override
    public void paint(Graphics2D g, TextAttributes attributes) {
        if (background != null) {
            Color preservedBackground = g.getBackground();
            g.setColor(TargetAWT.to(background));
            if (roundedCorners != null) {
                int arcWidth = roundedCorners.getArcWidth();
                int arcHeight = roundedCorners.getArcHeight();
                fillRoundedRectangle(g, getHeight(), getWidth(), arcWidth, arcHeight, backgroundAlpha);
            }
            else {
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            g.setColor(preservedBackground);
        }
        
        try (var ignored = GraphicsUtil.withTranslated(g, leftInset(), topInset())) {
            presentation.paint(g, attributes);
        }
    }

    @Override
    public void mouseClicked(MouseEvent event, Point translated) {
        handleMouse(translated, p -> presentation.mouseClicked(event, p));
    }

    @Override
    public void mouseMoved(MouseEvent event, Point translated) {
        handleMouse(translated, p -> presentation.mouseMoved(event, p));
    }

    @Override
    public void mouseExited() {
        try {
            presentation.mouseExited();
        }
        finally {
            presentationIsUnderCursor = false;
        }
    }

    private void handleMouse(Point original, Consumer<Point> action) {
        int x = original.x;
        int y = original.y;
        if (!isInInnerBounds(x, y)) {
            if (presentationIsUnderCursor) {
                presentation.mouseExited();
                presentationIsUnderCursor = false;
            }
            return;
        }
        Point translated = new Point(x - leftInset(), y - topInset());
        action.accept(translated);
    }

    private boolean isInInnerBounds(int x, int y) {
        return x >= leftInset() && x < leftInset() + presentation.getWidth()
            && y >= topInset() && y < topInset() + presentation.getHeight();
    }

    private int leftInset() {
        return padding != null ? padding.getLeft() : 0;
    }

    private int rightInset() {
        return padding != null ? padding.getRight() : 0;
    }

    private int topInset() {
        return padding != null ? padding.getTop() : 0;
    }

    private int bottomInset() {
        return padding != null ? padding.getBottom() : 0;
    }

    private void fillRoundedRectangle(Graphics2D g, int height, int width, int arcWidth, int arcHeight, float alpha) {
        GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
        GraphicsUtil.paintWithAlpha(g, alpha);
        g.fillRoundRect(0, 0, width, height, arcWidth, arcHeight);
        config.restore();
    }
}
