// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

final class OrientableScrollablePanel extends JPanel implements Scrollable {
    private final int orientation;
    private int verticalUnit = 1;
    private int horizontalUnit = 1;

    OrientableScrollablePanel(int orientation, @Nullable LayoutManager layout) {
        super(layout);
        if (orientation != SwingConstants.VERTICAL && orientation != SwingConstants.HORIZONTAL) {
            throw new IllegalStateException(
                "SwingConstants.VERTICAL or SwingConstants.HORIZONTAL is expected for orientation, got " + orientation
            );
        }
        this.orientation = orientation;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        FontMetrics fontMetrics = getFontMetrics(getFont());
        verticalUnit = fontMetrics.getMaxAscent() + fontMetrics.getMaxDescent();
        horizontalUnit = fontMetrics.charWidth('W');
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.HORIZONTAL ? horizontalUnit : verticalUnit;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.HORIZONTAL ? visibleRect.width : visibleRect.height;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return orientation == SwingConstants.VERTICAL;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return orientation == SwingConstants.HORIZONTAL;
    }
}
