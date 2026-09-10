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
package consulo.desktop.awt.ui.impl;

import consulo.ui.Space;
import consulo.ui.ex.awt.JBUI;

import java.util.Map;

/**
 * What a step of the {@link Space} scale is worth on this frontend, in pixels of the display it ends up on.
 * <p/>
 * The table is unscaled, so the numbers read the way a designer says them, and the scale of the display is applied
 * on the way out - the same way this frontend already treats every other length.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public final class DesktopSpace {
    private static final Map<Space, Integer> ourPixels = Map.of(
        Space.NONE, 0,
        Space.X_SMALL, 2,
        Space.SMALL, 4,
        Space.MEDIUM, 6,
        Space.LARGE, 8,
        Space.X_LARGE, 16,
        Space.XX_LARGE, 24,
        Space.XXX_LARGE, 48
    );

    public static int toPixels(Space space) {
        return JBUI.scale(ourPixels.getOrDefault(space, 0));
    }

    private DesktopSpace() {
    }
}
