// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.inlay;

import consulo.ui.annotation.RequiredUIAccess;

import java.awt.*;

/**
 * Listener for presentation events.
 */
public interface PresentationListener {
    /**
     * Called when the content of a presentation changes.
     *
     * @param area region (in this presentation’s coordinate space) that changed
     */
    @RequiredUIAccess
    void contentChanged(Rectangle area);

    /**
     * Called when the size of a presentation changes.
     *
     * @param previous the old size
     * @param current  the new size
     */
    @RequiredUIAccess
    void sizeChanged(Dimension previous, Dimension current);
}
