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

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.layout.ScrollableLayout;
import org.jspecify.annotations.Nullable;

/**
 * Viewport access of a {@link ScrollableLayout}. All coordinates are in the space of the scrolled content.
 * <p>
 * The state is the last one known on the UI thread: a frontend which learns about scrolling asynchronously reports it
 * with a {@link ScrollableLayoutViewportEvent} once it arrives.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
public interface ScrollableLayoutInternal extends ScrollableLayout {
    static ScrollableLayoutInternal of(ScrollableLayout layout) {
        return (ScrollableLayoutInternal) layout;
    }

    @RequiredUIAccess
    Rectangle2D getViewRect();

    /**
     * The size of the scrolled content, in the coordinates of {@link #getViewRect()}. A content which scrolls itself,
     * such as a list, reports what it scrolls.
     */
    @RequiredUIAccess
    Size2D getContentSize();

    /**
     * Scrolls so that {@code position} becomes the top left corner of the view rectangle, clamped to the content.
     */
    @RequiredUIAccess
    void scrollTo(Point2D position);

    /**
     * Tracked descendants report their bounds with {@link #getBounds(Component)}, and moving or resizing one of
     * them fires a {@link ScrollableLayoutViewportEvent}.
     */
    @RequiredUIAccess
    void setBoundsTracked(Component descendant, boolean tracked);

    /**
     * @return the last known bounds of a tracked descendant, or {@code null} if it is not tracked or not measured yet
     */
    @RequiredUIAccess
    @Nullable Rectangle2D getBounds(Component descendant);

    /**
     * A component shown over the top of the visible part, staying in place while the content scrolls under it.
     */
    @RequiredUIAccess
    void setTopOverlay(@Nullable Component overlay);

    default Disposable addViewportListener(ComponentEventListener<ScrollableLayout, ScrollableLayoutViewportEvent> listener) {
        return addListener(ScrollableLayoutViewportEvent.class, listener);
    }
}
