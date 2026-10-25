/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.internal;

import consulo.ui.Rectangle2D;
import consulo.ui.event.ComponentEvent;
import consulo.ui.layout.ScrollableLayout;

/**
 * Fired when the visible part of a {@link ScrollableLayout} changes: it was scrolled, the viewport was resized, the
 * content changed its size, or a descendant tracked by {@link ScrollableLayoutInternal#setBoundsTracked} moved or
 * was resized.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public final class ScrollableLayoutViewportEvent extends ComponentEvent<ScrollableLayout> {
    private final Rectangle2D myViewRect;

    public ScrollableLayoutViewportEvent(ScrollableLayout component, Rectangle2D viewRect) {
        super(component);
        myViewRect = viewRect;
    }

    /**
     * @return the visible rectangle in the coordinates of the scrolled content
     */
    public Rectangle2D getViewRect() {
        return myViewRect;
    }
}
