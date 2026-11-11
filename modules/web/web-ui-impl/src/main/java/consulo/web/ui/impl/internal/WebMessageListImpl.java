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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.event.details.InputDetails;
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
import consulo.web.ui.impl.internal.base.WebInputDetails;
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
        private boolean myWriting;
        private LocalizeValue myWritingStatus = LocalizeValue.empty();
        private @Nullable Element myWritingElement;
        private @Nullable Element myWritingStatusElement;
        /**
         * Whether the message is in the page - only the newest ones are, and the earlier ones once scrolled to.
         */
        private boolean myAttached;

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

            // the indicator stays after the text the message grows by
            Element writing = myWritingElement;
            if (writing != null) {
                writing.removeFromParent();
                myMessage.appendChild(writing);
            }
        }

        @Override
        public boolean isWriting() {
            return myWriting;
        }

        /**
         * Three dots pulse one after another while the message is written - an animated svg of its own, in the colour of
         * the text around it.
         */
        @RequiredUIAccess
        @Override
        public void setWriting(boolean writing) {
            if (myWriting == writing) {
                return;
            }
            myWriting = writing;
            if (!writing) {
                Element element = myWritingElement;
                myWritingElement = null;
                myWritingStatusElement = null;
                if (element != null) {
                    element.removeFromParent();
                }
                return;
            }
            Element element = new Element("div");
            element.getClassList().add("consulo-message-writing");
            Element dots = new Element("span");
            dots.getClassList().add("consulo-message-writing-dots");
            dots.setProperty("innerHTML", WRITING_SVG);
            element.appendChild(dots);
            Element status = new Element("span");
            status.getClassList().add("consulo-message-writing-status");
            status.setText(myWritingStatus.get());
            element.appendChild(status);
            myWritingElement = element;
            myWritingStatusElement = status;
            myMessage.appendChild(element);
        }

        @Override
        public LocalizeValue getWritingStatus() {
            return myWritingStatus;
        }

        @RequiredUIAccess
        @Override
        public void setWritingStatus(LocalizeValue status) {
            myWritingStatus = status;
            Element element = myWritingStatusElement;
            if (element != null) {
                element.setText(status.get());
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

    /**
     * Three dots fading in one after another, animated by the svg itself.
     */
    private static final String WRITING_SVG = writingSvg();

    private static String writingSvg() {
        StringBuilder svg = new StringBuilder("<svg xmlns='http://www.w3.org/2000/svg' width='24' height='8' viewBox='0 0 24 8' fill='currentColor'>");
        for (int i = 0; i < 3; i++) {
            svg.append("<circle cx='").append(4 + i * 8).append("' cy='4' r='3' opacity='0.25'>")
                .append("<animate attributeName='opacity' values='0.25;1;0.25' dur='1.2s' begin='")
                .append(i * 0.2).append("s' repeatCount='indefinite'/></circle>");
        }
        return svg.append("</svg>").toString();
    }

    private final List<Item> myItems = new ArrayList<>();
    private final com.vaadin.flow.component.messages.MessageList myMarkdownLoader = new com.vaadin.flow.component.messages.MessageList();
    private static final String LINK = "event.composedPath().find(node => node.tagName === 'A' && node.hasAttribute('href'))";
    private static final String LINK_HREF = LINK + ".getAttribute('href')";

    /**
     * How many messages go into the page at a time.
     */
    private static final int PAGE_SIZE = 40;
    private static final String LOAD_EARLIER_EVENT = "consulo-load-earlier";

    private boolean myMarkdown;
    private boolean myFlushPending;
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

        // near the top the earlier messages are asked for, and what was shown stays in place when they come in front
        // of it; a list at its end stays there while its messages grow - markdown renders after they arrive
        vaadin.getElement().addEventListener(LOAD_EARLIER_EVENT, event -> loadEarlier());
        vaadin.getElement().executeJs(
            "const list = this; let loading = false; list.__pinned = true;"
                + " const resize = new ResizeObserver(() => { if (list.__pinned) { list.scrollTop = list.scrollHeight; } });"
                + " for (const child of list.children) { resize.observe(child); }"
                + " new MutationObserver(records => { for (const record of records) { for (const node of record.addedNodes) {"
                + " if (node.nodeType === 1) { resize.observe(node); } } } }).observe(list, { childList: true });"
                + " list.addEventListener('scroll', () => {"
                + " list.__pinned = list.scrollHeight - list.clientHeight - list.scrollTop < 48;"
                + " if (loading || list.scrollTop > list.clientHeight / 2) { return; }"
                + " loading = true; const height = list.scrollHeight; const top = list.scrollTop;"
                + " const added = new MutationObserver(() => { added.disconnect(); requestAnimationFrame(() => {"
                + " if (list.scrollTop === top) { list.scrollTop = top + list.scrollHeight - height; } loading = false; }); });"
                + " added.observe(list, { childList: true });"
                + " setTimeout(() => { if (loading) { added.disconnect(); loading = false; } }, 3000);"
                + " list.dispatchEvent(new CustomEvent('" + LOAD_EARLIER_EVENT + "'));"
                + " });"
        );

        WebInputDetails.addClickEventListener(vaadin.getElement(), "click", this::onLink)
            .setFilter(LINK + " != null")
            .addEventData(LINK_HREF)
            .preventDefault();
    }

    @RequiredUIAccess
    private void onLink(DomEvent event, InputDetails details) {
        String href = event.getEventData().path(LINK_HREF).asString("");
        if (href.isEmpty()) {
            return;
        }

        if (dataObject().hasListeners(HyperlinkEvent.class)) {
            getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, href, details));
        }
        else if (href.contains("://")) {
            getVaadinComponent().getUI().ifPresent(ui -> ui.getPage().open(href, "_blank"));
        }
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
        scheduleFlush();
        return item;
    }

    /**
     * The messages added during a request go into the page once, before the response - a history of thousands sends
     * only its newest {@value #PAGE_SIZE}.
     */
    private void scheduleFlush() {
        if (myFlushPending) {
            return;
        }
        Vaadin vaadin = toVaadinComponent();
        UI ui = vaadin.getUI().orElse(UI.getCurrent());
        if (ui == null) {
            flush();
            return;
        }
        myFlushPending = true;
        ui.beforeClientResponse(vaadin, context -> {
            myFlushPending = false;
            flush();
        });
    }

    private void flush() {
        int lastAttached = -1;
        for (int i = myItems.size() - 1; i >= 0; i--) {
            if (myItems.get(i).myAttached) {
                lastAttached = i;
                break;
            }
        }
        int firstNew = lastAttached + 1;
        if (firstNew >= myItems.size()) {
            return;
        }
        Element list = toVaadinComponent().getElement();
        if (myItems.size() - firstNew > PAGE_SIZE) {
            // more came than a page - the page starts again with the newest ones
            for (Item item : myItems) {
                if (item.myAttached) {
                    item.myMessage.removeFromParent();
                    item.myAttached = false;
                }
            }
            firstNew = myItems.size() - PAGE_SIZE;
        }
        for (int i = firstNew; i < myItems.size(); i++) {
            Item item = myItems.get(i);
            list.appendChild(item.myMessage);
            item.myAttached = true;
        }
        list.executeJs("const list = this; list.__pinned = true; requestAnimationFrame(() => list.scrollTop = list.scrollHeight)");
    }

    /**
     * Puts the {@value #PAGE_SIZE} messages before the first one in the page in front of it.
     */
    private void loadEarlier() {
        int firstAttached = -1;
        for (int i = 0; i < myItems.size(); i++) {
            if (myItems.get(i).myAttached) {
                firstAttached = i;
                break;
            }
        }
        if (firstAttached <= 0) {
            return;
        }
        Element list = toVaadinComponent().getElement();
        // the markdown loader stands first in the list
        int index = list.indexOfChild(myItems.get(firstAttached).myMessage);
        for (int i = Math.max(0, firstAttached - PAGE_SIZE); i < firstAttached; i++) {
            Item item = myItems.get(i);
            list.insertChild(index++, item.myMessage);
            item.myAttached = true;
        }
    }

    @RequiredUIAccess
    @Override
    public void remove(MessageListItem item) {
        if (item instanceof Item own && myItems.remove(own)) {
            own.myMessage.removeFromParent();
            own.myAttached = false;
        }
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        for (Item item : myItems) {
            item.myMessage.removeFromParent();
            item.myAttached = false;
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
    public void setShowAvatars(boolean showAvatars) {
        toVaadinComponent().setClassName("consulo-hide-avatars", !showAvatars);
    }

    @Override
    public boolean isShowAvatars() {
        return !toVaadinComponent().hasClassName("consulo-hide-avatars");
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
