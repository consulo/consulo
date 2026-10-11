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
package consulo.ui.impl.font;

import consulo.ui.TreeStyle;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public final class FontSizeLadder {
    private FontSizeLadder() {
    }

    public static int pointDelta(TreeStyle style) {
        return switch (style) {
            case FONT_XX_SMALL -> -3;
            case FONT_X_SMALL -> -2;
            case FONT_SMALL -> -1;
            case FONT_LARGE -> 1;
            case FONT_X_LARGE -> 3;
            case FONT_XX_LARGE -> 5;
            case FONT_MEDIUM, TRANSPARENT_BACKGROUND -> 0;
        };
    }
}
