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
package consulo.ui.messageList;

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * One message of a {@link MessageList}. Changes show at once, so an answer can grow while it arrives.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public interface MessageListItem {
    LocalizeValue getAuthor();

    String getText();

    /**
     * Shows the text alone, dropping the {@link #getParts() parts}.
     */
    @RequiredUIAccess
    void setText(String text);

    @RequiredUIAccess
    void appendText(String text);

    /**
     * The parts the message shows instead of its text, empty for a message of text alone.
     */
    List<MessageListItemPart> getParts();

    /**
     * Shows the parts one under another instead of the text: a text part renders the way the text of the message does,
     * a component part stands between them at the width of the text.
     */
    @RequiredUIAccess
    void setParts(List<MessageListItemPart> parts);

    @Nullable Instant getTime();

    @RequiredUIAccess
    void setTime(@Nullable Instant time);

    @Nullable Image getAuthorImage();

    /**
     * Without an image the author is shown by the initials of the name.
     */
    @RequiredUIAccess
    void setAuthorImage(@Nullable Image image);

    MessageListItemStyle getStyle();

    @RequiredUIAccess
    void setStyle(MessageListItemStyle style);
}
