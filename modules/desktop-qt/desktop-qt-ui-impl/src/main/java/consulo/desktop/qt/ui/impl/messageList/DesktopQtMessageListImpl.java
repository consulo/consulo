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
package consulo.desktop.qt.ui.impl.messageList;

import consulo.desktop.qt.ui.impl.DesktopQtSpace;
import consulo.desktop.qt.ui.impl.DesktopQtStyleApplier;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.avatar.DesktopQtAvatarPainter;
import consulo.desktop.qt.ui.impl.base.DesktopQtWrappingLabel;
import consulo.localize.LocalizeValue;
import consulo.ui.AvatarStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.avatar.AvatarPresentation;
import consulo.ui.impl.messageList.MessageListPresentation;
import consulo.ui.messageList.MessageList;
import consulo.ui.Component;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemPart;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.ui.messageList.MessageListStyle;
import consulo.ui.style.ComponentColors;
import io.qt.core.QSize;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QPalette;
import io.qt.gui.QTextDocument;
import io.qt.widgets.QFrame;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLabel;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QScrollBar;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-13
 */
public class DesktopQtMessageListImpl extends QtComponentDelegate<QScrollArea> implements MessageList {
    private static final int PINNED_DISTANCE = 24;

    private class Item implements MessageListItem {
        private final LocalizeValue myAuthor;
        private String myText;
        private @Nullable Instant myTime;
        private @Nullable Image myAuthorImage;
        private MessageListItemStyle myStyle = MessageListItemStyle.OTHER;
        private List<MessageListItemPart> myParts = List.of();

        private @Nullable QWidget myRow;
        private @Nullable QLabel myAvatar;
        private @Nullable QLabel myHeader;
        private @Nullable QHBoxLayout myHeaderLayout;
        private @Nullable Component myActionsComponent;
        private @Nullable QLabel myBody;
        private @Nullable QWidget myPartsWidget;
        private @Nullable QVBoxLayout myPartsLayout;
        private final List<QLabel> myPartLabels = new ArrayList<>();
        private final List<QtComponentDelegate<?>> myPartComponents = new ArrayList<>();

        Item(LocalizeValue author, String text) {
            myAuthor = author;
            myText = text;
        }

        void build(QWidget container) {
            QWidget row = new QWidget(container);
            QHBoxLayout rowLayout = new QHBoxLayout(row);
            rowLayout.setContentsMargins(0, 0, 0, 0);
            rowLayout.setSpacing(DesktopQtSpace.toPixels(Space.MEDIUM));

            QLabel avatar = new QLabel(row);
            avatar.setFixedSize(avatarSize(), avatarSize());
            rowLayout.addWidget(avatar, 0, Qt.AlignmentFlag.AlignTop);

            QWidget content = new QWidget(row);
            QVBoxLayout contentLayout = new QVBoxLayout(content);
            contentLayout.setContentsMargins(0, 0, 0, 0);
            contentLayout.setSpacing(DesktopQtSpace.toPixels(Space.X_SMALL));

            QWidget headerRow = new QWidget(content);
            QHBoxLayout headerLayout = new QHBoxLayout(headerRow);
            headerLayout.setContentsMargins(0, 0, 0, 0);
            headerLayout.setSpacing(DesktopQtSpace.toPixels(Space.MEDIUM));

            QLabel header = new QLabel(headerRow);
            header.setTextFormat(Qt.TextFormat.RichText);
            headerLayout.addWidget(header, 1);
            contentLayout.addWidget(headerRow);

            QLabel body = createBodyLabel(content);
            contentLayout.addWidget(body);

            QWidget parts = new QWidget(content);
            QVBoxLayout partsLayout = new QVBoxLayout(parts);
            partsLayout.setContentsMargins(0, 0, 0, 0);
            partsLayout.setSpacing(DesktopQtSpace.toPixels(Space.SMALL));
            contentLayout.addWidget(parts);

            rowLayout.addWidget(content, 1);

            myRow = row;
            myAvatar = avatar;
            myHeader = header;
            myHeaderLayout = headerLayout;
            myBody = body;
            myPartsWidget = parts;
            myPartsLayout = partsLayout;

            updateAvatar();
            updateHeader();
            attachActions();
            rebuildParts();
            updateBody();
        }

        private void attachActions() {
            QHBoxLayout layout = myHeaderLayout;
            QWidget parent = layout == null ? null : layout.parentWidget();
            if (parent == null || parent.isDisposed() || !(myActionsComponent instanceof QtComponentDelegate<?> delegate)) {
                return;
            }
            delegate.setParent(DesktopQtMessageListImpl.this);
            delegate.bind(parent, null);
            QWidget widget = delegate.toQtComponent();
            if (widget != null) {
                layout.addWidget(widget, 0, Qt.AlignmentFlag.AlignVCenter);
            }
            contentChanged();
        }

        private void detachActions() {
            QHBoxLayout layout = myHeaderLayout;
            if (!(myActionsComponent instanceof QtComponentDelegate<?> delegate)) {
                return;
            }
            QWidget widget = delegate.toQtComponent();
            if (layout != null && widget != null && !widget.isDisposed()) {
                layout.removeWidget(widget);
            }
            delegate.disposeQt();
        }

        private QLabel createBodyLabel(QWidget parent) {
            DesktopQtWrappingLabel body = new DesktopQtWrappingLabel(parent);
            body.setWordWrap(true);
            body.setOpenExternalLinks(true);
            body.setTextInteractionFlags(Qt.TextInteractionFlag.TextBrowserInteraction);
            return body;
        }

        void rebuildParts() {
            QWidget parts = myPartsWidget;
            QVBoxLayout layout = myPartsLayout;
            if (parts == null || layout == null || parts.isDisposed()) {
                return;
            }

            for (QLabel label : myPartLabels) {
                if (!label.isDisposed()) {
                    layout.removeWidget(label);
                    label.hide();
                    label.disposeLater();
                }
            }
            myPartLabels.clear();
            for (QtComponentDelegate<?> delegate : myPartComponents) {
                QWidget widget = delegate.toQtComponent();
                if (widget != null && !widget.isDisposed()) {
                    layout.removeWidget(widget);
                }
                delegate.disposeQt();
            }
            myPartComponents.clear();

            for (MessageListItemPart part : myParts) {
                Component component = part.getComponent();
                if (part.getText() != null) {
                    QLabel label = createBodyLabel(parts);
                    layout.addWidget(label);
                    myPartLabels.add(label);
                }
                else if (component instanceof QtComponentDelegate<?> delegate) {
                    delegate.setParent(DesktopQtMessageListImpl.this);
                    delegate.bind(parts, null);
                    QWidget widget = delegate.toQtComponent();
                    if (widget != null) {
                        layout.addWidget(widget);
                    }
                    myPartComponents.add(delegate);
                }
            }
        }

        void updateAvatar() {
            QLabel avatar = myAvatar;
            if (avatar == null) {
                return;
            }
            Image image = myAuthorImage;
            avatar.setFixedSize(avatarSize(), avatarSize());
            avatar.setPixmap(DesktopQtAvatarPainter.paint(myAuthor.get(), image, avatarSize(), avatar.devicePixelRatioF(), avatar.palette()));
            avatar.setVisible(MessageListPresentation.showsAvatar(myStyle, myShowOwnAvatar));
        }

        void updateHeader() {
            QLabel header = myHeader;
            if (header == null) {
                return;
            }
            String time = MessageListPresentation.formatTime(myTime);
            QColor muted = DesktopQtStyleApplier.themeColor(ComponentColors.INFO_FOREGROUND, header.palette().color(QPalette.ColorRole.PlaceholderText));
            header.setText("<b>" + escape(myAuthor.get()) + "</b>"
                + (time == null ? "" : "&nbsp;&nbsp;<span style='font-size:small;color:" + muted.name() + "'>" + escape(time) + "</span>"));
        }

        void updateBody() {
            QLabel avatar = myAvatar;
            if (avatar != null) {
                avatar.setVisible(MessageListPresentation.showsAvatar(myStyle, myShowOwnAvatar));
            }
            QLabel body = myBody;
            if (body == null) {
                return;
            }
            fillLabel(body, myText);
            body.setVisible(myParts.isEmpty() && !myText.isEmpty());

            int labelIndex = 0;
            for (MessageListItemPart part : myParts) {
                String text = part.getText();
                if (text != null && labelIndex < myPartLabels.size()) {
                    fillLabel(myPartLabels.get(labelIndex++), text);
                }
            }
            QWidget parts = myPartsWidget;
            if (parts != null && !parts.isDisposed()) {
                parts.setVisible(!myParts.isEmpty());
            }
            contentChanged();
        }

        private void fillLabel(QLabel label, String text) {
            if (label.isDisposed()) {
                return;
            }
            if (myMarkdown) {
                label.setTextFormat(Qt.TextFormat.RichText);
                label.setText(markdownToHtml(text));
            }
            else {
                label.setTextFormat(Qt.TextFormat.PlainText);
                label.setText(text);
            }

            QColor color = switch (myStyle) {
                case ERROR -> DesktopQtStyleApplier.themeColor(ComponentColors.ERROR_FOREGROUND, label.palette().color(QPalette.ColorRole.WindowText));
                case SYSTEM -> DesktopQtStyleApplier.themeColor(ComponentColors.INFO_FOREGROUND, label.palette().color(QPalette.ColorRole.PlaceholderText));
                default -> null;
            };
            label.setStyleSheet(color == null ? "" : "QLabel { color: " + color.name() + "; }");
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
            if (!myParts.isEmpty()) {
                myParts = List.of();
                rebuildParts();
            }
            updateBody();
        }

        @Override
        public List<MessageListItemPart> getParts() {
            return myParts;
        }

        @RequiredUIAccess
        @Override
        public void setParts(List<MessageListItemPart> parts) {
            myParts = List.copyOf(parts);
            rebuildParts();
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
            updateAvatar();
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

        @Override
        public @Nullable Component getActionsComponent() {
            return myActionsComponent;
        }

        @RequiredUIAccess
        @Override
        public void setActionsComponent(@Nullable Component component) {
            if (myActionsComponent == component) {
                return;
            }
            detachActions();
            myActionsComponent = component;
            attachActions();
        }
    }

    private final List<Item> myItems = new ArrayList<>();
    private static final int MARKDOWN_UNDERLINE = 0x4000;

    private static String markdownToHtml(String text) {
        QTextDocument document = new QTextDocument();
        try {
            int features = QTextDocument.MarkdownFeature.MarkdownDialectGitHub.value() & ~MARKDOWN_UNDERLINE;
            document.setMarkdown(text, new QTextDocument.MarkdownFeatures(features));
            return document.toHtml();
        }
        finally {
            document.dispose();
        }
    }

    private boolean myMarkdown;
    private boolean myShowOwnAvatar;
    private boolean myCompact;
    private boolean myPinned = true;
    private boolean myGeometryUpdatePending;

    private @Nullable QWidget myContainer;
    private @Nullable QVBoxLayout myContainerLayout;

    private int avatarSize() {
        return AvatarPresentation.sizeOf(myCompact ? AvatarStyle.X_SMALL : AvatarStyle.SMALL);
    }

    private void applyCompact(QVBoxLayout layout) {
        int padding = myCompact ? 0 : DesktopQtSpace.toPixels(Space.MEDIUM);
        layout.setContentsMargins(padding, padding, padding, padding);
        layout.setSpacing(DesktopQtSpace.toPixels(myCompact ? Space.SMALL : Space.LARGE));
    }

    @Override
    public void addStyle(MessageListStyle style) {
        if (style != MessageListStyle.COMPACT || myCompact) {
            return;
        }
        myCompact = true;

        QVBoxLayout layout = myContainerLayout;
        if (layout != null && !layout.isDisposed()) {
            applyCompact(layout);
        }
        for (Item item : myItems) {
            item.updateAvatar();
        }
    }

    @Override
    protected QScrollArea createQt(QWidget parent) {
        // a list standing among other content is as tall as its messages at the width it gets - the scroll area
        // would otherwise ask for the height its wrapping labels take at their narrowest
        QScrollArea area = new QScrollArea(parent) {
            @Override
            public boolean hasHeightForWidth() {
                QWidget content = widget();
                return content != null && content.hasHeightForWidth();
            }

            @Override
            public int heightForWidth(int width) {
                QWidget content = widget();
                if (content == null || !content.hasHeightForWidth()) {
                    return super.heightForWidth(width);
                }
                return content.heightForWidth(Math.max(0, width - 2 * frameWidth())) + 2 * frameWidth();
            }

            @Override
            public QSize sizeHint() {
                QSize hint = super.sizeHint();
                if (!myCompact || !hasHeightForWidth()) {
                    return hint;
                }
                int width = width() > 0 ? width() : hint.width();
                return new QSize(hint.width(), heightForWidth(width));
            }
        };
        area.setWidgetResizable(true);
        area.setFrameShape(QFrame.Shape.NoFrame);
        area.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);

        QWidget container = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(container);
        applyCompact(layout);
        layout.addStretch(1);
        area.setWidget(container);

        QScrollBar bar = area.verticalScrollBar();
        bar.valueChanged.connect(value -> myPinned = bar.maximum() - value <= PINNED_DISTANCE);
        bar.rangeChanged.connect((minimum, maximum) -> {
            if (myPinned) {
                bar.setValue(maximum);
            }
        });

        myContainer = container;
        myContainerLayout = layout;
        myPinned = true;

        for (Item item : myItems) {
            attach(item);
        }
        return area;
    }

    private void attach(Item item) {
        QWidget container = myContainer;
        QVBoxLayout layout = myContainerLayout;
        if (container == null || layout == null || container.isDisposed()) {
            return;
        }
        item.build(container);
        layout.insertWidget(layout.count() - 1, item.myRow);
        contentChanged();
    }

    private void contentChanged() {
        QVBoxLayout layout = myContainerLayout;
        if (layout != null) {
            layout.invalidate();
        }
        if (myGeometryUpdatePending) {
            return;
        }
        myGeometryUpdatePending = true;
        QTimer.singleShot(0, () -> {
            myGeometryUpdatePending = false;
            QWidget widget = myComponent;
            while (widget != null && !widget.isDisposed()) {
                widget.updateGeometry();
                widget = widget.parentWidget();
            }
        });
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @RequiredUIAccess
    @Override
    public MessageListItem add(LocalizeValue author, String text) {
        Item item = new Item(author, text);
        myItems.add(item);
        myPinned = true;
        attach(item);
        return item;
    }

    @RequiredUIAccess
    @Override
    public void remove(MessageListItem item) {
        if (item instanceof Item own && myItems.remove(own)) {
            QWidget row = own.myRow;
            if (row != null && !row.isDisposed()) {
                QVBoxLayout layout = myContainerLayout;
                if (layout != null) {
                    layout.removeWidget(row);
                }
                row.hide();
                row.disposeLater();
            }
            for (QtComponentDelegate<?> delegate : own.myPartComponents) {
                delegate.disposeQt();
            }
            own.myPartComponents.clear();
            own.myPartLabels.clear();
            own.myRow = null;
            own.myAvatar = null;
            own.myHeader = null;
            own.myBody = null;
            own.myPartsWidget = null;
            own.myPartsLayout = null;
            contentChanged();
        }
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        for (Item item : List.copyOf(myItems)) {
            remove(item);
        }
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
