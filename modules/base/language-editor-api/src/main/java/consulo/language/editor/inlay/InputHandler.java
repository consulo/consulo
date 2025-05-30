// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inlay;

import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * Note to all API: you should not rely on the fact that if some method is called then
 * the mouse is inside the rectangle (0, 0)–(width, height).
 */
public interface InputHandler {
    /**
     * Called when user clicked on presentation.
     *
     * @param translated event point in coordinate system of associated presentation.
     */
    default void mouseClicked(MouseEvent event, Point translated) {
    }

    /**
     * Called when user presses on presentation.
     *
     * @param translated event point in coordinate system of associated presentation.
     */
    default void mousePressed(MouseEvent event, Point translated) {
    }

    /**
     * Called when the mouse button is released after pressing on presentation.
     *
     * @param translated event point in coordinate system of associated presentation.
     */
    default void mouseReleased(MouseEvent event, Point translated) {
    }

    /**
     * Called when user moves mouse in bounds of inlay.
     *
     * @param translated event point in coordinate system of associated presentation.
     */
    default void mouseMoved(MouseEvent event, Point translated) {
    }

    /**
     * Called when mouse leaves presentation.
     */
    default void mouseExited() {
    }

    /**
     * Allow change of inlay point if point in inlay model and real presentation differs.
     *
     * @param inlayPoint the original point in inlay coordinate space
     * @return possibly translated point in presentation coordinate space
     */
    default Point translatePoint(Point inlayPoint) {
        return inlayPoint;
    }
}
