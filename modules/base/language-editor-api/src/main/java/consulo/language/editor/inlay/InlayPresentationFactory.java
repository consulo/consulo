// Copyright 2000-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inlay;

import org.jetbrains.annotations.Contract;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

public interface InlayPresentationFactory {
    /**
     * Text that can be used for elements that ARE valid syntax if they are pasted into file.
     */
    @Contract(pure = true)
    InlayPresentation text(String text);

    /**
     * Small text should be used for elements whose text is not valid syntax in the file.
     * Should be used with {@link #container} to be aligned and properly placed.
     */
    @Contract(pure = true)
    InlayPresentation smallText(String text);

    /**
     * Wraps an existing presentation with optional padding, rounded corners, background, and opacity.
     *
     * @param presentation    the base presentation
     * @param padding         space between the presentation and its border (may be null)
     * @param roundedCorners  arc dimensions for corners (may be null)
     * @param background      background color (may be null)
     * @param backgroundAlpha opacity from 0 to 1
     */
    @Contract(pure = true)
    InlayPresentation container(InlayPresentation presentation,
                                Padding padding,
                                RoundedCorners roundedCorners,
                                Color background,
                                float backgroundAlpha);

    /**
     * Renders an icon inline.
     */
    @Contract(pure = true)
    InlayPresentation icon(Icon icon);

    /**
     * Attaches mouse handlers to a presentation.
     *
     * @param base          the base presentation
     * @param clickListener click handler (may be null)
     * @param hoverListener hover handler (may be null)
     */
    @Contract(pure = true)
    InlayPresentation mouseHandling(InlayPresentation base,
                                    ClickListener clickListener,
                                    HoverListener hoverListener);

    /**
     * Small icon variant.
     */
    @Contract(pure = true)
    InlayPresentation smallScaledIcon(consulo.ui.image.Image icon);

    /**
     * Listener for hover events.
     */
    interface HoverListener {
        void onHover(MouseEvent event, Point translated);

        void onHoverFinished();
    }

    /**
     * Listener for click events.
     */
    @FunctionalInterface
    interface ClickListener {
        void onClick(MouseEvent event, Point translated);
    }

    /**
     * Padding values for container presentations.
     */
    final class Padding {
        public final int left;
        public final int right;
        public final int top;
        public final int bottom;

        public Padding(int left, int right, int top, int bottom) {
            this.left = left;
            this.right = right;
            this.top = top;
            this.bottom = bottom;
        }

        public int getLeft() {
            return left;
        }

        public int getRight() {
            return right;
        }

        public int getBottom() {
            return bottom;
        }

        public int getTop() {
            return top;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Padding)) {
                return false;
            }
            Padding that = (Padding) o;
            return left == that.left
                && right == that.right
                && top == that.top
                && bottom == that.bottom;
        }

        @Override
        public int hashCode() {
            int result = left;
            result = 31 * result + right;
            result = 31 * result + top;
            result = 31 * result + bottom;
            return result;
        }

        @Override
        public String toString() {
            return "Padding(left=" + left
                + ", right=" + right
                + ", top=" + top
                + ", bottom=" + bottom + ")";
        }
    }

    /**
     * Rounded-corner dimensions for container presentations.
     */
    final class RoundedCorners {
        public final int arcWidth;
        public final int arcHeight;

        public RoundedCorners(int arcWidth, int arcHeight) {
            this.arcWidth = arcWidth;
            this.arcHeight = arcHeight;
        }

        public int getArcWidth() {
            return arcWidth;
        }

        public int getArcHeight() {
            return arcHeight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof RoundedCorners)) {
                return false;
            }
            RoundedCorners that = (RoundedCorners) o;
            return arcWidth == that.arcWidth
                && arcHeight == that.arcHeight;
        }

        @Override
        public int hashCode() {
            int result = arcWidth;
            result = 31 * result + arcHeight;
            return result;
        }

        @Override
        public String toString() {
            return "RoundedCorners(arcWidth=" + arcWidth
                + ", arcHeight=" + arcHeight + ")";
        }
    }
}
