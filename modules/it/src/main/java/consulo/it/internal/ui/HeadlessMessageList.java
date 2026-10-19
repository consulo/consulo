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
package consulo.it.internal.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemPart;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.ui.messageList.MessageListStyle;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless {@link MessageList}: keeps the messages, so a test reads what a conversation shows.
 *
 * @author VISTALL
 */
public class HeadlessMessageList extends HeadlessComponentBase implements MessageList {
    private static class Item implements MessageListItem {
        private final LocalizeValue myAuthor;
        private String myText;
        private List<MessageListItemPart> myParts = List.of();
        private @Nullable Instant myTime;
        private @Nullable Image myAuthorImage;
        private MessageListItemStyle myStyle = MessageListItemStyle.OTHER;

        Item(LocalizeValue author, String text) {
            myAuthor = author;
            myText = text;
        }

        @Override
        public LocalizeValue getAuthor() {
            return myAuthor;
        }

        @Override
        public String getText() {
            return myText;
        }

        @Override
        public void setText(String text) {
            myText = text;
            myParts = List.of();
        }

        @Override
        public List<MessageListItemPart> getParts() {
            return myParts;
        }

        @Override
        public void setParts(List<MessageListItemPart> parts) {
            myParts = List.copyOf(parts);
        }

        @Override
        public void appendText(String text) {
            myText += text;
        }

        @Override
        public @Nullable Instant getTime() {
            return myTime;
        }

        @Override
        public void setTime(@Nullable Instant time) {
            myTime = time;
        }

        @Override
        public @Nullable Image getAuthorImage() {
            return myAuthorImage;
        }

        @Override
        public void setAuthorImage(@Nullable Image image) {
            myAuthorImage = image;
        }

        @Override
        public MessageListItemStyle getStyle() {
            return myStyle;
        }

        @Override
        public void setStyle(MessageListItemStyle style) {
            myStyle = style;
        }
    }

    private final List<MessageListItem> myItems = new ArrayList<>();
    private boolean myMarkdown;
    private boolean myShowOwnAvatar;

    @Override
    public MessageListItem add(LocalizeValue author, String text) {
        Item item = new Item(author, text);
        myItems.add(item);
        return item;
    }

    @Override
    public void remove(MessageListItem item) {
        myItems.remove(item);
    }

    @Override
    public void clear() {
        myItems.clear();
    }

    @Override
    public List<MessageListItem> getItems() {
        return List.copyOf(myItems);
    }

    @Override
    public void setMarkdown(boolean markdown) {
        myMarkdown = markdown;
    }

    @Override
    public boolean isMarkdown() {
        return myMarkdown;
    }

    @Override
    public void setShowOwnAvatar(boolean showOwnAvatar) {
        myShowOwnAvatar = showOwnAvatar;
    }

    @Override
    public boolean isShowOwnAvatar() {
        return myShowOwnAvatar;
    }

    @Override
    public void addStyle(MessageListStyle style) {
    }
}
