// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inlay;

import consulo.colorScheme.TextAttributes;

import java.awt.*;

/**
 * Building block of inlay view. Its implementations are not expected to throw exceptions.
 * Most useful creation methods live in {@link InlayPresentationFactory}.
 * If you implement a new presentation, consider extending {@link BasePresentation}.
 */
public interface InlayPresentation extends InputHandler {
    /**
     * @return the width of this presentation in pixels.
     */
    int getWidth();

    /**
     * @return the height of this presentation in pixels.
     */
    int getHeight();

    /**
     * Renders the inlay.
     *
     * @param g          graphics to draw on; valid drawing area is (0,0)–(width−1,height−1)
     * @param attributes text attributes to use when painting
     */
    void paint(Graphics2D g, TextAttributes attributes);

    /**
     * Notify listeners that the size changed, causing a partial repaint.
     *
     * @param previous the old size
     * @param current  the new size
     */
    void fireSizeChanged(Dimension previous, Dimension current);

    /**
     * Notify listeners that the content changed in the given area.
     *
     * @param area region (in this presentation’s coordinate space) that changed
     */
    void fireContentChanged(Rectangle area);

    /**
     * Register a listener to presentation events.
     */
    void addListener(PresentationListener listener);

    /**
     * Unregister a previously added listener.
     */
    void removeListener(PresentationListener listener);

    /**
     * Called when a new presentation is collected at a place where an old one exists.
     * Should not fire events (use an invalidation flag instead) to avoid event spam.
     * By default presentations are stateless and this returns true.
     *
     * @param previousPresentation the previous presentation
     * @return true if this presentation should be reused, false to replace it
     */
    default boolean updateState(InlayPresentation previousPresentation) {
        return true;
    }

    /**
     * For testing purposes only.
     */
    @Override
    String toString();
}
