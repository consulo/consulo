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

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ToastStyle;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleColorValue;
import org.jspecify.annotations.Nullable;

/**
 * Style to fill, border and icon of a toast, shared by every frontend. A toast without a style is drawn plain.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
public final class ToastPresentation {
    /**
     * How much of its fill a {@link ToastStyle#GLASSY} toast keeps.
     */
    public static final float GLASSY_OPACITY = 0.8f;
    /**
     * The deviation of the gaussian blur behind a {@link ToastStyle#GLASSY} toast, in unscaled pixels.
     */
    public static final int GLASSY_BLUR = 3;

    public static @Nullable Image icon(@Nullable ToastStyle style) {
        if (style == null) {
            return null;
        }
        return switch (style) {
            case INFO -> PlatformIconGroup.generalBallooninformation();
            case SUCCESS -> PlatformIconGroup.statusSuccess();
            case WARNING -> PlatformIconGroup.generalBalloonwarning();
            case ERROR -> PlatformIconGroup.generalBalloonerror();
            case GLASSY -> null;
        };
    }

    public static StyleColorValue background(@Nullable ToastStyle style) {
        if (style == null) {
            return ComponentColors.LAYOUT;
        }
        return switch (style) {
            case INFO -> ComponentColors.TOAST_INFO_BACKGROUND;
            case SUCCESS -> ComponentColors.TOAST_SUCCESS_BACKGROUND;
            case WARNING -> ComponentColors.TOAST_WARNING_BACKGROUND;
            case ERROR -> ComponentColors.TOAST_ERROR_BACKGROUND;
            case GLASSY -> ComponentColors.LAYOUT;
        };
    }

    public static StyleColorValue border(@Nullable ToastStyle style) {
        if (style == null) {
            return ComponentColors.BORDER;
        }
        return switch (style) {
            case INFO -> ComponentColors.FOCUS_COLOR;
            case SUCCESS -> ComponentColors.SUCCESS_BORDER;
            case WARNING -> ComponentColors.WARNING_BORDER;
            case ERROR -> ComponentColors.ERROR_BORDER;
            case GLASSY -> ComponentColors.BORDER;
        };
    }

    private ToastPresentation() {
    }
}
