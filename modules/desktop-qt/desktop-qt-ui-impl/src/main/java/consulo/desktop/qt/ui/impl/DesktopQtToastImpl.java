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
package consulo.desktop.qt.ui.impl;

import consulo.desktop.qt.ui.impl.base.DesktopQtImageWidget;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Toast;
import consulo.ui.ToastStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.impl.ToastPresentation;
import consulo.ui.internal.ToastInternal;
import consulo.ui.style.StyleManager;
import consulo.util.lang.xml.XmlStringUtil;
import io.qt.core.QEvent;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QEnterEvent;
import io.qt.gui.QFont;
import io.qt.gui.QImage;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.gui.QRegion;
import io.qt.gui.QResizeEvent;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QLabel;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QVBoxLayout;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-16
 */
public class DesktopQtToastImpl extends QtComponentDelegate<QWidget> implements ToastInternal, DesktopQtIconOwner {
    private static final int ARC = 20;
    private static final int SHADOW_SIZE = 10;
    private static final int SHADOW_BLUR = 8;
    private static final int SHADOW_OFFSET = 2;

    private record Action(LocalizeValue text, Runnable action) {
    }

    private class ToastWidget extends QWidget {
        private @Nullable QImage myShadowImage;

        ToastWidget(QWidget parent) {
            super(parent);
        }

        @Override
        protected void resizeEvent(QResizeEvent event) {
            super.resizeEvent(event);
            myShadowImage = null;
        }

        @Override
        protected void enterEvent(QEnterEvent event) {
            super.enterEvent(event);
            setHovered(true);
        }

        @Override
        protected void leaveEvent(QEvent event) {
            super.leaveEvent(event);
            setHovered(false);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            int shadow = shadowSize();
            double width = width() - 2 * shadow;
            double height = height() - 2 * shadow;
            if (width <= 0 || height <= 0) {
                return;
            }

            QPalette palette = palette();
            QColor background = DesktopQtStyleApplier.themeColor(ToastPresentation.background(myStyle), palette.color(QPalette.ColorRole.Window));
            QColor border = DesktopQtStyleApplier.themeColor(ToastPresentation.border(myStyle), palette.color(QPalette.ColorRole.Mid));

            QPainter painter = new QPainter(this);
            try {
                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                if (myShadow) {
                    if (myShadowImage == null) {
                        myShadowImage = createShadow(width(), height());
                    }
                    painter.drawImage(0, 0, myShadowImage);
                }

                if (myLeftLine) {
                    double lineWidth = ToastPresentation.LEFT_LINE_WIDTH;
                    QPainterPath line = new QPainterPath();
                    line.addRoundedRect(new QRectF(shadow, shadow, lineWidth, height), lineWidth / 2, lineWidth / 2);
                    painter.setPen(Qt.PenStyle.NoPen);
                    painter.setBrush(new QBrush(border));
                    painter.drawPath(line);
                    return;
                }

                QPainterPath shape = new QPainterPath();
                shape.addRoundedRect(new QRectF(shadow + 0.5, shadow + 0.5, width - 1, height - 1), ARC / 2.0, ARC / 2.0);

                if (isGlassy()) {
                    paintBackdrop(painter, shape, new QRect(x() + shadow, y() + shadow, (int) width, (int) height));
                    background.setAlphaF(background.alphaF() * ToastPresentation.GLASSY_OPACITY);
                }

                painter.setPen(Qt.PenStyle.NoPen);
                painter.setBrush(new QBrush(background));
                painter.drawPath(shape);

                painter.setBrush(Qt.BrushStyle.NoBrush);
                painter.setPen(new QPen(border, 1));
                painter.drawPath(shape);
            }
            finally {
                painter.end();
            }
        }

        void dropShadow() {
            myShadowImage = null;
        }

        /**
         * What lies under the card are the other children of the host - never the host itself, it holds this
         * toast. Other toasts are left out.
         */
        private void paintBackdrop(QPainter painter, QPainterPath shape, QRect card) {
            QWidget host = parentWidget();
            if (host == null) {
                return;
            }

            int margin = 2 * ToastPresentation.GLASSY_BLUR;
            QRect area = card.adjusted(-margin, -margin, margin, margin).intersected(host.rect());
            if (area.isEmpty()) {
                return;
            }

            QImage snapshot = new QImage(area.width(), area.height(), QImage.Format.Format_ARGB32_Premultiplied);
            snapshot.fill(palette().color(QPalette.ColorRole.Window));
            QPainter snapshotPainter = new QPainter(snapshot);
            try {
                for (QObject child : host.children()) {
                    if (child instanceof QWidget widget && !(widget instanceof ToastWidget) && !widget.isWindow() && widget.isVisible()) {
                        QRect visible = widget.geometry().intersected(area);
                        if (!visible.isEmpty()) {
                            widget.render(
                                snapshotPainter,
                                new QPoint(visible.x() - area.x(), visible.y() - area.y()),
                                new QRegion(visible.translated(-widget.x(), -widget.y()))
                            );
                        }
                    }
                }
            }
            finally {
                snapshotPainter.end();
            }

            QImage blurred = DesktopQtBlur.blur(snapshot, 2 * ToastPresentation.GLASSY_BLUR);
            painter.save();
            try {
                painter.setClipPath(shape);
                painter.drawImage(area.x() - x(), area.y() - y(), blurred);
            }
            finally {
                painter.restore();
            }
        }
    }

    private @Nullable ToastStyle myStyle;
    private boolean myGlassy;
    private boolean myLeftLine;
    private @Nullable Image myIcon;
    private LocalizeValue myTitle = LocalizeValue.empty();
    private LocalizeValue myText = LocalizeValue.empty();
    private boolean myHtml;
    private final List<Action> myActions = new ArrayList<>();
    private @Nullable Component myToolbar;
    private boolean myShadow;
    private boolean myHovered;

    private @Nullable QHBoxLayout myRow;
    private @Nullable DesktopQtImageWidget myIconWidget;
    private @Nullable QLabel myTitleLabel;
    private @Nullable QLabel myTextLabel;
    private @Nullable QWidget myActionsWidget;
    private @Nullable QHBoxLayout myActionsLayout;
    private @Nullable QVBoxLayout myToolbarLayout;

    @Override
    protected QWidget createQt(QWidget parent) {
        return new ToastWidget(parent);
    }

    @Override
    protected void initialize(QWidget component) {
        QHBoxLayout row = new QHBoxLayout(component);
        row.setSpacing(8);
        myRow = row;
        updateMargins();

        QVBoxLayout iconColumn = new QVBoxLayout();
        iconColumn.setContentsMargins(0, 2, 0, 0);
        myIconWidget = new DesktopQtImageWidget(component);
        iconColumn.addWidget(myIconWidget);
        iconColumn.addStretch();
        row.addLayout(iconColumn);

        QVBoxLayout center = new QVBoxLayout();
        center.setSpacing(2);

        myTitleLabel = new QLabel(component);
        myTitleLabel.setTextFormat(Qt.TextFormat.PlainText);
        myTitleLabel.setWordWrap(true);
        QFont titleFont = myTitleLabel.font();
        titleFont.setBold(true);
        myTitleLabel.setFont(titleFont);
        center.addWidget(myTitleLabel);

        myTextLabel = new QLabel(component);
        myTextLabel.setWordWrap(true);
        QLabel textLabel = myTextLabel;
        textLabel.linkActivated.connect(href -> getListenerDispatcher(HyperlinkEvent.class)
            .onEvent(new HyperlinkEvent(DesktopQtToastImpl.this, href, DesktopQtCurrentInput.current(textLabel))));
        center.addWidget(myTextLabel);

        myActionsWidget = new QWidget(component);
        myActionsLayout = new QHBoxLayout(myActionsWidget);
        myActionsLayout.setContentsMargins(0, 4, 0, 0);
        myActionsLayout.setSpacing(14);
        myActionsLayout.addStretch();
        center.addWidget(myActionsWidget);

        row.addLayout(center, 1);

        myToolbarLayout = new QVBoxLayout();
        myToolbarLayout.setContentsMargins(0, 0, 0, 0);
        myToolbarLayout.addStretch();
        row.addLayout(myToolbarLayout);

        updateIcon();
        updateTitle();
        updateText();
        for (Action action : myActions) {
            addActionLink(action);
        }
        updateActionsVisibility();
        bindToolbar();
    }

    @Override
    public void addStyle(ToastStyle style) {
        if (style == ToastStyle.GLASSY) {
            myGlassy = true;
            if (toQtComponent() instanceof ToastWidget widget) {
                widget.dropShadow();
            }
        }
        else if (style == ToastStyle.LEFT_LINE) {
            myLeftLine = true;
            updateMargins();
            updateIcon();
        }
        else {
            myStyle = style;
            updateIcon();
        }
        QWidget component = toQtComponent();
        if (component != null) {
            component.update();
        }
    }

    private boolean isGlassy() {
        return myGlassy && myShadow;
    }

    @RequiredUIAccess
    @Override
    public Toast setTitle(LocalizeValue title) {
        myTitle = title;
        updateTitle();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setText(LocalizeValue text) {
        myText = text;
        myHtml = false;
        updateText();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setHtmlText(LocalizeValue html) {
        myText = html;
        myHtml = true;
        updateText();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setIcon(@Nullable Image icon) {
        myIcon = icon;
        updateIcon();
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast addAction(LocalizeValue text, @RequiredUIAccess Runnable action) {
        Action entry = new Action(text, action);
        myActions.add(entry);
        if (myActionsLayout != null) {
            addActionLink(entry);
            updateActionsVisibility();
        }
        return this;
    }

    @RequiredUIAccess
    @Override
    public Toast setToolbarComponent(@Nullable Component toolbar) {
        Component old = myToolbar;
        if (old == toolbar) {
            return this;
        }

        if (old instanceof QtComponentDelegate<?> delegate) {
            QWidget widget = delegate.toQtComponent();
            if (widget != null && !widget.isDisposed() && myToolbarLayout != null) {
                myToolbarLayout.removeWidget(widget);
                widget.hide();
            }
            delegate.setParent(null);
        }

        myToolbar = toolbar;
        bindToolbar();
        return this;
    }

    @RequiredUIAccess
    @Override
    public void setShadow(boolean shadow) {
        myShadow = shadow;
        if (toQtComponent() instanceof ToastWidget widget) {
            widget.dropShadow();
        }
        updateMargins();
        QWidget component = toQtComponent();
        if (component != null) {
            component.update();
        }
    }

    @Override
    public void refreshIcons() {
        updateIcon();
    }

    @Override
    public void disposeQt() {
        myRow = null;
        myIconWidget = null;
        myTitleLabel = null;
        myTextLabel = null;
        myActionsWidget = null;
        myActionsLayout = null;
        myToolbarLayout = null;

        super.disposeQt();
    }

    private int shadowSize() {
        return myShadow ? SHADOW_SIZE : 0;
    }

    private void updateMargins() {
        QHBoxLayout row = myRow;
        if (row == null) {
            return;
        }
        int shadow = shadowSize();
        if (myLeftLine) {
            row.setContentsMargins(ToastPresentation.LEFT_LINE_WIDTH + ToastPresentation.LEFT_LINE_GAP + shadow, 2 + shadow, shadow, 2 + shadow);
            return;
        }
        row.setContentsMargins(10 + shadow, 8 + shadow, 8 + shadow, 8 + shadow);
    }

    private void updateIcon() {
        DesktopQtImageWidget iconWidget = myIconWidget;
        if (iconWidget == null || iconWidget.isDisposed()) {
            return;
        }
        Image icon = myIcon != null ? myIcon : myLeftLine ? null : ToastPresentation.icon(myStyle);
        iconWidget.setImage(icon);
        iconWidget.setVisible(icon != null);
    }

    private void updateTitle() {
        QLabel label = myTitleLabel;
        if (label == null || label.isDisposed()) {
            return;
        }
        label.setText(myTitle.get());
        label.setVisible(myTitle != LocalizeValue.empty());
    }

    private void updateText() {
        QLabel label = myTextLabel;
        if (label == null || label.isDisposed()) {
            return;
        }
        label.setTextFormat(myHtml ? Qt.TextFormat.RichText : Qt.TextFormat.PlainText);
        if (myHtml) {
            label.setTextInteractionFlags(Qt.TextInteractionFlag.LinksAccessibleByMouse, Qt.TextInteractionFlag.LinksAccessibleByKeyboard);
        }
        else {
            label.setTextInteractionFlags(Qt.TextInteractionFlag.NoTextInteraction);
        }
        label.setText(myText.get());
        label.setVisible(myText != LocalizeValue.empty());
    }

    private void addActionLink(Action action) {
        QHBoxLayout layout = myActionsLayout;
        QWidget host = myActionsWidget;
        if (layout == null || host == null) {
            return;
        }
        QLabel link = new QLabel(host);
        link.setTextFormat(Qt.TextFormat.RichText);
        link.setTextInteractionFlags(Qt.TextInteractionFlag.LinksAccessibleByMouse, Qt.TextInteractionFlag.LinksAccessibleByKeyboard);
        link.setText("<a href=\"action\">" + XmlStringUtil.escapeText(action.text().get()) + "</a>");
        link.linkActivated.connect(href -> action.action().run());
        layout.insertWidget(layout.count() - 1, link);
    }

    private void updateActionsVisibility() {
        QWidget widget = myActionsWidget;
        if (widget != null && !widget.isDisposed()) {
            widget.setVisible(!myActions.isEmpty());
        }
    }

    private void bindToolbar() {
        QWidget component = toQtComponent();
        QVBoxLayout layout = myToolbarLayout;
        if (component == null || layout == null || !(myToolbar instanceof QtComponentDelegate<?> delegate)) {
            return;
        }

        delegate.setParent(this);
        delegate.bind(component, null);

        QWidget widget = delegate.toQtComponent();
        if (widget != null) {
            QSizePolicy policy = widget.sizePolicy();
            policy.setRetainSizeWhenHidden(true);
            widget.setSizePolicy(policy);
            layout.insertWidget(0, widget);
            widget.setVisible(myHovered);
        }
    }

    private void setHovered(boolean hovered) {
        myHovered = hovered;
        QWidget widget = myToolbar instanceof QtComponentDelegate<?> delegate ? delegate.toQtComponent() : null;
        if (widget != null && !widget.isDisposed()) {
            widget.setVisible(hovered);
        }
    }

    private QImage createShadow(int width, int height) {
        int pad = SHADOW_BLUR * 2;
        QImage mask = new QImage(width + 2 * pad, height + 2 * pad, QImage.Format.Format_ARGB32_Premultiplied);
        mask.fill(Qt.GlobalColor.transparent);
        QPainter painter = new QPainter(mask);
        try {
            painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
            painter.setPen(Qt.PenStyle.NoPen);
            painter.setBrush(new QBrush(new QColor(0, 0, 0, StyleManager.get().getCurrentStyle().isDark() ? 140 : 70)));
            QPainterPath path = new QPainterPath();
            path.addRoundedRect(new QRectF(pad + SHADOW_SIZE, pad + SHADOW_SIZE + SHADOW_OFFSET, width - 2 * SHADOW_SIZE, height - 2 * SHADOW_SIZE), ARC / 2.0, ARC / 2.0);
            painter.drawPath(path);
        }
        finally {
            painter.end();
        }

        QImage blurred = DesktopQtBlur.blur(mask, SHADOW_BLUR * 2);
        if (isGlassy()) {
            QPainter cut = new QPainter(blurred);
            try {
                cut.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                cut.setCompositionMode(QPainter.CompositionMode.CompositionMode_Clear);
                QPainterPath card = new QPainterPath();
                card.addRoundedRect(
                    new QRectF(pad + SHADOW_SIZE + 1, pad + SHADOW_SIZE + 1, width - 2 * SHADOW_SIZE - 2, height - 2 * SHADOW_SIZE - 2),
                    ARC / 2.0,
                    ARC / 2.0
                );
                cut.fillPath(card, new QBrush(new QColor(0, 0, 0)));
            }
            finally {
                cut.end();
            }
        }
        return blurred.copy(pad, pad, width, height);
    }
}
