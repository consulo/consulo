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
import consulo.desktop.qt.ui.impl.base.DesktopQtWrappingLabel;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.localize.LocalizeValue;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.messageList.MessageListPresentation;
import consulo.ui.messageList.MessageList;
import consulo.ui.messageList.MessageListItem;
import consulo.ui.messageList.MessageListItemStyle;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StyleManager;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QFont;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPixmap;
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
    private static final int AVATAR_SIZE = 28;
    private static final int PINNED_DISTANCE = 24;

    private class Item implements MessageListItem {
        private final LocalizeValue myAuthor;
        private String myText;
        private @Nullable Instant myTime;
        private @Nullable Image myAuthorImage;
        private MessageListItemStyle myStyle = MessageListItemStyle.OTHER;

        private @Nullable QWidget myRow;
        private @Nullable QLabel myAvatar;
        private @Nullable QLabel myHeader;
        private @Nullable QLabel myBody;

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
            avatar.setFixedSize(AVATAR_SIZE, AVATAR_SIZE);
            rowLayout.addWidget(avatar, 0, Qt.AlignmentFlag.AlignTop);

            QWidget content = new QWidget(row);
            QVBoxLayout contentLayout = new QVBoxLayout(content);
            contentLayout.setContentsMargins(0, 0, 0, 0);
            contentLayout.setSpacing(DesktopQtSpace.toPixels(Space.X_SMALL));

            QLabel header = new QLabel(content);
            header.setTextFormat(Qt.TextFormat.RichText);
            contentLayout.addWidget(header);

            DesktopQtWrappingLabel body = new DesktopQtWrappingLabel(content);
            body.setWordWrap(true);
            body.setOpenExternalLinks(true);
            body.setTextInteractionFlags(Qt.TextInteractionFlag.TextBrowserInteraction);
            contentLayout.addWidget(body);

            rowLayout.addWidget(content, 1);

            myRow = row;
            myAvatar = avatar;
            myHeader = header;
            myBody = body;

            updateAvatar();
            updateHeader();
            updateBody();
        }

        void updateAvatar() {
            QLabel avatar = myAvatar;
            if (avatar == null) {
                return;
            }
            Image image = myAuthorImage;
            avatar.setPixmap(avatarPixmap(myAuthor.get(), image, avatar.devicePixelRatioF(), avatar.palette()));
            avatar.setVisible(MessageListPresentation.showsAvatar(myStyle));
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
                avatar.setVisible(MessageListPresentation.showsAvatar(myStyle));
            }
            QLabel body = myBody;
            if (body == null) {
                return;
            }
            if (myMarkdown) {
                body.setTextFormat(Qt.TextFormat.RichText);
                body.setText(markdownToHtml(myText));
            }
            else {
                body.setTextFormat(Qt.TextFormat.PlainText);
                body.setText(myText);
            }
            body.setVisible(!myText.isEmpty());

            QColor color = switch (myStyle) {
                case ERROR -> DesktopQtStyleApplier.themeColor(ComponentColors.ERROR_FOREGROUND, body.palette().color(QPalette.ColorRole.WindowText));
                case SYSTEM -> DesktopQtStyleApplier.themeColor(ComponentColors.INFO_FOREGROUND, body.palette().color(QPalette.ColorRole.PlaceholderText));
                default -> null;
            };
            body.setStyleSheet(color == null ? "" : "QLabel { color: " + color.name() + "; }");
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
    private boolean myPinned = true;

    private @Nullable QWidget myContainer;
    private @Nullable QVBoxLayout myContainerLayout;

    @Override
    protected QScrollArea createQt(QWidget parent) {
        QScrollArea area = new QScrollArea(parent);
        area.setWidgetResizable(true);
        area.setFrameShape(QFrame.Shape.NoFrame);
        area.setHorizontalScrollBarPolicy(Qt.ScrollBarPolicy.ScrollBarAlwaysOff);

        QWidget container = new QWidget();
        QVBoxLayout layout = new QVBoxLayout(container);
        int padding = DesktopQtSpace.toPixels(Space.MEDIUM);
        layout.setContentsMargins(padding, padding, padding, padding);
        layout.setSpacing(DesktopQtSpace.toPixels(Space.LARGE));
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
    }

    private static QPixmap avatarPixmap(String name, @Nullable Image image, double ratio, QPalette palette) {
        int pixels = (int) Math.ceil(AVATAR_SIZE * ratio);
        QPixmap pixmap = new QPixmap(pixels, pixels);
        pixmap.setDevicePixelRatio(ratio);
        pixmap.fill(new QColor(Qt.GlobalColor.transparent));

        boolean dark = StyleManager.get().getCurrentStyle().isDark();
        QColor initialsColor = QColor.fromHsvF(
            MessageListPresentation.hueOf(name),
            MessageListPresentation.saturation(dark),
            MessageListPresentation.brightness(dark)
        );
        QColor background = DesktopQtStyleApplier.themeColor(ComponentColors.COMPONENT_BACKGROUND, palette.color(QPalette.ColorRole.Base));
        QColor border = DesktopQtStyleApplier.themeColor(ComponentColors.BORDER, palette.color(QPalette.ColorRole.Mid));

        QPainter painter = new QPainter(pixmap);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setPen(border);
            painter.setBrush(background);
            QRectF rect = new QRectF(0.5, 0.5, AVATAR_SIZE - 1, AVATAR_SIZE - 1);
            painter.drawEllipse(rect);

            if (image != null) {
                QPixmap icon = DesktopQtImage.toQPixmap(image);
                double width = icon.width() / icon.devicePixelRatio();
                double height = icon.height() / icon.devicePixelRatio();
                painter.drawPixmap(new QRectF((AVATAR_SIZE - width) / 2, (AVATAR_SIZE - height) / 2, width, height), icon, new QRectF(0, 0, icon.width(), icon.height()));
            }
            else {
                QFont font = painter.font();
                font.setPixelSize((int) (AVATAR_SIZE * 0.42));
                painter.setFont(font);
                painter.setPen(initialsColor);
                painter.drawText(rect, Qt.AlignmentFlag.AlignCenter.value(), MessageListPresentation.initialsOf(name));
            }
        }
        finally {
            painter.end();
        }
        return pixmap;
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
                row.hide();
                row.disposeLater();
            }
            own.myRow = null;
            own.myAvatar = null;
            own.myHeader = null;
            own.myBody = null;
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
}
