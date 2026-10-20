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
package consulo.ui.impl;

import consulo.ui.BadgeStyle;
import consulo.ui.LabelStyle;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleColorValue;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public final class BadgePresentation {
    public static final int ARC = 10;
    public static final int HORIZONTAL_PADDING = 6;
    public static final int VERTICAL_PADDING = 1;

    public static @Nullable BadgeStyle tone(Set<BadgeStyle> styles) {
        for (BadgeStyle style : styles) {
            if (style == BadgeStyle.SUCCESS || style == BadgeStyle.WARNING || style == BadgeStyle.ERROR || style == BadgeStyle.CONTRAST) {
                return style;
            }
        }
        return null;
    }

    public static StyleColorValue border(Set<BadgeStyle> styles) {
        BadgeStyle tone = tone(styles);
        if (tone == null) {
            return ComponentColors.FOCUS_COLOR;
        }
        return switch (tone) {
            case SUCCESS -> ComponentColors.SUCCESS_BORDER;
            case WARNING -> ComponentColors.WARNING_BORDER;
            case ERROR -> ComponentColors.ERROR_BORDER;
            default -> ComponentColors.TEXT_FOREGROUND;
        };
    }

    public static StyleColorValue background(Set<BadgeStyle> styles) {
        if (styles.contains(BadgeStyle.FILLED)) {
            return border(styles);
        }
        BadgeStyle tone = tone(styles);
        if (tone == null) {
            return ComponentColors.SELECTION_BACKGROUND;
        }
        return switch (tone) {
            case SUCCESS -> ComponentColors.TOAST_SUCCESS_BACKGROUND;
            case WARNING -> ComponentColors.TOAST_WARNING_BACKGROUND;
            case ERROR -> ComponentColors.TOAST_ERROR_BACKGROUND;
            default -> ComponentColors.TEXT_FOREGROUND;
        };
    }

    public static StyleColorValue foreground(Set<BadgeStyle> styles) {
        BadgeStyle tone = tone(styles);
        if (tone == BadgeStyle.CONTRAST || tone == null && styles.contains(BadgeStyle.FILLED)) {
            return ComponentColors.LAYOUT;
        }
        return ComponentColors.TEXT_FOREGROUND;
    }

    public static int fontPointDelta(Set<BadgeStyle> styles) {
        return styles.contains(BadgeStyle.SMALL) ? LabelStyle.FONT_SMALL.getFontPointDelta() : 0;
    }

    private BadgePresentation() {
    }
}
