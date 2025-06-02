// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.popup;

import consulo.ui.ex.awt.JBUIScale;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.awt.geom.GeneralPath;

/**
 * @author UNV
 * @since 2025-06-01
 */
public class BalloonShaper {
    private final GeneralPath myPath = new GeneralPath();
    private final Rectangle myBodyBounds;
    private final Point myTargetPoint;
    private final int myBorderRadius;

    public BalloonShaper(Rectangle bodyBounds, Point targetPoint, int borderRadius) {
        myTargetPoint = targetPoint;
        myBodyBounds = bodyBounds;
        myBorderRadius = borderRadius;
        start(targetPoint);
    }

    private void start(Point start) {
        myPath.moveTo(start.x, start.y);
    }

    public int getX() {
        return (int)myPath.getCurrentPoint().getX();
    }

    public int getY() {
        return (int)myPath.getCurrentPoint().getY();
    }

    @Nonnull
    public BalloonShaper roundUpRight() {
        int x = getX(), y = getY(), r = myBorderRadius;
        myPath.quadTo(x, y - r, x + r, y - r);
        return this;
    }

    @Nonnull
    public BalloonShaper roundRightDown() {
        int x = getX(), y = getY(), r = myBorderRadius;
        myPath.quadTo(x + r, y, x + r, y + r);
        return this;
    }

    @Nonnull
    public BalloonShaper roundLeftUp() {
        int x = getX(), y = getY(), r = myBorderRadius;
        myPath.quadTo(x - r, y, x - r, y - r);
        return this;
    }

    @Nonnull
    public BalloonShaper roundLeftDown() {
        int x = getX(), y = getY(), r = myBorderRadius;
        myPath.quadTo(x, y + r, x - r, y + r);
        return this;
    }

    public BalloonShaper lineTo(int x, int y) {
        myPath.lineTo(x, y);
        return this;
    }

    public BalloonShaper horLineTo(int x) {
        myPath.lineTo(x, getY());
        return this;
    }

    public BalloonShaper vertLineTo(int y) {
        myPath.lineTo(getX(), y);
        return this;
    }

    public BalloonShaper lineToTargetPoint() {
        return lineTo((int)myTargetPoint.getX(), (int)myTargetPoint.getY());
    }

    @Nonnull
    public BalloonShaper toRightCurve() {
        return horLineTo((int)myBodyBounds.getMaxX() - myBorderRadius - JBUIScale.scale(1));
    }

    @Nonnull
    public BalloonShaper toBottomCurve() {
        return vertLineTo((int)myBodyBounds.getMaxY() - myBorderRadius - JBUIScale.scale(1));
    }

    @Nonnull
    public BalloonShaper toLeftCurve() {
        return horLineTo((int)myBodyBounds.getMinX() + myBorderRadius);
    }

    @Nonnull
    public BalloonShaper toTopCurve() {
        return vertLineTo((int)myBodyBounds.getMinY() + myBorderRadius);
    }

    public Shape close() {
        myPath.closePath();
        return myPath;
    }
}
