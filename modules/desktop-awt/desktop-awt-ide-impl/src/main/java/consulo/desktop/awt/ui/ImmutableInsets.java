/*
 * Copyright 2013-2025 consulo.io
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
package consulo.desktop.awt.ui;

import consulo.ui.Rectangle2D;
import consulo.ui.Size2D;
import jakarta.annotation.Nonnull;

import java.awt.*;
import java.io.Serializable;

/**
 * @author UNV
 * @since 2025-06-10
 */
public record ImmutableInsets(int top, int right, int bottom, int left) implements Serializable {
    private static final long serialVersionUID = -2585229325627968686L;

    public ImmutableInsets(int all) {
        this(all, all, all, all);
    }

    public ImmutableInsets(int topBottom, int leftRight) {
        this(topBottom, leftRight, topBottom, leftRight);
    }

    public Size2D stepOut(@Nonnull Size2D size) {
        return new Size2D(
            size.width() + left() + right(),
            size.height() + top() + bottom()
        );
    }

    public Size2D stepIn(@Nonnull Size2D size) {
        return new Size2D(
            size.width() - left() - right(),
            size.height() - top() - bottom()
        );
    }

    public Rectangle2D stepOut(@Nonnull Rectangle2D rectangle) {
        return new Rectangle2D(
            rectangle.minPoint().translate(-left(), -top()),
            stepOut(rectangle.size())
        );
    }

    public Rectangle2D stepIn(@Nonnull Rectangle2D rectangle) {
        return new Rectangle2D(
            rectangle.minPoint().translate(left(), top()),
            stepIn(rectangle.size())
        );
    }

    public static ImmutableInsets top(int top) {
        return new ImmutableInsets(top, 0, 0, 0);
    }

    public static ImmutableInsets right(int right) {
        return new ImmutableInsets(0, right, 0, 0);
    }

    public static ImmutableInsets bottom(int bottom) {
        return new ImmutableInsets(0, 0, bottom, 0);
    }

    public static ImmutableInsets left(int left) {
        return new ImmutableInsets(0, 0, 0, left);
    }

    public static ImmutableInsets of(int all) {
        return new ImmutableInsets(all);
    }

    public static ImmutableInsets of(int topBottom, int leftRight) {
        return new ImmutableInsets(topBottom, leftRight);
    }

    @SuppressWarnings("UnnecessaryFullyQualifiedName")
    public static ImmutableInsets of(@Nonnull Insets insets) {
        return new ImmutableInsets(insets.top, insets.right, insets.bottom, insets.left);
    }
}
