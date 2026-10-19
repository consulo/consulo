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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.dependency.StyleSheet;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.ui.messageList.MessageListStyle;
import consulo.ui.style.ComponentColors;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.avatar.AvatarPresentation;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.image.WebImageUrl;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
public class WebMessageListImpl extends VaadinComponentDelegate<WebMessageListImpl.Vaadin> implements MessageList {
    @StyleSheet("/messageList/webMessageList.css")
    public class Vaadin extends com.vaadin.flow.component.messages.MessageList implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebMessageListImpl.this;
        }
    }

    private class Item implements MessageListItem {
        private final LocalizeValue myAuthor;
        private final com.vaadin.flow.component.messages.MessageListItem myVaadinItem;
        private @Nullable Image myAuthorImage;
        private MessageListItemStyle myStyle = MessageListItemStyle.OTHER;

        Item(LocalizeValue author, String text) {
            myAuthor = author;
            String name = author.get();
            myVaadinItem = new com.vaadin.flow.component.messages.MessageListItem(text, null, name);
            updateColorIndex();
            myVaadinItem.setUserAbbreviation(AvatarPresentation.initialsOf(name));
            if (name.isEmpty()) {
                myVaadinItem.addThemeNames("consulo-anonymous");
            }
        }

        @Override
        public LocalizeValue getAuthor() {
            return myAuthor;
        }

        @Override
        public String getText() {
            String text = myVaadinItem.getText();
            return text == null ? "" : text;
        }

        @RequiredUIAccess
        @Override
        public void setText(String text) {
            myVaadinItem.setText(text);
        }

        @RequiredUIAccess
        @Override
        public void appendText(String text) {
            myVaadinItem.appendText(text);
        }

        @Override
        public @Nullable Instant getTime() {
            return myVaadinItem.getTime();
        }

        @RequiredUIAccess
        @Override
        public void setTime(@Nullable Instant time) {
            myVaadinItem.setTime(time);
        }

        @Override
        public @Nullable Image getAuthorImage() {
            return myAuthorImage;
        }

        @RequiredUIAccess
        @Override
        public void setAuthorImage(@Nullable Image image) {
            myAuthorImage = image;
            myVaadinItem.setUserImage(image == null ? null : WebImageUrl.toURL(image));
            updateColorIndex();
        }

        private void updateColorIndex() {
            String name = myAuthor.get();
            myVaadinItem.setUserColorIndex(name.isEmpty() || myAuthorImage != null ? null : AvatarPresentation.colorIndexOf(name));
        }

        @Override
        public MessageListItemStyle getStyle() {
            return myStyle;
        }

        @RequiredUIAccess
        @Override
        public void setStyle(MessageListItemStyle style) {
            myVaadinItem.removeThemeNames(themeName(myStyle));
            myStyle = style;
            myVaadinItem.addThemeNames(themeName(style));
        }
    }

    private final List<Item> myItems = new ArrayList<>();

    public WebMessageListImpl() {
        Vaadin vaadin = toVaadinComponent();
        vaadin.getStyle().set("--consulo-message-error-foreground", WebColors.toStyleCssColor(ComponentColors.ERROR_FOREGROUND));
        vaadin.getStyle().set("--consulo-message-system-foreground", WebColors.toStyleCssColor(ComponentColors.INFO_FOREGROUND));
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    private static String themeName(MessageListItemStyle style) {
        return "consulo-" + style.name().toLowerCase(Locale.ROOT);
    }

    @RequiredUIAccess
    @Override
    public MessageListItem add(LocalizeValue author, String text) {
        Item item = new Item(author, text);
        item.setStyle(MessageListItemStyle.OTHER);
        myItems.add(item);
        toVaadinComponent().addItem(item.myVaadinItem);
        return item;
    }

    @RequiredUIAccess
    @Override
    public void remove(MessageListItem item) {
        if (myItems.remove(item)) {
            syncItems();
        }
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        myItems.clear();
        syncItems();
    }

    private void syncItems() {
        List<com.vaadin.flow.component.messages.MessageListItem> items = new ArrayList<>(myItems.size());
        for (Item item : myItems) {
            items.add(item.myVaadinItem);
        }
        toVaadinComponent().setItems(items);
    }

    @Override
    public List<MessageListItem> getItems() {
        return List.copyOf(myItems);
    }

    @RequiredUIAccess
    @Override
    public void setMarkdown(boolean markdown) {
        toVaadinComponent().setMarkdown(markdown);
    }

    @Override
    public boolean isMarkdown() {
        return toVaadinComponent().isMarkdown();
    }

    @RequiredUIAccess
    @Override
    public void setShowOwnAvatar(boolean showOwnAvatar) {
        toVaadinComponent().setClassName("consulo-show-own-avatar", showOwnAvatar);
    }

    @Override
    public boolean isShowOwnAvatar() {
        return toVaadinComponent().hasClassName("consulo-show-own-avatar");
    }

    @Override
    public void addStyle(MessageListStyle style) {
        if (style == MessageListStyle.COMPACT) {
            toVaadinComponent().addClassName("consulo-compact");
        }
    }
}
