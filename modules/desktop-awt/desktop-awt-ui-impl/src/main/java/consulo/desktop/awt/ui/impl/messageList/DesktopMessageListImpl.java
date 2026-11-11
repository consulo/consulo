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
package consulo.desktop.awt.ui.impl.messageList;

import consulo.desktop.awt.ui.impl.DesktopSpace;
import consulo.desktop.awt.ui.impl.avatar.AvatarComponent;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.AvatarStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.ex.awt.AsyncProcessIcon;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.VerticalLayout;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.impl.messageList.MessageListPresentation;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemPart;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.ui.messageList.MessageListStyle;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A virtual list: a message is data, and gets components only while it is near the viewport - a chat of thousands of
 * messages costs about a screen of them. The list knows the height of every message, as measured once it had its
 * components or as estimated before, and places the components of the ones near the viewport at their offsets.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public class DesktopMessageListImpl extends SwingComponentDelegate<JBScrollPane> implements MessageList {
    private static final int ESTIMATED_LINE_LENGTH = 100;
    private static final int ESTIMATED_LINE_HEIGHT = 18;
    private static final int ESTIMATED_HEADER_HEIGHT = 24;
    private static final int MAX_ESTIMATED_LINES = 200;
    /**
     * How often a streamed answer is rendered again while it grows, in milliseconds.
     */
    private static final int APPEND_RENDER_DELAY = 100;

    private class MyScrollPane extends JBScrollPane implements FromSwingComponentWrapper {
        MyScrollPane(java.awt.Component view) {
            super(view);
        }

        @Override
        public boolean isValidateRoot() {
            return false;
        }

        @Override
        public Component toUIComponent() {
            return DesktopMessageListImpl.this;
        }
    }

    /**
     * Holds the components of the messages near the viewport, each at the offset of its message - as tall as all the
     * messages together.
     */
    private class ListPanel extends JPanel implements Scrollable {
        ListPanel() {
            super(null);
        }

        @Override
        public Dimension getPreferredSize() {
            Insets insets = getInsets();
            int width = contentWidth();
            int height = insets.top + insets.bottom;
            for (int i = 0; i < myItems.size(); i++) {
                height += heightOf(myItems.get(i), width) + (i > 0 ? gap() : 0);
            }
            return new Dimension(width + insets.left + insets.right, height);
        }

        @Override
        public void doLayout() {
            Insets insets = getInsets();
            int width = contentWidth();
            int y = insets.top;
            for (Item item : myItems) {
                int height = heightOf(item, width);
                ItemView view = item.myView;
                if (view != null) {
                    view.myPanel.setBounds(insets.left, y, width, height);
                }
                y += height + gap();
            }
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return JBUI.scale(16);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * A message - data alone, with its components while it is near the viewport.
     */
    private class Item implements MessageListItem {
        private final LocalizeValue myAuthor;
        private String myText;
        private List<MessageListItemPart> myParts = List.of();
        private @Nullable Instant myTime;
        private @Nullable Image myAuthorImage;
        private MessageListItemStyle myStyle = MessageListItemStyle.OTHER;
        private @Nullable Component myActionsComponent;
        private boolean myWriting;
        private LocalizeValue myWritingStatus = LocalizeValue.empty();

        private @Nullable ItemView myView;
        /**
         * The height the message took at {@link #myMeasuredWidth}, -1 while never measured.
         */
        private int myMeasuredHeight = -1;
        private int myMeasuredWidth = -1;
        private boolean myAppendPending;

        Item(LocalizeValue author, String text) {
            myAuthor = author;
            myText = text;
        }

        /**
         * The content changed - the view shows it again, and the height is measured again.
         */
        private void changed() {
            myMeasuredHeight = -1;
            ItemView view = myView;
            if (view != null) {
                view.update();
            }
            contentChanged();
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
            myParts = List.of();
            changed();
        }

        @RequiredUIAccess
        @Override
        public void appendText(String text) {
            myText = myText + text;
            if (!myParts.isEmpty()) {
                myParts = List.of();
                changed();
                return;
            }
            // a streamed answer grows by a few characters at a time - it is rendered again at most so often
            if (!myAppendPending) {
                myAppendPending = true;
                Timer timer = new Timer(APPEND_RENDER_DELAY, event -> {
                    myAppendPending = false;
                    changed();
                });
                timer.setRepeats(false);
                timer.start();
            }
        }

        @Override
        public List<MessageListItemPart> getParts() {
            return myParts;
        }

        @RequiredUIAccess
        @Override
        public void setParts(List<MessageListItemPart> parts) {
            ItemView view = myView;
            if (view != null) {
                view.detachComponents();
            }
            myParts = List.copyOf(parts);
            changed();
        }

        @Override
        public @Nullable Instant getTime() {
            return myTime;
        }

        @RequiredUIAccess
        @Override
        public void setTime(@Nullable Instant time) {
            myTime = time;
            changed();
        }

        @Override
        public @Nullable Image getAuthorImage() {
            return myAuthorImage;
        }

        @RequiredUIAccess
        @Override
        public void setAuthorImage(@Nullable Image image) {
            myAuthorImage = image;
            ItemView view = myView;
            if (view != null) {
                view.myAvatar.setImage(image);
            }
        }

        @Override
        public MessageListItemStyle getStyle() {
            return myStyle;
        }

        @RequiredUIAccess
        @Override
        public void setStyle(MessageListItemStyle style) {
            myStyle = style;
            changed();
        }

        @Override
        public @Nullable Component getActionsComponent() {
            return myActionsComponent;
        }

        @RequiredUIAccess
        @Override
        public void setActionsComponent(@Nullable Component component) {
            ItemView view = myView;
            if (view != null) {
                view.detachComponents();
            }
            myActionsComponent = component;
            changed();
        }

        @Override
        public boolean isWriting() {
            return myWriting;
        }

        @RequiredUIAccess
        @Override
        public void setWriting(boolean writing) {
            if (myWriting != writing) {
                myWriting = writing;
                changed();
            }
        }

        @Override
        public LocalizeValue getWritingStatus() {
            return myWritingStatus;
        }

        @RequiredUIAccess
        @Override
        public void setWritingStatus(LocalizeValue status) {
            myWritingStatus = status;
            changed();
        }
    }

    /**
     * The components of a message near the viewport - made from its data, dropped once it is far from the viewport.
     */
    private class ItemView {
        private final Item myItem;
        private final JPanel myPanel = new JPanel(new BorderLayout(DesktopSpace.toPixels(Space.MEDIUM), 0));
        private final AvatarComponent myAvatar = new AvatarComponent(myCompact ? AvatarStyle.X_SMALL : AvatarStyle.SMALL);
        private final JPanel myAvatarBox = new JPanel(new BorderLayout());
        private final JBLabel myHeader = new JBLabel();
        private final JPanel myHeaderRow = new JPanel(new BorderLayout(DesktopSpace.toPixels(Space.MEDIUM), 0));
        private final CobraMessageBody myBody = new CobraMessageBody(DesktopMessageListImpl.this::linkClicked, this::rendered);
        private final JPanel myPartsPanel = new JPanel(new VerticalLayout(DesktopSpace.toPixels(Space.SMALL)));
        private final JPanel myWritingRow = new JPanel(new BorderLayout(DesktopSpace.toPixels(Space.SMALL), 0));
        private final JBLabel myWritingLabel = new JBLabel();
        private @Nullable AsyncProcessIcon myWritingIcon;
        private java.awt.@Nullable Component myAttachedActions;
        private final List<java.awt.Component> myAttachedParts = new ArrayList<>();

        ItemView(Item item) {
            myItem = item;

            myPanel.setOpaque(false);
            myPanel.setBorder(JBUI.Borders.empty(0, 4));

            myAvatarBox.setOpaque(false);
            myAvatarBox.add(myAvatar, BorderLayout.NORTH);
            myPanel.add(myAvatarBox, BorderLayout.WEST);

            JPanel content = new JPanel(new VerticalLayout(DesktopSpace.toPixels(Space.X_SMALL)));
            content.setOpaque(false);
            myHeaderRow.setOpaque(false);
            myHeaderRow.add(myHeader, BorderLayout.CENTER);
            content.add(myHeaderRow);
            content.add(myBody);
            myPartsPanel.setOpaque(false);
            content.add(myPartsPanel);
            myWritingRow.setOpaque(false);
            myWritingLabel.setForeground(UIUtil.getContextHelpForeground());
            myWritingRow.add(myWritingLabel, BorderLayout.CENTER);
            content.add(myWritingRow);
            myPanel.add(content, BorderLayout.CENTER);

            myAvatar.setImage(item.myAuthorImage);
            update();
        }

        void update() {
            Item item = myItem;
            String name = item.myAuthor.get();
            myAvatar.setAvatarName(name);
            myAvatarBox.setVisible(MessageListPresentation.showsAvatar(item.myStyle, myShowOwnAvatar, myShowAvatars));
            String time = MessageListPresentation.formatTime(item.myTime);
            String timePart = time == null
                ? ""
                : "&nbsp;&nbsp;<span style='font-size:smaller;color:" + ColorUtil.toHtmlColor(UIUtil.getContextHelpForeground()) + "'>" + time + "</span>";
            myHeader.setText("<html><b>" + StringUtil.escapeXmlEntities(name) + "</b>" + timePart + "</html>");

            Component actions = item.myActionsComponent;
            if (actions != null && myAttachedActions == null) {
                java.awt.Component awt = TargetAWT.to(actions);
                myHeaderRow.add(awt, BorderLayout.EAST);
                myAttachedActions = awt;
            }

            fillBody(myBody, item.myText);
            myBody.setVisible(item.myParts.isEmpty() && !item.myText.isEmpty());


            if (myAttachedParts.isEmpty() && !item.myParts.isEmpty()) {
                myPartsPanel.removeAll();
                for (MessageListItemPart part : item.myParts) {
                    String text = part.getText();
                    Component component = part.getComponent();
                    if (text != null) {
                        CobraMessageBody body = new CobraMessageBody(DesktopMessageListImpl.this::linkClicked, this::rendered);
                        fillBody(body, text);
                        myPartsPanel.add(body);
                        myAttachedParts.add(body);
                    }
                    else if (component != null) {
                        java.awt.Component awt = TargetAWT.to(component);
                        myPartsPanel.add(awt);
                        myAttachedParts.add(awt);
                    }
                }
            }
            myPartsPanel.setVisible(!item.myParts.isEmpty());

            if (item.myWriting && myWritingIcon == null) {
                AsyncProcessIcon icon = new AsyncProcessIcon("MessageListWriting");
                myWritingIcon = icon;
                myWritingRow.add(icon, BorderLayout.WEST);
                icon.resume();
            }
            else if (!item.myWriting) {
                disposeWritingIcon();
            }
            myWritingLabel.setText(item.myWritingStatus.get());
            myWritingRow.setVisible(item.myWriting);

            myPanel.revalidate();
            myPanel.repaint();
        }

        private void fillBody(CobraMessageBody body, String text) {
            Color foreground = switch (myItem.myStyle) {
                case ERROR -> TargetAWT.to(ComponentColors.ERROR_FOREGROUND);
                case SYSTEM -> UIUtil.getContextHelpForeground();
                default -> UIUtil.getLabelForeground();
            };
            body.render(MessageListMarkdown.toCobraHtml(text, myMarkdown), MessageListMarkdown.cobraStyleSheet(foreground), myImageLoader,
                estimateTextHeight(text));
        }

        /**
         * A body rendered its document - the message is measured again.
         */
        private void rendered() {
            if (myItem.myView == this) {
                myItem.myMeasuredHeight = -1;
                contentChanged();
            }
        }

        /**
         * Gives back the components of the caller - the actions and the parts - which outlive the view.
         */
        void detachComponents() {
            java.awt.Component actions = myAttachedActions;
            if (actions != null) {
                myHeaderRow.remove(actions);
                myAttachedActions = null;
            }
            myPartsPanel.removeAll();
            myAttachedParts.clear();
        }

        void dispose() {
            detachComponents();
            disposeWritingIcon();
        }

        private void disposeWritingIcon() {
            AsyncProcessIcon icon = myWritingIcon;
            myWritingIcon = null;
            if (icon != null) {
                myWritingRow.remove(icon);
                icon.dispose();
            }
        }

        /**
         * The height at the width, laid out at it first - the wrapping bodies follow the width of their parent.
         */
        int measure(int width) {
            myPanel.setSize(width, Math.max(1, myPanel.getHeight()));
            myPanel.doLayout();
            myPanel.validate();
            return myPanel.getPreferredSize().height;
        }
    }

    private final ListPanel myListPanel = new ListPanel();
    private final List<Item> myItems = new ArrayList<>();
    private boolean myMarkdown;
    private boolean myShowOwnAvatar;
    private boolean myShowAvatars = true;
    private @Nullable AsyncHtmlImageLoader myImageLoader;
    private boolean myCompact;
    private boolean myUpdatePending;
    /**
     * Whether the list follows its end - true until the user scrolls away from it.
     */
    private boolean myPinned = true;
    private int myLastMaximum = -1;
    private int myLastValue;

    @Override
    protected JBScrollPane createComponent() {
        myListPanel.setBorder(JBUI.Borders.empty(8));
        myListPanel.setOpaque(false);

        MyScrollPane scrollPane = new MyScrollPane(myListPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        bar.addAdjustmentListener(event -> {
            int maximum = bar.getMaximum();
            int value = bar.getValue();
            boolean atEnd = value + bar.getVisibleAmount() >= maximum - JBUI.scale(24);
            if (maximum != myLastMaximum) {
                // the content grew or shrank - a list at its end stays there, it is not the user scrolling away
                myLastMaximum = maximum;
                if (myPinned) {
                    SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum()));
                }
            }
            else if (value < myLastValue && myListPanel.getHeight() == myListPanel.getPreferredSize().height) {
                // only the user scrolling up leaves the end - not the viewport clamping its position to a content
                // that shrank and is not laid out at its new height yet
                myPinned = atEnd;
            }
            else if (atEnd) {
                myPinned = true;
            }
            myLastValue = value;
            scheduleUpdate();
        });
        // resized - the heights are measured again at the new width
        scrollPane.getViewport().addChangeListener(event -> scheduleUpdate());
        return scrollPane;
    }

    private int gap() {
        return DesktopSpace.toPixels(myCompact ? Space.SMALL : Space.LARGE);
    }

    private int contentWidth() {
        Insets insets = myListPanel.getInsets();
        int width = toAWTComponent().getViewport().getWidth();
        return Math.max(0, width - insets.left - insets.right);
    }

    /**
     * The height of the message at the width - measured once it had its components there, estimated before.
     */
    private int heightOf(Item item, int width) {
        if (item.myMeasuredHeight >= 0 && item.myMeasuredWidth == width) {
            return item.myMeasuredHeight;
        }
        ItemView view = item.myView;
        if (view != null && width > 0) {
            item.myMeasuredHeight = view.measure(width);
            item.myMeasuredWidth = width;
            return item.myMeasuredHeight;
        }
        if (item.myMeasuredHeight >= 0) {
            return item.myMeasuredHeight;
        }
        return estimateHeight(item);
    }

    /**
     * About the height the message takes once it has its components - only that the scroll bar does not jump much when
     * it gets them.
     */
    private static int estimateHeight(Item item) {
        return JBUI.scale(ESTIMATED_HEADER_HEIGHT) + estimateTextHeight(item.myText);
    }

    private static int estimateTextHeight(String text) {
        int lines = 1;
        int lineLength = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || ++lineLength > ESTIMATED_LINE_LENGTH) {
                lines++;
                lineLength = 0;
            }
        }
        return Math.min(lines, MAX_ESTIMATED_LINES) * JBUI.scale(ESTIMATED_LINE_HEIGHT);
    }

    private void contentChanged() {
        myListPanel.revalidate();
        myListPanel.repaint();
        scheduleUpdate();
    }

    private void scheduleUpdate() {
        if (myUpdatePending) {
            return;
        }
        myUpdatePending = true;
        SwingUtilities.invokeLater(() -> {
            myUpdatePending = false;
            updateViews();
        });
    }

    /**
     * Gives components to the messages from a screen above the viewport to a screen below it and takes them from the
     * ones farther than three screens - the scroll position stays on what it shows while heights become known.
     */
    private void updateViews() {
        JBScrollPane scrollPane = toAWTComponent();
        JViewport viewport = scrollPane.getViewport();
        int width = contentWidth();
        if (width <= 0) {
            return;
        }
        Rectangle view = viewport.getViewRect();
        int keepTop = view.y - view.height;
        int keepBottom = view.y + view.height * 2;
        int dropTop = view.y - view.height * 3;
        int dropBottom = view.y + view.height * 4;

        boolean changed = false;
        int shiftAbove = 0;
        int y = myListPanel.getInsets().top;
        for (Item item : myItems) {
            int before = heightOf(item, width);
            int top = y;
            int bottom = y + before;
            if (item.myView == null && bottom >= keepTop && top <= keepBottom) {
                ItemView itemView = new ItemView(item);
                item.myView = itemView;
                myListPanel.add(itemView.myPanel);
                changed = true;
            }
            else if (item.myView != null && (bottom < dropTop || top > dropBottom)) {
                ItemView itemView = item.myView;
                item.myView = null;
                itemView.dispose();
                myListPanel.remove(itemView.myPanel);
                changed = true;
            }
            int after = heightOf(item, width);
            if (after != before && top < view.y) {
                // a message above the viewport got its real height - what the viewport shows stays in place
                shiftAbove += after - before;
            }
            y += after + gap();
        }

        if (changed || shiftAbove != 0) {
            myListPanel.revalidate();
            myListPanel.repaint();
        }
        JScrollBar bar = scrollPane.getVerticalScrollBar();
        if (myPinned) {
            SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum()));
        }
        else if (shiftAbove != 0) {
            int shift = shiftAbove;
            SwingUtilities.invokeLater(() -> bar.setValue(bar.getValue() + shift));
        }
    }

    @RequiredUIAccess
    @Override
    public void addStyle(MessageListStyle style) {
        if (style != MessageListStyle.COMPACT || myCompact) {
            return;
        }
        myCompact = true;

        myListPanel.setBorder(JBUI.Borders.empty());
        for (Item item : myItems) {
            ItemView view = item.myView;
            if (view != null) {
                view.myAvatar.setStyle(AvatarStyle.X_SMALL);
            }
            item.myMeasuredHeight = -1;
        }

        // a compact list stands on whatever is under it, so nothing of its own is painted
        JBScrollPane scrollPane = toAWTComponent();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        contentChanged();
    }

    @RequiredUIAccess
    @Override
    public MessageListItem add(LocalizeValue author, String text) {
        Item item = new Item(author, text);
        myItems.add(item);
        myPinned = true;
        contentChanged();
        return item;
    }

    @RequiredUIAccess
    @Override
    public void remove(MessageListItem item) {
        if (item instanceof Item own && myItems.remove(own)) {
            ItemView view = own.myView;
            own.myView = null;
            if (view != null) {
                view.dispose();
                myListPanel.remove(view.myPanel);
            }
            contentChanged();
        }
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        for (Item item : myItems) {
            ItemView view = item.myView;
            item.myView = null;
            if (view != null) {
                view.dispose();
            }
        }
        myItems.clear();
        myListPanel.removeAll();
        myPinned = true;
        contentChanged();
    }

    @Override
    public List<MessageListItem> getItems() {
        return List.copyOf(myItems);
    }

    /**
     * Everything shown changed - every message is shown again, and its height measured again.
     */
    private void allChanged() {
        for (Item item : myItems) {
            item.myMeasuredHeight = -1;
            ItemView view = item.myView;
            if (view != null) {
                view.update();
            }
        }
        contentChanged();
    }

    @RequiredUIAccess
    @Override
    public void setMarkdown(boolean markdown) {
        if (myMarkdown != markdown) {
            myMarkdown = markdown;
            allChanged();
        }
    }

    @Override
    public boolean isMarkdown() {
        return myMarkdown;
    }

    @RequiredUIAccess
    @Override
    public void setShowOwnAvatar(boolean showOwnAvatar) {
        if (myShowOwnAvatar != showOwnAvatar) {
            myShowOwnAvatar = showOwnAvatar;
            allChanged();
        }
    }

    @Override
    public boolean isShowOwnAvatar() {
        return myShowOwnAvatar;
    }

    @RequiredUIAccess
    @Override
    public void setShowAvatars(boolean showAvatars) {
        if (myShowAvatars != showAvatars) {
            myShowAvatars = showAvatars;
            allChanged();
        }
    }

    @Override
    public boolean isShowAvatars() {
        return myShowAvatars;
    }

    @RequiredUIAccess
    @Override
    public void setImageLoader(@Nullable AsyncHtmlImageLoader imageLoader) {
        if (myImageLoader != imageLoader) {
            myImageLoader = imageLoader;
            allChanged();
        }
    }

    @Override
    public @Nullable AsyncHtmlImageLoader getImageLoader() {
        return myImageLoader;
    }

    /**
     * A link of a message clicked - told to the listeners of the list, or opened outside when an absolute url and
     * nobody listens.
     */
    private boolean linkClicked(String href, java.awt.event.MouseEvent event) {
        if (href.isEmpty()) {
            return false;
        }
        if (hasListeners(HyperlinkEvent.class)) {
            getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, href, DesktopAWTInputDetails.convert(toAWTComponent(), event)));
            return true;
        }
        if (href.contains("://")) {
            Platform.current().openInBrowser(href);
            return true;
        }
        return false;
    }
}
