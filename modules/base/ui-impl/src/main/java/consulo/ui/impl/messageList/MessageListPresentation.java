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

/**
 * What every frontend of a message list shows the same way: the time line and whether an author gets a picture.
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

    private MessageListPresentation() {
    }

    public static boolean showsAvatar(MessageListItemStyle style, boolean showOwnAvatar) {
        return switch (style) {
            case OTHER, ERROR -> true;
            case OWN -> showOwnAvatar;
            case SYSTEM -> false;
        };
    }
}
