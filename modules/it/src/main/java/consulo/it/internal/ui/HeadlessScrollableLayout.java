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
package consulo.it.internal.ui;

import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.ScrollableLayoutInternal;
import consulo.ui.layout.LayoutConstraint;
import org.jspecify.annotations.Nullable;

/**
 * Dummy-but-creatable headless {@link ScrollableLayout} over the single component it scrolls.
 *
 * @author VISTALL
 */
public class HeadlessScrollableLayout extends HeadlessLayoutBase<LayoutConstraint> implements ScrollableLayoutInternal {
    private Rectangle2D myViewRect = new Rectangle2D(0, 0);

    public HeadlessScrollableLayout(@Nullable Component component) {
        if (component != null) {
            add(component, LayoutConstraint.NONE);
        }
    }

    @RequiredUIAccess
    @Override
    public Rectangle2D getViewRect() {
        return myViewRect;
    }

    @RequiredUIAccess
    @Override
    public void scrollTo(Point2D position) {
        myViewRect = new Rectangle2D(position, myViewRect.size());
    }

    @RequiredUIAccess
    @Override
    public void setBoundsTracked(Component descendant, boolean tracked) {
    }

    @RequiredUIAccess
    @Override
    public @Nullable Rectangle2D getBounds(Component descendant) {
        return null;
    }

    @RequiredUIAccess
    @Override
    public void setTopOverlay(@Nullable Component overlay) {
    }
}
