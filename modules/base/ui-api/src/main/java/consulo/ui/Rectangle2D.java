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
 * @since 2017-09-25
 */
public record Rectangle2D(@Nonnull Point2D coordinate, @Nonnull Size2D size) implements Serializable, Cloneable {
    private static final long serialVersionUID = 4140523038283686399L;

    public Rectangle2D(int width, int height) {
        this(0, 0, width, height);
    }

    public Rectangle2D(int x, int y, int width, int height) {
        this(new Point2D(x, y), new Size2D(width, height));
    }

    public Rectangle2D(@Nonnull Rectangle2D rectangle2D) {
        this(rectangle2D.coordinate(), rectangle2D.size());
    }

    public boolean isEmpty() {
        return (getWidth() <= 0) || (getHeight() <= 0);
    }

    public int getHeight() {
        return size().height();
    }

    public int getWidth() {
        return size().width();
    }

    public int getX() {
        return coordinate().x();
    }

    public int getY() {
        return coordinate().y();
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Rectangle2D clone() {
        return new Rectangle2D(coordinate(), size());
    }
}
