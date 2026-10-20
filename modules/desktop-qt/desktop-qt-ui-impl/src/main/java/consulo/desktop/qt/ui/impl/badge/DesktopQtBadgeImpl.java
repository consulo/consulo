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
package consulo.desktop.qt.ui.impl.badge;

import consulo.desktop.qt.ui.impl.DesktopQtStyleApplier;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.base.DesktopQtIconLabel;
import consulo.desktop.qt.ui.impl.font.DesktopQtFontSize;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.localize.LocalizeValue;
import consulo.ui.Badge;
import consulo.ui.BadgeStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.BadgePresentation;
import io.qt.core.QRectF;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPainterPath;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.widgets.QLayout;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public class DesktopQtBadgeImpl extends QtComponentDelegate<DesktopQtBadgeImpl.BadgeFrame> implements Badge, DesktopQtIconOwner {
    public class BadgeFrame extends DesktopQtIconLabel {
        public BadgeFrame(@Nullable QWidget parent) {
            super(parent);
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            QPalette palette = palette();
            QColor background = DesktopQtStyleApplier.themeColor(BadgePresentation.background(myStyles), palette.color(QPalette.ColorRole.Button));
            QColor border = DesktopQtStyleApplier.themeColor(BadgePresentation.border(myStyles), palette.color(QPalette.ColorRole.Mid));

            QPainter painter = new QPainter(this);
            try {
                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                QPainterPath shape = new QPainterPath();
                shape.addRoundedRect(new QRectF(0.5, 0.5, width() - 1, height() - 1), BadgePresentation.ARC / 2.0, BadgePresentation.ARC / 2.0);
                painter.setBrush(new QBrush(background));
                painter.setPen(new QPen(border, 1));
                painter.drawPath(shape);
            }
            finally {
                painter.end();
            }
            super.paintEvent(event);
        }
    }

    private final Set<BadgeStyle> myStyles = EnumSet.noneOf(BadgeStyle.class);
    private LocalizeValue myText;
    private @Nullable Image myIcon;

    public DesktopQtBadgeImpl(LocalizeValue text) {
        myText = text;
    }

    @Override
    protected BadgeFrame createQt(QWidget parent) {
        return new BadgeFrame(parent);
    }

    @Override
    protected void initialize(BadgeFrame component) {
        component.setText(myText.get());
        component.setImage(myIcon);
        QLayout layout = component.layout();
        if (layout != null) {
            layout.setContentsMargins(
                BadgePresentation.HORIZONTAL_PADDING,
                BadgePresentation.VERTICAL_PADDING,
                BadgePresentation.HORIZONTAL_PADDING,
                BadgePresentation.VERTICAL_PADDING
            );
        }
        component.setSizePolicy(QSizePolicy.Policy.Maximum, QSizePolicy.Policy.Fixed);
        updateLook(component);
    }

    private void updateLook(BadgeFrame component) {
        QPalette palette = component.palette();
        QColor foreground = DesktopQtStyleApplier.themeColor(BadgePresentation.foreground(myStyles), palette.color(QPalette.ColorRole.WindowText));
        component.textLabel().setStyleSheet("color: " + foreground.name() + "; background: transparent;");

        component.textLabel().setFont(DesktopQtFontSize.withPointDelta(component.font(), BadgePresentation.fontPointDelta(myStyles)));
        component.update();
    }

    @Override
    public void refreshIcons() {
        BadgeFrame component = myComponent;
        if (component != null && !component.isDisposed()) {
            component.setImage(myIcon);
            updateLook(component);
        }
    }

    @Override
    public void addStyle(BadgeStyle style) {
        if (style != BadgeStyle.FILLED && style != BadgeStyle.SMALL) {
            myStyles.removeIf(each -> each != BadgeStyle.FILLED && each != BadgeStyle.SMALL);
        }
        myStyles.add(style);
        BadgeFrame component = myComponent;
        if (component != null && !component.isDisposed()) {
            updateLook(component);
        }
    }

    @Override
    public LocalizeValue getText() {
        return myText;
    }

    @RequiredUIAccess
    @Override
    public void setText(LocalizeValue text) {
        myText = text;
        BadgeFrame component = myComponent;
        if (component != null && !component.isDisposed()) {
            component.setText(text.get());
        }
    }

    @Override
    public @Nullable Image getIcon() {
        return myIcon;
    }

    @RequiredUIAccess
    @Override
    public void setIcon(@Nullable Image icon) {
        myIcon = icon;
        refreshIcons();
    }
}
