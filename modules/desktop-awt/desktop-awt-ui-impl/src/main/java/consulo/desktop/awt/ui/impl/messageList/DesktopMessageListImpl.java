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
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.localize.LocalizeValue;
import consulo.ui.AvatarStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBHtmlEditorKit;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.VerticalLayout;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.impl.messageList.MessageListPresentation;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.ui.messageList.MessageListStyle;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.View;
import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
public class DesktopMessageListImpl extends SwingComponentDelegate<JBScrollPane> implements MessageList {
    private class MyScrollPane extends JBScrollPane implements FromSwingComponentWrapper {
        MyScrollPane(java.awt.Component view) {
            super(view);
        }

        @Override
        public Component toUIComponent() {
            return DesktopMessageListImpl.this;
        }
    }

    private static class ListPanel extends JPanel implements Scrollable {
        ListPanel() {
            super(new VerticalLayout(DesktopSpace.toPixels(Space.LARGE)));
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

    private static class MessageBody extends JEditorPane {
        MessageBody() {
            setEditable(false);
            setOpaque(false);
            setBorder(JBUI.Borders.empty());
            setEditorKit(JBHtmlEditorKit.create(true));
        }

        @Override
        public Dimension getPreferredSize() {
            Container parent = getParent();
            int width = parent == null ? 0 : parent.getWidth();
            if (width <= 0) {
                return super.getPreferredSize();
            }
            View root = getUI().getRootView(this);
            root.setSize(width, Integer.MAX_VALUE);
            Insets insets = getInsets();
            return new Dimension(width, (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS)) + insets.top + insets.bottom);
        }
    }

    private class Item implements MessageListItem {
        private final LocalizeValue myAuthor;
        private final JPanel myPanel = new JPanel(new BorderLayout(DesktopSpace.toPixels(Space.MEDIUM), 0));
        private final AvatarComponent myAvatar = new AvatarComponent(myCompact ? AvatarStyle.X_SMALL : AvatarStyle.SMALL);
        private final JPanel myAvatarBox = new JPanel(new BorderLayout());
        private final JBLabel myHeader = new JBLabel();
        private final MessageBody myBody = new MessageBody();

        private String myText;
        private @Nullable Instant myTime;
        private @Nullable Image myAuthorImage;
        private MessageListItemStyle myStyle = MessageListItemStyle.OTHER;

        Item(LocalizeValue author, String text) {
            myAuthor = author;
            myText = text;

            myPanel.setOpaque(false);
            myPanel.setBorder(JBUI.Borders.empty(0, 4));

            myAvatarBox.setOpaque(false);
            myAvatarBox.add(myAvatar, BorderLayout.NORTH);
            myPanel.add(myAvatarBox, BorderLayout.WEST);

            JPanel content = new JPanel(new VerticalLayout(DesktopSpace.toPixels(Space.X_SMALL)));
            content.setOpaque(false);
            content.add(myHeader);
            content.add(myBody);
            myPanel.add(content, BorderLayout.CENTER);

            updateHeader();
            updateBody();
        }

        private void updateHeader() {
            String name = myAuthor.get();
            myAvatar.setAvatarName(name);
            String time = MessageListPresentation.formatTime(myTime);
            String timePart = time == null
                ? ""
                : "&nbsp;&nbsp;<span style='font-size:smaller;color:" + ColorUtil.toHtmlColor(UIUtil.getContextHelpForeground()) + "'>" + time + "</span>";
            myHeader.setText("<html><b>" + StringUtil.escapeXmlEntities(name) + "</b>" + timePart + "</html>");
        }

        private void updateBody() {
            myAvatarBox.setVisible(MessageListPresentation.showsAvatar(myStyle, myShowOwnAvatar));
            myBody.setForeground(switch (myStyle) {
                case ERROR -> TargetAWT.to(ComponentColors.ERROR_FOREGROUND);
                case SYSTEM -> UIUtil.getContextHelpForeground();
                default -> UIUtil.getLabelForeground();
            });
            myBody.setText("<html><head><style>" + MessageListMarkdown.styleSheet() + "</style></head><body>"
                + MessageListMarkdown.toHtml(myText, myMarkdown) + "</body></html>");
            myBody.setVisible(!myText.isEmpty());
            relayout();
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
            updateBody();
        }

        @RequiredUIAccess
        @Override
        public void appendText(String text) {
            setText(myText + text);
        }

        @Override
        public @Nullable Instant getTime() {
            return myTime;
        }

        @RequiredUIAccess
        @Override
        public void setTime(@Nullable Instant time) {
            myTime = time;
            updateHeader();
        }

        @Override
        public @Nullable Image getAuthorImage() {
            return myAuthorImage;
        }

        @RequiredUIAccess
        @Override
        public void setAuthorImage(@Nullable Image image) {
            myAuthorImage = image;
            myAvatar.setImage(image);
        }

        @Override
        public MessageListItemStyle getStyle() {
            return myStyle;
        }

        @RequiredUIAccess
        @Override
        public void setStyle(MessageListItemStyle style) {
            myStyle = style;
            updateBody();
        }
    }

    private final ListPanel myListPanel = new ListPanel();
    private final List<Item> myItems = new ArrayList<>();
    private boolean myMarkdown;
    private boolean myShowOwnAvatar;
    private boolean myCompact;

    @Override
    protected JBScrollPane createComponent() {
        myListPanel.setBorder(JBUI.Borders.empty(8));
        myListPanel.setOpaque(false);

        MyScrollPane scrollPane = new MyScrollPane(myListPanel);
        scrollPane.setBorder(JBUI.Borders.empty());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    @RequiredUIAccess
    @Override
    public void addStyle(MessageListStyle style) {
        if (style != MessageListStyle.COMPACT || myCompact) {
            return;
        }
        myCompact = true;

        myListPanel.setBorder(JBUI.Borders.empty());
        myListPanel.removeAll();
        myListPanel.setLayout(new VerticalLayout(DesktopSpace.toPixels(Space.SMALL)));
        for (Item item : myItems) {
            item.myAvatar.setStyle(AvatarStyle.X_SMALL);
            myListPanel.add(item.myPanel);
        }

        // a compact list stands on whatever is under it, so nothing of its own is painted
        JBScrollPane scrollPane = toAWTComponent();
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        relayout();
    }

    private void relayout() {
        JScrollBar bar = toAWTComponent().getVerticalScrollBar();
        boolean atBottom = bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - JBUI.scale(24);

        myListPanel.revalidate();
        myListPanel.repaint();

        if (atBottom) {
            SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum()));
        }
    }

    @RequiredUIAccess
    @Override
    public MessageListItem add(LocalizeValue author, String text) {
        Item item = new Item(author, text);
        myItems.add(item);
        myListPanel.add(item.myPanel);

        JScrollBar bar = toAWTComponent().getVerticalScrollBar();
        myListPanel.revalidate();
        myListPanel.repaint();
        SwingUtilities.invokeLater(() -> bar.setValue(bar.getMaximum()));
        return item;
    }

    @RequiredUIAccess
    @Override
    public void remove(MessageListItem item) {
        if (item instanceof Item own && myItems.remove(own)) {
            myListPanel.remove(own.myPanel);
            relayout();
        }
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        myItems.clear();
        myListPanel.removeAll();
        relayout();
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
        for (Item item : myItems) {
            item.updateBody();
        }
    }

    @Override
    public boolean isMarkdown() {
        return myMarkdown;
    }

    @RequiredUIAccess
    @Override
    public void setShowOwnAvatar(boolean showOwnAvatar) {
        if (myShowOwnAvatar == showOwnAvatar) {
            return;
        }
        myShowOwnAvatar = showOwnAvatar;
        for (Item item : myItems) {
            item.updateBody();
        }
    }

    @Override
    public boolean isShowOwnAvatar() {
        return myShowOwnAvatar;
    }
}
