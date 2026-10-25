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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.avatar.AvatarPresentation;
import consulo.ui.impl.messageList.MessageListPresentation;
import consulo.ui.impl.messageList.MessageListImages;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemPart;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.ui.messageList.MessageListStyle;
import consulo.ui.style.ComponentColors;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.image.WebImageUrl;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The messages are {@code vaadin-message} elements of a list of its own rather than a {@code vaadin-message-list}: that
 * one renders its messages from data alone, and a message here can hold components between the parts of its text. A
 * hidden {@code vaadin-message-list} stays inside, since it is the one loading the markdown element once it shows
 * markdown.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public class WebMessageListImpl extends VaadinComponentDelegate<WebMessageListImpl.Vaadin> implements MessageList {
    @StyleSheet("/messageList/webMessageList.css")
    public class Vaadin extends Div implements FromVaadinComponentWrapper {
        @Override
        public @Nullable Component toUIComponent() {
            return WebMessageListImpl.this;
        }
    }

    private class Item implements MessageListItem {
        private final LocalizeValue myAuthor;
        private final Element myMessage = new Element("vaadin-message");
        private final List<Element> myContent = new ArrayList<>();
        private String myText;
        private List<MessageListItemPart> myParts = List.of();
        private @Nullable Instant myTime;
        private @Nullable Image myAuthorImage;
        private MessageListItemStyle myStyle = MessageListItemStyle.OTHER;
        private @Nullable Component myActionsComponent;
        private @Nullable Element myActionsElement;

        Item(LocalizeValue author, String text) {
            myAuthor = author;
            myText = text;

            String name = author.get();
            myMessage.setAttribute("role", "listitem");
            myMessage.setProperty("userName", name);
            myMessage.setProperty("userAbbr", AvatarPresentation.initialsOf(name));
            if (name.isEmpty()) {
                myMessage.getThemeList().add("consulo-anonymous");
            }

            Element avatar = new Element("vaadin-avatar");
            avatar.setAttribute("slot", "avatar");
            myMessage.appendChild(avatar);

            updateColorIndex();
            updateContent();
        }

        void updateContent() {
            for (Element element : myContent) {
                element.removeFromParent();
            }
            myContent.clear();

            if (myParts.isEmpty()) {
                if (!myText.isEmpty()) {
                    myContent.add(createText(myText));
                }
            }
            else {
                for (MessageListItemPart part : myParts) {
                    String text = part.getText();
                    Component component = part.getComponent();
                    if (text != null) {
                        myContent.add(createText(text));
                    }
                    else if (component != null) {
                        myContent.add(TargetVaadin.to(component).getElement());
                    }
                }
            }

            for (Element element : myContent) {
                myMessage.appendChild(element);
            }
        }

        private Element createText(String text) {
            if (myMarkdown) {
                Element markdown = new Element("vaadin-markdown");
                markdown.setProperty("content", resolveImages(text));
                return markdown;
            }
            Element span = new Element("span");
            span.setText(text);
            return span;
        }

        @Override
        public LocalizeValue getAuthor() {
            return myAuthor;
        }

        @Override
        public String getText() {
            return myText;
        }

        @RequiredUIAccess
        @Override
        public void setText(String text) {
            myText = text;
            if (myParts.isEmpty() && myContent.size() == 1 && !text.isEmpty()) {
                Element element = myContent.get(0);
                if (myMarkdown) {
                    element.setProperty("content", resolveImages(text));
                }
                else {
                    element.setText(text);
                }
            }
            else {
                myParts = List.of();
                updateContent();
            }
            scrollToEndIfPinned();
        }

        @RequiredUIAccess
        @Override
        public void appendText(String text) {
            setText(myText + text);
        }

        @Override
        public List<MessageListItemPart> getParts() {
            return myParts;
        }

        @RequiredUIAccess
        @Override
        public void setParts(List<MessageListItemPart> parts) {
            myParts = List.copyOf(parts);
            updateContent();
        }

        @Override
        public @Nullable Instant getTime() {
            return myTime;
        }

        @RequiredUIAccess
        @Override
        public void setTime(@Nullable Instant time) {
            myTime = time;
            String formatted = MessageListPresentation.formatTime(time);
            if (formatted == null) {
                myMessage.removeProperty("time");
            }
            else {
                myMessage.setProperty("time", formatted);
            }
        }

        @Override
        public @Nullable Image getAuthorImage() {
            return myAuthorImage;
        }

        @RequiredUIAccess
        @Override
        public void setAuthorImage(@Nullable Image image) {
            myAuthorImage = image;
            String url = image == null ? null : WebImageUrl.toURL(image);
            if (url == null) {
                myMessage.removeProperty("userImg");
            }
            else {
                myMessage.setProperty("userImg", url);
            }
            updateColorIndex();
        }

        private void updateColorIndex() {
            String name = myAuthor.get();
            if (name.isEmpty() || myAuthorImage != null) {
                myMessage.removeProperty("userColorIndex");
            }
            else {
                myMessage.setProperty("userColorIndex", AvatarPresentation.colorIndexOf(name));
            }
        }

        @Override
        public MessageListItemStyle getStyle() {
            return myStyle;
        }

        @RequiredUIAccess
        @Override
        public void setStyle(MessageListItemStyle style) {
            myMessage.getThemeList().remove(themeName(myStyle));
            myStyle = style;
            myMessage.getThemeList().add(themeName(style));
        }

        @Override
        public @Nullable Component getActionsComponent() {
            return myActionsComponent;
        }

        @RequiredUIAccess
        @Override
        public void setActionsComponent(@Nullable Component component) {
            if (myActionsElement != null) {
                myActionsElement.removeFromParent();
                myActionsElement = null;
            }
            myActionsComponent = component;
            if (component == null) {
                myMessage.getStyle().remove("position");
                return;
            }

            Element element = TargetVaadin.to(component).getElement();
            element.getStyle()
                .set("position", "absolute")
                .set("width", "auto")
                .set("top", "0")
                .set("right", "4px");
            myMessage.getStyle().set("position", "relative");
            myMessage.appendChild(element);
            myActionsElement = element;
        }
    }

    private final List<Item> myItems = new ArrayList<>();
    private final com.vaadin.flow.component.messages.MessageList myMarkdownLoader = new com.vaadin.flow.component.messages.MessageList();
    private boolean myMarkdown;
    private @Nullable AsyncHtmlImageLoader myImageLoader;
    private final Map<String, Image> myImages = new HashMap<>();

    public WebMessageListImpl() {
        Vaadin vaadin = toVaadinComponent();
        vaadin.addClassName("consulo-message-list");
        vaadin.getElement().setAttribute("role", "list");
        vaadin.getStyle().set("--consulo-message-error-foreground", WebColors.toStyleCssColor(ComponentColors.ERROR_FOREGROUND));
        vaadin.getStyle().set("--consulo-message-system-foreground", WebColors.toStyleCssColor(ComponentColors.INFO_FOREGROUND));

        myMarkdownLoader.getStyle().set("display", "none");
        vaadin.add(myMarkdownLoader);
    }

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    private static String themeName(MessageListItemStyle style) {
        return "consulo-" + style.name().toLowerCase(Locale.ROOT);
    }

    private void scrollToEndIfPinned() {
        toVaadinComponent().getElement().executeJs(
            "const list = this; requestAnimationFrame(() => {"
                + " if (list.scrollHeight - list.clientHeight - list.scrollTop < list.clientHeight / 2) {"
                + " list.scrollTop = list.scrollHeight; } })"
        );
    }

    @RequiredUIAccess
    @Override
    public MessageListItem add(LocalizeValue author, String text) {
        Item item = new Item(author, text);
        item.setStyle(MessageListItemStyle.OTHER);
        myItems.add(item);
        toVaadinComponent().getElement().appendChild(item.myMessage);
        toVaadinComponent().getElement().executeJs("const list = this; requestAnimationFrame(() => list.scrollTop = list.scrollHeight)");
        return item;
    }

    @RequiredUIAccess
    @Override
    public void remove(MessageListItem item) {
        if (item instanceof Item own && myItems.remove(own)) {
            own.myMessage.removeFromParent();
        }
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        for (Item item : myItems) {
            item.myMessage.removeFromParent();
        }
        myItems.clear();
    }

    @Override
    public List<MessageListItem> getItems() {
        return List.copyOf(myItems);
    }

    @RequiredUIAccess
    @Override
    public void setMarkdown(boolean markdown) {
        if (myMarkdown == markdown) {
            return;
        }
        myMarkdown = markdown;
        myMarkdownLoader.setMarkdown(markdown);
        for (Item item : myItems) {
            item.updateContent();
        }
    }

    @Override
    public boolean isMarkdown() {
        return myMarkdown;
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

    @RequiredUIAccess
    @Override
    public void setImageLoader(@Nullable AsyncHtmlImageLoader imageLoader) {
        if (myImageLoader == imageLoader) {
            return;
        }
        myImageLoader = imageLoader;
        myImages.clear();
        if (myMarkdown) {
            for (Item item : myItems) {
                item.updateContent();
            }
        }
    }

    @Override
    public @Nullable AsyncHtmlImageLoader getImageLoader() {
        return myImageLoader;
    }

    private String resolveImages(String markdown) {
        AsyncHtmlImageLoader loader = myImageLoader;
        if (loader == null) {
            return markdown;
        }
        return MessageListImages.replaceMarkdownSources(markdown, src -> {
            if (src.startsWith("data:")) {
                return src;
            }
            Image image = myImages.computeIfAbsent(src, it -> Image.async(Image.empty(0), () -> loader.load(null, it)));
            String url = WebImageUrl.toURL(image);
            return url == null ? src : url;
        });
    }

    @Override
    public void addStyle(MessageListStyle style) {
        if (style == MessageListStyle.COMPACT) {
            toVaadinComponent().addClassName("consulo-compact");
        }
    }
}
