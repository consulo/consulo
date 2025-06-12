/*
 * Copyright 2013-2017 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.ui;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * @author VISTALL
 * @author UNV
 * @since 2017-09-25
 */
public record Rectangle2D(@Nonnull Point2D minPoint, @Nonnull Size2D size) implements Serializable {
    private static final long serialVersionUID = 4140523038283686399L;

    public Rectangle2D(int width, int height) {
        this(0, 0, width, height);
    }

    public Rectangle2D(int x, int y, int width, int height) {
        this(new Point2D(x, y), new Size2D(width, height));
    }

    public Rectangle2D(@Nonnull Size2D size) {
        this(new Point2D(), size);
    }

    public int minX() {
        return minPoint.x();
    }

    public int minY() {
        return minPoint.y();
    }

    public int maxX() {
        return minPoint.x() + size.width();
    }

    public int maxY() {
        return minPoint.y() + size.height();
    }

    public int width() {
        return size.width();
    }

    public int height() {
        return size.height();
    }

    public Point2D maxPoint() {
        return new Point2D(maxX(), maxY());
    }

    public boolean isEmpty() {
        return size.isEmpty();
    }

    /**
     * Checks whether or not this {@code Rectangle2D} contains the specified {@code Point2D}.
     *
     * @param point the {@code Point2D} to test.
     * @return  {@code true} if the specified {@code Point} is inside this {@code Rectangle2D};
     *          {@code false} otherwise.
     */
    public boolean contains(Point2D point) {
        return (width() | height()) >= 0 && minX() <= point.x() && minY() <= point.y() && point.x() <= maxX() && point.y() <= maxY();
    }

    /**
     * Checks whether or not this {@code Rectangle2D} entirely contains the specified {@code Rectangle2D}.
     *
     * @param rectangle the specified {@code Rectangle2D}.
     * @return {@code true} if the {@code Rectangle2D} is contained entirely inside this {@code Rectangle2D};
     *         {@code false} otherwise.
     */
    public boolean contains(Rectangle2D rectangle) {
        return (width() | height() | rectangle.width() | rectangle.height()) >= 0
            && minX() <= rectangle.minX() && minY() <= rectangle.minY()
            && rectangle.maxX() <= maxX() && rectangle.maxY() <= maxY();
    }

    /**
     * Determines whether or not this {@code Rectangle2D} and the specified {@code Rectangle2D} intersect.
     * Two rectangles intersect if their intersection is nonempty.
     *
     * @param rectangle the specified {@code Rectangle2D}.
     * @return {@code true} if the specified {@code Rectangle2D} and this {@code Rectangle2D} intersect;
     *         {@code false} otherwise.
     */
    public boolean intersects(Rectangle2D rectangle) {
        return !isEmpty() && !rectangle.isEmpty()
            && minX() < rectangle.maxX() && rectangle.minX() < maxX()
            && minY() < rectangle.maxY() && rectangle.minY() < maxY();
    }

    /**
     * Computes the intersection of this {@code Rectangle2D} with the specified {@code Rectangle2D}.
     * Returns a new {@code Rectangle2D} that represents the intersection of the two rectangles.
     * If the two rectangles do not intersect, the result will be an empty rectangle.
     *
     * @param rectangle the specified {@code Rectangle2D}.
     * @return the largest {@code Rectangle2D} contained in both the specified {@code Rectangle2D} and in this {@code Rectangle2D};
     *         or if the rectangles do not intersect, an empty rectangle.
     */
    public Rectangle2D intersection(Rectangle2D rectangle) {
        int x1 = Math.max(minX(), rectangle.minX());
        int y1 = Math.max(minY(), rectangle.minY());
        int x2 = Math.min(maxX(), rectangle.maxX());
        int y2 = Math.min(maxY(), rectangle.maxY());
        return new Rectangle2D(x1, y1, x2 - x1, y2 - y1);
    }

    public Rectangle2D translate(int dx, int dy) {
        return new Rectangle2D(minPoint().translate(dx, dy), size());
    }

    public Rectangle2D translateToFit(Rectangle2D rectangle) {
        int x = minX();
        if (maxX() > rectangle.maxX()) {
            x = rectangle.maxX() - width();
        }
        x = Math.max(x, rectangle.minX());

        int y = minY();
        if (maxY() > rectangle.maxY()) {
            y = rectangle.maxY() - height();
        }
        y = Math.max(y, rectangle.minY());

        return new Rectangle2D(new Point2D(x, y), size());
    }
}
