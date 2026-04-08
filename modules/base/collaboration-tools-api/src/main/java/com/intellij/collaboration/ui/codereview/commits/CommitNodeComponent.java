// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.commits;

import consulo.ui.ex.awt.JBDimension;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.util.MacUIUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

@ApiStatus.Internal
public class CommitNodeComponent extends JComponent {
    private Type type = Type.SINGLE;

    public CommitNodeComponent() {
        setOpaque(false);
    }

    public @Nonnull Type getType() {
        return type;
    }

    public void setType(@Nonnull Type type) {
        this.type = type;
    }

    @Override
    public Dimension getPreferredSize() {
        return new JBDimension(
            (int) PaintParameters.getElementWidth(PaintParameters.ROW_HEIGHT),
            PaintParameters.ROW_HEIGHT
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        Rectangle rect = new Rectangle(getSize());
        JBInsets.removeFrom(rect, getInsets());

        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(
            RenderingHints.KEY_STROKE_CONTROL,
            MacUIUtil.USE_QUARTZ ? RenderingHints.VALUE_STROKE_PURE : RenderingHints.VALUE_STROKE_NORMALIZE
        );

        if (isOpaque()) {
            g2.setColor(getBackground());
            g2.fill(new Rectangle2D.Float(rect.x, rect.y, rect.width, rect.height));
        }

        g2.setColor(getForeground());
        drawNode(g2, rect);
        if (type == Type.LAST || type == Type.MIDDLE) {
            drawEdgeUp(g2, rect);
        }
        if (type == Type.FIRST || type == Type.MIDDLE) {
            drawEdgeDown(g2, rect);
        }
    }

    private void drawNode(Graphics2D g, Rectangle rect) {
        int radius = calcRadius(rect);
        Ellipse2D.Double circle = new Ellipse2D.Double(
            rect.getCenterX() - radius, rect.getCenterY() - radius, radius * 2.0, radius * 2.0);
        g.fill(circle);
    }

    protected int calcRadius(Rectangle rect) {
        return (int) PaintParameters.getCircleRadius(rect.height);
    }

    private void drawEdgeUp(Graphics2D g, Rectangle rect) {
        double y1 = 0.0;
        double y2 = rect.getCenterY();
        drawEdge(g, rect, y1, y2);
    }

    private void drawEdgeDown(Graphics2D g, Rectangle rect) {
        double y1 = rect.getCenterY();
        double y2 = rect.getMaxY();
        drawEdge(g, rect, y1, y2);
    }

    private void drawEdge(Graphics2D g, Rectangle rect, double y1, double y2) {
        double x = rect.getCenterX();
        float width = calcLineThickness(rect);
        Rectangle2D.Double line = new Rectangle2D.Double(x - width / 2, y1 - 0.5, width, y1 + y2 + 0.5);
        g.fill(line);
    }

    protected float calcLineThickness(Rectangle rect) {
        return PaintParameters.getLineThickness(rect.height);
    }

    public enum Type {
        SINGLE,
        FIRST,
        MIDDLE,
        LAST
    }

    public static @Nonnull Type typeForListItem(int itemIndex, int listSize) {
        if (listSize <= 1) {
            return Type.SINGLE;
        }
        if (itemIndex == 0) {
            return Type.FIRST;
        }
        if (itemIndex == listSize - 1) {
            return Type.LAST;
        }
        return Type.MIDDLE;
    }
}
