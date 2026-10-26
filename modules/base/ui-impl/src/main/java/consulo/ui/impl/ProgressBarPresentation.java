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

import consulo.ui.ProgressBarStyle;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleColorValue;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * What the tone and height styles of a progress bar mean, shared by the frontends so a bar looks alike on each.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
public final class ProgressBarPresentation {
    /** The fill of the tone among {@code styles}, or null for the frontend's own accent. */
    public static @Nullable StyleColorValue valueColor(Set<ProgressBarStyle> styles) {
        for (ProgressBarStyle style : styles) {
            switch (style) {
                case SUCCESS:
                    return ComponentColors.SUCCESS_BORDER;
                case WARNING:
                    return ComponentColors.WARNING_BORDER;
                case ERROR:
                    return ComponentColors.ERROR_BORDER;
                default:
                    break;
            }
        }
        return null;
    }

    /** The thickness in unscaled pixels the height among {@code styles} asks for, or 0 for the default line. */
    public static int barHeight(Set<ProgressBarStyle> styles) {
        for (ProgressBarStyle style : styles) {
            switch (style) {
                case HEIGHT_MEDIUM:
                    return 10;
                case HEIGHT_LARGE:
                    return 16;
                default:
                    break;
            }
        }
        return 0;
    }

    /** Adds {@code style} to {@code styles}, dropping the tone or height it replaces. */
    public static void add(Set<ProgressBarStyle> styles, ProgressBarStyle style) {
        styles.removeIf(each -> each.isTone() && style.isTone() || each.isHeight() && style.isHeight());
        styles.add(style);
    }

    private ProgressBarPresentation() {
    }
}
