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

import consulo.desktop.qt.ui.impl.DesktopQtCurrentInput;
import consulo.desktop.qt.ui.impl.DesktopQtGeometry;
import consulo.desktop.qt.ui.impl.DesktopQtSpace;
import consulo.desktop.qt.ui.impl.DesktopQtStyleApplier;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.avatar.DesktopQtAvatarPainter;
import consulo.desktop.qt.ui.impl.base.DesktopQtWrappingLabel;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.ui.AvatarStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.impl.avatar.AvatarPresentation;
import consulo.ui.impl.messageList.MessageListPresentation;
import consulo.ui.messageList.AsyncHtmlImageLoader;
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
import io.qt.gui.QResizeEvent;
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
    private static final int ESTIMATED_LINE_LENGTH = 100;
    private static final int ESTIMATED_LINE_SPACING = 18;
    private static final int DEFAULT_WIDTH = 400;
    private static final int MAX_ESTIMATED_LINES = 200;
    /**
     * How often a streamed answer is rendered again while it grows, in milliseconds.
     */
    private static final int APPEND_RENDER_DELAY = 100;

    private static final int PINNED_DISTANCE = 24;

    private class Item implements MessageListItem {
        private final LocalizeValue myAuthor;
        private String myText;
        private @Nullable Instant myTime;
        private @Nullable Image myAuthorImage;
        private MessageListItemStyle myStyle = MessageListItemStyle.OTHER;
        private List<MessageListItemPart> myParts = List.of();

        private @Nullable QWidget myRow;
        /**
         * The height the row took at {@link #myMeasuredWidth} when it was last built, -1 while never.
         */
        private int myMeasuredHeight = -1;
        private int myMeasuredWidth = -1;
        private @Nullable QLabel myAvatar;
        private @Nullable QLabel myHeader;
        private @Nullable QHBoxLayout myHeaderLayout;
        private @Nullable Component myActionsComponent;
        private @Nullable QLabel myBody;
        private @Nullable QWidget myPartsWidget;
        private @Nullable QVBoxLayout myPartsLayout;
        private final List<QLabel> myPartLabels = new ArrayList<>();
        private final List<QtComponentDelegate<?>> myPartComponents = new ArrayList<>();
        private boolean myAppendPending;
        private boolean myWriting;
        private LocalizeValue myWritingStatus = LocalizeValue.empty();
        private @Nullable QLabel myWritingLabel;
        private @Nullable QTimer myWritingTimer;
        private int myWritingFrame;

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

            QLabel writing = new QLabel(content);
            writing.setTextFormat(Qt.TextFormat.RichText);
            contentLayout.addWidget(writing);

            rowLayout.addWidget(content, 1);

            myRow = row;
            myAvatar = avatar;
            myHeader = header;
            myHeaderLayout = headerLayout;
            myBody = body;
            myPartsWidget = parts;
            myPartsLayout = partsLayout;
            myWritingLabel = writing;

            updateAvatar();
            updateHeader();
            attachActions();
            rebuildParts();
            updateBody();
            updateWriting();
        }

        /**
         * Three dots light one after another while the message is written, the status after them.
         */
        void updateWriting() {
            QLabel label = myWritingLabel;
            if (label == null || label.isDisposed()) {
                return;
            }
            if (!myWriting) {
                stopWritingTimer();
                label.setVisible(false);
                contentChanged();
                return;
            }
            if (myWritingTimer == null) {
                QTimer timer = new QTimer(label);
                timer.setInterval(400);
                timer.timeout.connect(() -> {
                    myWritingFrame = (myWritingFrame + 1) % 3;
                    renderWriting();
                });
                timer.start();
                myWritingTimer = timer;
            }
            label.setVisible(true);
            renderWriting();
            contentChanged();
        }

        private void renderWriting() {
            QLabel label = myWritingLabel;
            if (label == null || label.isDisposed()) {
                return;
            }
            QColor muted = DesktopQtStyleApplier.themeColor(ComponentColors.INFO_FOREGROUND, label.palette().color(QPalette.ColorRole.PlaceholderText));
            StringBuilder dots = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                dots.append(i == myWritingFrame ? "&#9679;" : "&#9675;");
            }
            String status = myWritingStatus.get();
            label.setText("<span style='color:" + muted.name() + "'>" + dots
                + (status.isEmpty() ? "" : "&nbsp;&nbsp;" + escape(status)) + "</span>");
        }

        private void stopWritingTimer() {
            QTimer timer = myWritingTimer;
            myWritingTimer = null;
            if (timer != null && !timer.isDisposed()) {
                timer.stop();
                timer.disposeLater();
            }
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
                updateWriting();
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
            renderWriting();
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
            body.setOpenExternalLinks(false);
            body.setTextInteractionFlags(Qt.TextInteractionFlag.TextBrowserInteraction);
            body.linkActivated.connect(href -> linkActivated(body, href));
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
            avatar.setVisible(MessageListPresentation.showsAvatar(myStyle, myShowOwnAvatar, myShowAvatars));
            DesktopQtAvatarPainter.repaintWhileLoading(avatar, image, this::updateAvatar);
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
                avatar.setVisible(MessageListPresentation.showsAvatar(myStyle, myShowOwnAvatar, myShowAvatars));
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
                label.setText(myImages.resolveImages(markdownToHtml(text), label.width()));
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
            if (!myParts.isEmpty()) {
                setText(myText + text);
                return;
            }
            // a streamed answer grows by a few characters at a time - it is rendered again at most so often
            myText = myText + text;
            if (!myAppendPending) {
                myAppendPending = true;
                QTimer.singleShot(APPEND_RENDER_DELAY, () -> {
                    myAppendPending = false;
                    updateBody();
                });
            }
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
    private final DesktopQtMessageListImages myImages = new DesktopQtMessageListImages(runnable -> getUIAccess().give(runnable), this::refreshImages);
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
    private boolean myShowAvatars = true;
    private boolean myCompact;
    private boolean myPinned = true;
    private boolean myGeometryUpdatePending;
    private boolean myRenderPending;

    private @Nullable Container myContainer;
    private int myLineSpacing;

    private int avatarSize() {
        return AvatarPresentation.sizeOf(myCompact ? AvatarStyle.X_SMALL : AvatarStyle.SMALL);
    }

    private int padding() {
        return myCompact ? 0 : DesktopQtSpace.toPixels(Space.MEDIUM);
    }

    private int gap() {
        return DesktopQtSpace.toPixels(myCompact ? Space.SMALL : Space.LARGE);
    }

    /**
     * Holds the rows of the messages near the viewport, each at the offset of its message - as tall as all the messages
     * together, at the width it gets.
     */
    private final class Container extends QWidget {
        @Override
        public boolean hasHeightForWidth() {
            return true;
        }

        @Override
        public int heightForWidth(int width) {
            return totalHeight(width);
        }

        @Override
        public QSize sizeHint() {
            int width = width() > 0 ? width() : DEFAULT_WIDTH;
            return new QSize(width, totalHeight(width));
        }

        @Override
        protected void resizeEvent(QResizeEvent event) {
            super.resizeEvent(event);
            scheduleLayout();
        }
    }

    @RequiredUIAccess
    @Override
    public void addStyle(MessageListStyle style) {
        if (style != MessageListStyle.COMPACT || myCompact) {
            return;
        }
        myCompact = true;

        for (Item item : myItems) {
            item.updateAvatar();
            item.myMeasuredHeight = -1;
        }
        contentChanged();
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

        Container container = new Container();
        area.setWidget(container);

        QScrollBar bar = area.verticalScrollBar();
        bar.valueChanged.connect(value -> {
            myPinned = bar.maximum() - value <= PINNED_DISTANCE;
            scheduleLayout();
        });
        bar.rangeChanged.connect((minimum, maximum) -> {
            if (myPinned) {
                bar.setValue(maximum);
            }
            scheduleLayout();
        });

        myContainer = container;
        myPinned = true;
        contentChanged();
        return area;
    }

    /**
     * The height of the message at the width - measured from its row while it has one, as measured last before, or
     * estimated when it never had one.
     */
    private int heightOf(Item item, int width) {
        QWidget row = item.myRow;
        if (row != null && !row.isDisposed() && width > 0) {
            int height = row.hasHeightForWidth() ? row.heightForWidth(width) : row.sizeHint().height();
            item.myMeasuredHeight = height;
            item.myMeasuredWidth = width;
            return height;
        }
        if (item.myMeasuredHeight >= 0) {
            return item.myMeasuredHeight;
        }
        return (estimateLines(item.myText) + 1) * lineSpacing() + DesktopQtSpace.toPixels(Space.X_SMALL);
    }

    /**
     * Asked of the font once - a call into qt costs, and the estimate asks it for every message never shown.
     */
    private int lineSpacing() {
        int lineSpacing = myLineSpacing;
        if (lineSpacing <= 0) {
            Container container = myContainer;
            if (container == null || container.isDisposed()) {
                return ESTIMATED_LINE_SPACING;
            }
            lineSpacing = container.fontMetrics().lineSpacing();
            myLineSpacing = lineSpacing;
        }
        return lineSpacing;
    }

    private int totalHeight(int width) {
        int rowWidth = Math.max(0, width - 2 * padding());
        int height = 2 * padding();
        for (int i = 0; i < myItems.size(); i++) {
            height += heightOf(myItems.get(i), rowWidth) + (i > 0 ? gap() : 0);
        }
        return height;
    }

    private static int estimateLines(String text) {
        int lines = 1;
        int lineLength = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n' || ++lineLength > ESTIMATED_LINE_LENGTH) {
                lines++;
                lineLength = 0;
            }
        }
        return Math.min(lines, MAX_ESTIMATED_LINES);
    }

    private void scheduleLayout() {
        if (myRenderPending) {
            return;
        }
        myRenderPending = true;
        QTimer.singleShot(0, () -> {
            myRenderPending = false;
            layoutRows();
        });
    }

    /**
     * Builds the rows of the messages from a screen above the viewport to a screen below it, drops the ones farther
     * than three screens and places the rest at their offsets - what the viewport shows stays in place while heights
     * become known.
     */
    private void layoutRows() {
        QScrollArea area = myComponent;
        Container container = myContainer;
        if (area == null || area.isDisposed() || container == null || container.isDisposed()) {
            return;
        }
        int width = container.width() > 0 ? container.width() : area.viewport().width();
        int rowWidth = Math.max(0, width - 2 * padding());
        if (rowWidth <= 0) {
            return;
        }
        QScrollBar bar = area.verticalScrollBar();
        int viewTop = bar.value();
        int viewHeight = Math.max(area.viewport().height(), 1);
        int keepTop = viewTop - viewHeight;
        int keepBottom = viewTop + viewHeight * 2;
        int dropTop = viewTop - viewHeight * 3;
        int dropBottom = viewTop + viewHeight * 4;

        int shiftAbove = 0;
        int y = padding();
        for (Item item : myItems) {
            int before = heightOf(item, rowWidth);
            int top = y;
            int bottom = y + before;
            boolean built = item.myRow != null && !item.myRow.isDisposed();
            if (!built && bottom >= keepTop && top <= keepBottom) {
                item.build(container);
                QWidget row = item.myRow;
                if (row != null) {
                    row.show();
                }
            }
            else if (built && (bottom < dropTop || top > dropBottom)) {
                unbuild(item);
            }
            int after = heightOf(item, rowWidth);
            if (after != before && top < viewTop) {
                shiftAbove += after - before;
            }
            QWidget row = item.myRow;
            if (row != null && !row.isDisposed()) {
                row.setGeometry(padding(), y, rowWidth, after);
            }
            y += after + gap();
        }
        int total = y - (myItems.isEmpty() ? 0 : gap()) + padding();
        if (container.minimumHeight() != total) {
            container.setMinimumHeight(total);
            container.updateGeometry();
        }
        if (!myPinned && shiftAbove != 0) {
            bar.setValue(bar.value() + shiftAbove);
        }
    }

    /**
     * Drops the row of the message - the components of the caller in it are given back, to come again with the row.
     */
    private void unbuild(Item item) {
        QWidget row = item.myRow;
        if (row != null && !row.isDisposed()) {
            row.hide();
            row.disposeLater();
        }
        for (QtComponentDelegate<?> delegate : item.myPartComponents) {
            delegate.disposeQt();
        }
        item.myPartComponents.clear();
        item.myPartLabels.clear();
        item.myRow = null;
        item.myAvatar = null;
        item.myHeader = null;
        item.myHeaderLayout = null;
        item.myBody = null;
        item.myPartsWidget = null;
        item.myPartsLayout = null;
        item.stopWritingTimer();
        item.myWritingLabel = null;
    }

    private void contentChanged() {
        scheduleLayout();
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
        contentChanged();
        return item;
    }

    @RequiredUIAccess
    @Override
    public void remove(MessageListItem item) {
        if (item instanceof Item own && myItems.remove(own)) {
            unbuild(own);
            contentChanged();
        }
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        for (Item item : List.copyOf(myItems)) {
            unbuild(item);
        }
        myItems.clear();
        myPinned = true;
        contentChanged();
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

    @RequiredUIAccess
    @Override
    public void setShowAvatars(boolean showAvatars) {
        if (myShowAvatars == showAvatars) {
            return;
        }
        myShowAvatars = showAvatars;
        for (Item item : myItems) {
            item.updateBody();
        }
    }

    @Override
    public boolean isShowAvatars() {
        return myShowAvatars;
    }

    @RequiredUIAccess
    @Override
    public void setImageLoader(@Nullable AsyncHtmlImageLoader imageLoader) {
        if (myImages.getLoader() == imageLoader) {
            return;
        }
        myImages.setLoader(imageLoader);
        refreshImages();
    }

    @Override
    public @Nullable AsyncHtmlImageLoader getImageLoader() {
        return myImages.getLoader();
    }

    private void refreshImages() {
        if (!myMarkdown) {
            return;
        }
        for (Item item : myItems) {
            item.updateBody();
        }
        QWidget container = myContainer;
        if (container != null && !container.isDisposed()) {
            DesktopQtGeometry.invalidateHierarchy(container);
        }
    }

    @RequiredUIAccess
    private void linkActivated(QLabel body, String href) {
        if (href.isEmpty()) {
            return;
        }
        if (myDataObject.hasListeners(HyperlinkEvent.class)) {
            getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, href, DesktopQtCurrentInput.current(body)));
        }
        else if (href.contains("://")) {
            Platform.current().openInBrowser(href);
        }
    }
}
