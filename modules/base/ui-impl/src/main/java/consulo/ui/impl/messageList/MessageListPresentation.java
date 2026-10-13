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
package consulo.ui.impl.messageList;

import consulo.ui.messageList.MessageListItemStyle;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * What every frontend of a message list shows the same way: the time line, the initials of an author without a
 * picture and the colour of that author.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public final class MessageListPresentation {
    /**
     * The date and the time of a message, the way the web list writes it - {@code Oct 13, 2026, 12:06 PM}.
     */
    public static @Nullable String formatTime(@Nullable Instant time) {
        if (time == null) {
            return null;
        }
        return time.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT));
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
     * A hue in {@code [0, 1)} which stays the same for a name, so an author keeps a colour across messages and lists.
     */
    public static float hueOf(String name) {
        return Math.floorMod(name.hashCode(), 360) / 360f;
    }

    /**
     * The colour of the initials is drawn on the background of the list, so it has to stay readable on it.
     */
    public static float saturation(boolean dark) {
        return dark ? 0.5f : 0.75f;
    }

    public static float brightness(boolean dark) {
        return dark ? 0.85f : 0.6f;
    }

    private MessageListPresentation() {
    }

    public static boolean showsAvatar(MessageListItemStyle style) {
        return style == MessageListItemStyle.OTHER || style == MessageListItemStyle.ERROR;
    }
}
