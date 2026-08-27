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
package consulo.ui;

import consulo.ui.event.ComponentEvent;
import org.jspecify.annotations.Nullable;

/**
 * A point of a component, for whatever is placed against a place inside it rather than against the component as a
 * whole - a popup opened at a line of an editor, for one.
 * <p/>
 * The accessors are named apart from those of a frontend's own relative point, which answers in the types of that
 * frontend and implements this to say the same thing in the types of the platform.
 *
 * @author VISTALL
 * @since 2026-08-09
 */
public interface RelativePoint2D {
    /**
     * Where the event happened inside its component. The position of a programmatic event is (0, 0) - a
     * caller that wants "no particular place" instead should check the event's input details first.
     */
    static RelativePoint2D of(ComponentEvent<?> event) {
        return of(event.getComponent(), event.getInputDetails().getPosition());
    }

    static RelativePoint2D of(Component component, Point2D point) {
        return new RelativePoint2D() {
            @Override
            public Component getUIComponent() {
                return component;
            }

            @Override
            public Point2D getUIPoint() {
                return point;
            }

            @Override
            public String toString() {
                return point + " on " + component;
            }
        };
    }

    /**
     * {@code null} when the point was measured against something this frontend has no component for.
     */
    @Nullable
    Component getUIComponent();

    Point2D getUIPoint();
}
