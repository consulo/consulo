// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview.comment;

import com.intellij.collaboration.ui.util.VolatileImageBufferingPainter;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.paint.RectanglePainter;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Do not create directly - use {@link com.intellij.collaboration.ui.ClippingRoundedPanel}.
 * This panel clips the rounded corners of its children and background and allows the underlying background to show through.
 * This is achieved by painting to off-screen buffer first, clearing the corners and then painting said buffer to the graphics, which
 * is very ineffective.
 */
@ApiStatus.Internal
public final class RoundedPanel extends JPanel {
    private final int arcRadius;
    private final VolatileImageBufferingPainter bufferingPainter = new VolatileImageBufferingPainter(Transparency.TRANSLUCENT);
    private boolean fillBackground = false;

    @Obsolete
    public RoundedPanel(LayoutManager layout, int arcRadius) {
        super(layout);
        this.arcRadius = arcRadius;
        super.setOpaque(false);
        setCursor(Cursor.getDefaultCursor());
    }

    @Obsolete
    public RoundedPanel(LayoutManager layout) {
        this(layout, 8);
    }

    @Override
    public void setOpaque(boolean isOpaque) {
        // Disable opaque
    }

    @Override
    public void paint(Graphics g) {
        fillBackground = false;
        super.paint(g);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // will be called inside paint if the panel is not fully obscured by children
        fillBackground = true;
    }

    //TODO: optimize with clip
    @Override
    protected void paintChildren(Graphics g) {
        Rectangle componentBounds = new Rectangle(getSize());
        JBInsets.removeFrom(componentBounds, getInsets());
        Shape outsideShape = createOutsideShape(getSize(), componentBounds, arcRadius - 1);
        bufferingPainter.paintBuffered(g, getSize(), g2 -> paintAndSmooth(g2, componentBounds, outsideShape));
    }

    private void paintAndSmooth(Graphics2D g2, Rectangle componentBounds, Shape outsideShape) {
        paintBackground(g2, componentBounds);
        super.paintChildren(g2);
        clearArea(g2, outsideShape);
        super.paintBorder(g2);
    }

    private void paintBackground(Graphics2D g2, Shape area) {
        if (fillBackground && isBackgroundSet()) {
            g2.setColor(getBackground());
            Rectangle rect = area.getBounds();
            int arc = arcRadius * 2;
            RectanglePainter.FILL.paint(g2, rect.x, rect.y, rect.width, rect.height, arc);
        }
    }

    private void clearArea(Graphics2D g2, Shape area) {
        // AA disabled for now because it tremendously slows down the painting
        Composite composite = g2.getComposite();
        g2.setComposite(AlphaComposite.Clear);
        g2.fill(area);
        g2.setComposite(composite);
    }

    // border will be painted after children to avoid clipping
    @Override
    protected void paintBorder(Graphics g) {
    }

    // From Swing's point of view children are painted in a rectangular box,
    // so when a repaint happens on a child, this panel will not clip the corners.
    // This property causes repaint of a child to trigger repaint of this panel.
    @Override
    public boolean isPaintingOrigin() {
        return true;
    }

    @SuppressWarnings("UseDPIAwareInsets")
    private static Shape createOutsideShape(Dimension overallSize, Rectangle componentBounds, int arcRadius) {
        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        // inner
        var innerRect = componentBounds.getBounds2D();
        double arc = arcRadius * 2.0;
        path.append(new RoundRectangle2D.Double(innerRect.getX(), innerRect.getY(),
            innerRect.getWidth(), innerRect.getHeight(),
            arc, arc
        ), false);
        // outer
        Rectangle outerRect = new Rectangle(overallSize);
        JBInsets.addTo(outerRect, new Insets(1, 1, 1, 1));
        path.append(outerRect, false);
        return path;
    }
}
