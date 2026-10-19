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
package consulo.desktop.qt.ui.impl.layout;

import consulo.desktop.qt.ui.impl.DesktopQtSpace;
import consulo.desktop.qt.ui.impl.DesktopQtStyleApplier;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.Card;
import consulo.ui.CardStyle;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.style.ComponentColors;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QFont;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.widgets.QBoxLayout;
import io.qt.widgets.QFrame;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLabel;
import io.qt.widgets.QLayout;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class DesktopQtCardImpl extends DesktopQtLayoutComponent<LayoutConstraint, DesktopQtCardImpl.Slot> implements Card {
    public enum Kind {
        MEDIA,
        PREFIX,
        HEADER,
        SUFFIX,
        CONTENT,
        FOOTER
    }

    public record Slot(Kind kind, int order) {
    }

    private static class CardFrame extends QFrame {
        CardFrame(QWidget parent) {
            super(parent);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPalette palette = palette();
            QColor border = DesktopQtStyleApplier.themeColor(ComponentColors.BORDER, palette.color(QPalette.ColorRole.Mid));

            QPainter painter = new QPainter(this);
            try {
                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                painter.setPen(new QPen(border));
                painter.drawRoundedRect(new QRectF(0.5, 0.5, width() - 1, height() - 1), 10, 10);
            }
            finally {
                painter.end();
            }
        }
    }

    private final Set<CardStyle> myStyles = EnumSet.noneOf(CardStyle.class);

    private LocalizeValue myTitle = LocalizeValue.empty();
    private LocalizeValue mySubtitle = LocalizeValue.empty();

    private @Nullable Component myMedia;
    private @Nullable Component myHeader;
    private @Nullable Component myHeaderPrefix;
    private @Nullable Component myHeaderSuffix;

    private int myOrder;

    private @Nullable QLabel myTitleLabel;
    private @Nullable QLabel mySubtitleLabel;
    private @Nullable QWidget myTitleBox;
    private @Nullable QWidget myMediaBox;
    private @Nullable QBoxLayout myMediaLayout;
    private @Nullable QBoxLayout myPrefixLayout;
    private @Nullable QBoxLayout myHeaderLayout;
    private @Nullable QBoxLayout mySuffixLayout;
    private @Nullable QBoxLayout myContentLayout;
    private @Nullable QBoxLayout myFooterLayout;

    @Override
    protected QWidget createQt(QWidget parent) {
        CardFrame frame = new CardFrame(parent);

        boolean horizontal = myStyles.contains(CardStyle.HORIZONTAL);
        boolean cover = myStyles.contains(CardStyle.COVER_MEDIA);
        int padding = DesktopQtSpace.toPixels(myStyles.contains(CardStyle.COMPACT) ? Space.MEDIUM : Space.LARGE);

        QBoxLayout root = horizontal ? new QHBoxLayout(frame) : new QVBoxLayout(frame);
        root.setContentsMargins(0, 0, 0, 0);
        root.setSpacing(0);

        QWidget mediaBox = new QWidget(frame);
        QVBoxLayout mediaLayout = new QVBoxLayout(mediaBox);
        int mediaPadding = cover ? 0 : padding;
        mediaLayout.setContentsMargins(mediaPadding, mediaPadding, horizontal ? 0 : mediaPadding, horizontal ? mediaPadding : 0);
        root.addWidget(mediaBox);
        mediaBox.setVisible(false);
        myMediaBox = mediaBox;
        myMediaLayout = mediaLayout;

        QWidget body = new QWidget(frame);
        QVBoxLayout bodyLayout = new QVBoxLayout(body);
        bodyLayout.setContentsMargins(padding, padding, padding, padding);
        bodyLayout.setSpacing(DesktopQtSpace.toPixels(Space.MEDIUM));
        root.addWidget(body, 1);

        QWidget headerRow = new QWidget(body);
        QHBoxLayout headerRowLayout = new QHBoxLayout(headerRow);
        headerRowLayout.setContentsMargins(0, 0, 0, 0);
        headerRowLayout.setSpacing(DesktopQtSpace.toPixels(Space.SMALL));
        bodyLayout.addWidget(headerRow);

        QVBoxLayout prefixLayout = new QVBoxLayout();
        prefixLayout.setContentsMargins(0, 0, 0, 0);
        headerRowLayout.addLayout(prefixLayout);
        myPrefixLayout = prefixLayout;

        QVBoxLayout headerLayout = new QVBoxLayout();
        headerLayout.setContentsMargins(0, 0, 0, 0);
        headerRowLayout.addLayout(headerLayout, 1);
        myHeaderLayout = headerLayout;

        QWidget titleBox = new QWidget(headerRow);
        QVBoxLayout titleLayout = new QVBoxLayout(titleBox);
        titleLayout.setContentsMargins(0, 0, 0, 0);
        titleLayout.setSpacing(0);
        QLabel title = new QLabel(titleBox);
        QFont font = title.font();
        font.setBold(true);
        title.setFont(font);
        QLabel subtitle = new QLabel(titleBox);
        subtitle.setForegroundRole(QPalette.ColorRole.PlaceholderText);
        titleLayout.addWidget(title);
        titleLayout.addWidget(subtitle);
        headerLayout.addWidget(titleBox);
        myTitleBox = titleBox;
        myTitleLabel = title;
        mySubtitleLabel = subtitle;

        QVBoxLayout suffixLayout = new QVBoxLayout();
        suffixLayout.setContentsMargins(0, 0, 0, 0);
        headerRowLayout.addLayout(suffixLayout);
        mySuffixLayout = suffixLayout;

        QWidget contentBox = new QWidget(body);
        QVBoxLayout contentLayout = new QVBoxLayout(contentBox);
        contentLayout.setContentsMargins(0, 0, 0, 0);
        contentLayout.setSpacing(DesktopQtSpace.toPixels(Space.SMALL));
        bodyLayout.addWidget(contentBox);
        myContentLayout = contentLayout;

        QWidget footerBox = new QWidget(body);
        QHBoxLayout footerLayout = new QHBoxLayout(footerBox);
        footerLayout.setContentsMargins(0, 0, 0, 0);
        footerLayout.setSpacing(DesktopQtSpace.toPixels(Space.SMALL));
        footerLayout.addStretch(1);
        bodyLayout.addWidget(footerBox);
        bodyLayout.addStretch(1);
        myFooterLayout = footerLayout;

        updateTexts();
        return frame;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return null;
    }

    @Override
    protected void attach(QtComponentDelegate<?> child, @Nullable Object layoutData) {
        if (!(layoutData instanceof Slot slot)) {
            return;
        }

        QWidget widget = child.toQtComponent();
        switch (slot.kind()) {
            case MEDIA -> {
                if (myMediaBox != null) {
                    myMediaBox.setVisible(true);
                }
                addTo(myMediaLayout, widget, Qt.AlignmentFlag.AlignHCenter);
            }
            case PREFIX -> addTo(myPrefixLayout, widget, Qt.AlignmentFlag.AlignTop);
            case HEADER -> {
                if (myTitleBox != null) {
                    myTitleBox.setVisible(false);
                }
                addTo(myHeaderLayout, widget, null);
            }
            case SUFFIX -> addTo(mySuffixLayout, widget, Qt.AlignmentFlag.AlignTop);
            case CONTENT -> addTo(myContentLayout, widget, null);
            case FOOTER -> {
                QBoxLayout footer = myFooterLayout;
                if (footer != null && widget != null) {
                    footer.insertWidget(footer.count() - 1, widget);
                }
            }
        }
    }

    @Override
    protected void detach(QtComponentDelegate<?> child) {
        QWidget widget = child.toQtComponent();
        if (!isAlive(widget)) {
            return;
        }

        QWidget parent = widget.parentWidget();
        if (parent != null && parent.layout() != null) {
            parent.layout().removeWidget(widget);
        }
        for (QBoxLayout layout : new QBoxLayout[]{myMediaLayout, myPrefixLayout, myHeaderLayout, mySuffixLayout, myContentLayout, myFooterLayout}) {
            if (layout != null && !layout.isDisposed()) {
                layout.removeWidget(widget);
            }
        }

        if (child == myHeader && myTitleBox != null && !myTitleBox.isDisposed()) {
            myTitleBox.setVisible(true);
        }
        if (child == myMedia && myMediaBox != null && !myMediaBox.isDisposed()) {
            myMediaBox.setVisible(false);
        }
    }

    @Override
    public void disposeQt() {
        myTitleLabel = null;
        mySubtitleLabel = null;
        myTitleBox = null;
        myMediaBox = null;
        myMediaLayout = null;
        myPrefixLayout = null;
        myHeaderLayout = null;
        mySuffixLayout = null;
        myContentLayout = null;
        myFooterLayout = null;
        super.disposeQt();
    }

    @Override
    public void addStyle(CardStyle style) {
        myStyles.add(style);
    }

    @RequiredUIAccess
    @Override
    public Card setMedia(@Nullable Component media) {
        myMedia = replace(myMedia, media, Kind.MEDIA);
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setTitle(LocalizeValue title) {
        myTitle = title;
        updateTexts();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setSubtitle(LocalizeValue subtitle) {
        mySubtitle = subtitle;
        updateTexts();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setHeader(@Nullable Component header) {
        myHeader = replace(myHeader, header, Kind.HEADER);
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setHeaderPrefix(@Nullable Component prefix) {
        myHeaderPrefix = replace(myHeaderPrefix, prefix, Kind.PREFIX);
        return this;
    }

    @RequiredUIAccess
    @Override
    public Card setHeaderSuffix(@Nullable Component suffix) {
        myHeaderSuffix = replace(myHeaderSuffix, suffix, Kind.SUFFIX);
        return this;
    }

    @Override
    public Slot convertConstraintsToLayoutData(LayoutConstraint constraint) {
        return new Slot(Kind.CONTENT, ++myOrder);
    }

    @RequiredUIAccess
    @Override
    public Card addToFooter(Component component) {
        addImpl(component, new Slot(Kind.FOOTER, ++myOrder));
        return this;
    }

    @Override
    protected int compareLayoutData(@Nullable Object first, @Nullable Object second) {
        Slot firstSlot = first instanceof Slot slot ? slot : new Slot(Kind.CONTENT, 0);
        Slot secondSlot = second instanceof Slot slot ? slot : new Slot(Kind.CONTENT, 0);
        int byKind = firstSlot.kind().compareTo(secondSlot.kind());
        return byKind != 0 ? byKind : Integer.compare(firstSlot.order(), secondSlot.order());
    }

    @RequiredUIAccess
    private @Nullable Component replace(@Nullable Component old, @Nullable Component component, Kind kind) {
        if (old != null) {
            remove(old);
        }
        if (component != null) {
            addImpl(component, new Slot(kind, ++myOrder));
        }
        return component;
    }

    private void updateTexts() {
        QLabel title = myTitleLabel;
        QLabel subtitle = mySubtitleLabel;
        if (title == null || subtitle == null || title.isDisposed() || subtitle.isDisposed()) {
            return;
        }

        title.setText(myTitle.get());
        title.setVisible(myTitle != LocalizeValue.empty());
        subtitle.setText(mySubtitle.get());
        subtitle.setVisible(mySubtitle != LocalizeValue.empty());
    }

    private static void addTo(@Nullable QBoxLayout layout, @Nullable QWidget widget, Qt.@Nullable AlignmentFlag alignment) {
        if (layout == null || widget == null) {
            return;
        }
        if (alignment == null) {
            layout.addWidget(widget);
        }
        else {
            layout.addWidget(widget, 0, alignment);
        }
    }
}
