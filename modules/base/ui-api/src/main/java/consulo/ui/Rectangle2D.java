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

    public boolean contains(int x, int y) {
        return (width() | height()) >= 0 && minX() <= x && minY() <= y && x <= maxX() && y <= maxY();
    }

    public boolean contains(Point2D point) {
        return contains(point.x(), point.y());
    }

    public boolean contains(int x, int y, int width, int height) {
        return (width() | height() | width | height) >= 0 && minX() <= x && minY() <= y && x + width <= maxX() && y + height <= maxY();
    }

    public boolean contains(Rectangle2D rectangle) {
        return contains(rectangle.minX(), rectangle.minY(), rectangle.width(), rectangle.height());
    }
}
