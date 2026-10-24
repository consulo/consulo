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
package consulo.ui.impl.avatar;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.AvatarStyle;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * What every frontend of an avatar shows the same way: its size, the initials of a name without a picture and the
 * colour of that name.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class AvatarPresentation {
    /**
     * The number of user colours of the web theme an avatar picks from.
     */
    public static final int COLOR_COUNT = 7;

    /**
     * The width in pixels of the border an outline style draws in place of the plain one.
     */
    public static final int OUTLINE_WIDTH = 2;

    private AvatarPresentation() {
    }

    /**
     * The size in pixels, matching what the web theme computes for the same style.
     */
    public static int sizeOf(@Nullable AvatarStyle style) {
        if (style == null) {
            return 32;
        }
        return switch (style) {
            case X_SMALL -> 24;
            case SMALL -> 28;
            case LARGE -> 34;
            case X_LARGE -> 40;
            case OUTLINE_SUCCESS, OUTLINE_WARNING, OUTLINE_NEUTRAL -> 32;
        };
    }

    public static boolean isOutline(AvatarStyle style) {
        return outlineColorOf(style) != null;
    }

    /**
     * The colour of the border an outline style draws instead of the plain one, {@code null} for a size style.
     */
    public static @Nullable ComponentColors outlineColorOf(@Nullable AvatarStyle style) {
        if (style == null) {
            return null;
        }
        return switch (style) {
            case OUTLINE_SUCCESS -> ComponentColors.AVATAR_OUTLINE_SUCCESS;
            case OUTLINE_WARNING -> ComponentColors.AVATAR_OUTLINE_WARNING;
            case OUTLINE_NEUTRAL -> ComponentColors.AVATAR_OUTLINE_NEUTRAL;
            case X_SMALL, SMALL, LARGE, X_LARGE -> null;
        };
    }

    /**
     * The picture to draw: the given image, the default person picture for a nameless avatar, or {@code null} when the
     * initials of the name are drawn.
     */
    public static @Nullable Image imageOf(String name, @Nullable Image image) {
        if (image != null) {
            return image;
        }
        return name.isBlank() ? PlatformIconGroup.actionsLoginavatar() : null;
    }

    /**
     * Up to two letters - the first letter or digit of each of the first two words which have one.
     */
    public static String initialsOf(String name) {
        StringBuilder builder = new StringBuilder();
        for (String word : name.trim().split("\\s+")) {
            if (builder.codePointCount(0, builder.length()) >= 2) {
                break;
            }
            word.codePoints().filter(Character::isLetterOrDigit).findFirst().ifPresent(builder::appendCodePoint);
        }
        return builder.toString().toUpperCase(Locale.ROOT);
    }

    /**
     * The user colour of the web theme which stays the same for a name.
     */
    public static int colorIndexOf(String name) {
        return Math.floorMod(name.hashCode(), COLOR_COUNT);
    }

    /**
     * A hue in {@code [0, 1)} which stays the same for a name, so a person keeps a colour across messages and lists.
     */
    public static float hueOf(String name) {
        return Math.floorMod(name.hashCode(), 360) / 360f;
    }

    /**
     * The colour of the initials is drawn on the background of the avatar, so it has to stay readable on it.
     */
    public static float saturation(boolean dark) {
        return dark ? 0.5f : 0.75f;
    }

    public static float brightness(boolean dark) {
        return dark ? 0.85f : 0.6f;
    }
}
