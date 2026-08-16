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
package consulo.desktop.qt.wm.impl.toolWindow;

import consulo.application.ui.UISettings;
import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.image.DesktopQtIconOwner;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.project.ui.impl.internal.wm.ToolWindowBase;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowStripeButton;
import consulo.ui.ex.toolWindow.WindowInfo;
import consulo.ui.image.Image;
import io.qt.core.QEvent;
import io.qt.core.QRectF;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QEnterEvent;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPixmap;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtToolWindowStripeButtonImpl extends QtComponentDelegate<QWidget>
    implements ToolWindowStripeButton, DesktopQtIconOwner {
    private static final int PAD_ALONG = 5;
    private static final int PAD_ACROSS = 3;
    private static final int ICON_TEXT_GAP = 3;

    /**
     * What the awt stripe button keeps between its bounds and the background it fills - the room around the
     * background of one button and the background of the next.
     */
    private static final int MARGIN = 3;

    /**
     * The arc the background is rounded with, which is the whole of the corner rather than its radius - twice
     * the {@code Component.arc} the awt stripe button uses, which barely reads as rounded at this height.
     */
    private static final double ARC = 10;

    private final DesktopQtToolWindowInternalDecorator myDecorator;

    private boolean mySelected;
    private boolean myHovered;

    private String myText = "";
    private @Nullable Image myIcon;
    private @Nullable QPixmap myIconPixmap;

    @RequiredUIAccess
    public DesktopQtToolWindowStripeButtonImpl(
        DesktopQtToolWindowInternalDecorator decorator,
        DesktopQtToolWindowPanelImpl toolWindowPanel
    ) {
        myDecorator = decorator;
    }

    @Override
    protected QWidget createQt(QWidget parent) {
        return new StripeButtonWidget(parent);
    }

    @Override
    protected void initialize(QWidget component) {
        component.setSizePolicy(QSizePolicy.Policy.Fixed, QSizePolicy.Policy.Fixed);
        component.setCursor(Qt.CursorShape.PointingHandCursor);
    }

    private ToolWindowAnchor getAnchor() {
        return getWindowInfo().getAnchor();
    }

    private void onClick() {
        // the tool window itself is the live state - WindowInfo here is an immutable snapshot and the
        // cached selected flag is only refreshed by apply(), which never runs for restored tool windows
        if (myDecorator.getToolWindow().isVisible()) {
            myDecorator.fireHidden();
        }
        else {
            myDecorator.fireActivated();
        }
    }

    /**
     * Qt tears the stripe button down with the frame, and the tool window manager still applies window info
     * to it while the project closes - the java reference outlives the native widget and every call on it
     * throws {@link io.qt.QNoNativeResourcesException}.
     */
    private boolean isAlive() {
        return myComponent != null && !myComponent.isDisposed();
    }

    @Override
    public WindowInfo getWindowInfo() {
        return myDecorator.getWindowInfo();
    }

    @Override
    public void apply(WindowInfo info) {
        setSelected(info.isVisible() || info.isActive());
        updateState();
    }

    @RequiredUIAccess
    private void updateState() {
        ToolWindowBase window = (ToolWindowBase) myDecorator.getToolWindow();

        setSelected(window.isVisible());

        boolean toShow = window.isAvailable() || window.isPlaceholderMode();
        if (UISettings.getInstance().ALWAYS_SHOW_WINDOW_BUTTONS) {
            setVisible(window.isShowStripeButton() || isSelected());
        }
        else {
            setVisible(toShow && (window.isShowStripeButton() || isSelected()));
        }
        setEnabled(toShow && !window.isPlaceholderMode());

        myText = window.getDisplayName().get();
        setIcon(window.getIcon());

        if (isAlive()) {
            myComponent.updateGeometry();
            myComponent.update();
        }
    }

    private void setIcon(@Nullable Image icon) {
        if (myIcon == icon) {
            return;
        }

        myIcon = icon;

        updateIconPixmap();
    }

    private void updateIconPixmap() {
        myIconPixmap = myIcon instanceof DesktopQtImage qtImage
            ? qtImage.toQPixmap()
            : null;
    }

    @Override
    public void refreshIcons() {
        updateIconPixmap();

        if (isAlive()) {
            myComponent.update();
        }
    }

    public boolean isSelected() {
        return mySelected;
    }

    public void setSelected(boolean selected) {
        mySelected = selected;

        if (isAlive()) {
            myComponent.update();
        }
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    @RequiredUIAccess
    public void updatePresentation() {
        updateState();
    }

    @Override
    public void dispose() {
    }

    private int iconAlong(boolean vertical) {
        if (myIcon == null) {
            return 0;
        }
        return vertical ? myIcon.getHeight() : myIcon.getWidth();
    }

    private int iconAcross(boolean vertical) {
        if (myIcon == null) {
            return 0;
        }
        return vertical ? myIcon.getWidth() : myIcon.getHeight();
    }

    /**
     * The one measurement of the content, shared by the size hint and by the painting - so the room the text is
     * elided into is the very room the widget asked for.
     *
     * @param iconAlong  size of the icon on the axis the content runs along
     * @param iconAcross size of the icon on the other axis
     * @param textAlong  size of the whole, not elided, text on the axis the content runs along
     * @param textOffset where the text starts on the axis the content runs along
     * @param along      size of the content on the axis it runs along
     * @param across     size of the content on the other axis
     */
    private record ContentMetrics(int iconAlong, int iconAcross, int textAlong, int textOffset, int along, int across) {
    }

    private ContentMetrics measure(QFontMetrics fontMetrics, boolean vertical) {
        int iconAlong = iconAlong(vertical);
        int iconAcross = iconAcross(vertical);
        int textAlong = myText.isEmpty() ? 0 : fontMetrics.horizontalAdvance(myText);

        int gap = iconAlong > 0 && textAlong > 0 ? ICON_TEXT_GAP : 0;
        int textOffset = MARGIN + PAD_ALONG + iconAlong + gap;

        return new ContentMetrics(
            iconAlong,
            iconAcross,
            textAlong,
            textOffset,
            textOffset + textAlong + PAD_ALONG + MARGIN,
            (MARGIN + PAD_ACROSS) * 2 + Math.max(iconAcross, fontMetrics.height())
        );
    }

    /**
     * Paints itself instead of leaning on a {@link io.qt.widgets.QPushButton}: the stripe has to be free of any
     * button frame and, on the side anchors, has to read its text rotated by ninety degrees.
     */
    private class StripeButtonWidget extends QWidget {
        StripeButtonWidget(QWidget parent) {
            super(parent);
        }

        @Override
        public QSize sizeHint() {
            boolean vertical = !getAnchor().isHorizontal();

            ContentMetrics metrics = measure(fontMetrics(), vertical);

            return vertical
                ? new QSize(metrics.across(), metrics.along())
                : new QSize(metrics.along(), metrics.across());
        }

        @Override
        public QSize minimumSizeHint() {
            return sizeHint();
        }

        @Override
        protected void enterEvent(QEnterEvent event) {
            myHovered = true;
            update();
        }

        @Override
        protected void leaveEvent(QEvent event) {
            myHovered = false;
            update();
        }

        @Override
        protected void mouseReleaseEvent(QMouseEvent event) {
            if (event.button() == Qt.MouseButton.LeftButton && rect().contains(event.position().toPoint())) {
                onClick();
            }
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            ToolWindowAnchor anchor = getAnchor();
            boolean vertical = !anchor.isHorizontal();

            int w = width();
            int h = height();

            QPainter painter = new QPainter(this);
            try {
                painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
                painter.setRenderHint(QPainter.RenderHint.TextAntialiasing, true);
                painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, true);

                if (mySelected || myHovered) {
                    QColor highlight = palette().color(QPalette.ColorRole.Highlight);

                    painter.save();
                    try {
                        painter.setPen(Qt.PenStyle.NoPen);
                        painter.setBrush(new QColor(
                            highlight.red(),
                            highlight.green(),
                            highlight.blue(),
                            mySelected ? 110 : 55
                        ));
                        painter.drawRoundedRect(
                            new QRectF(MARGIN, MARGIN, w - MARGIN * 2, h - MARGIN * 2),
                            ARC / 2,
                            ARC / 2
                        );
                    }
                    finally {
                        painter.restore();
                    }
                }

                QFontMetrics fm = fontMetrics();

                int alongSize = vertical ? h : w;
                int acrossSize = vertical ? w : h;

                ContentMetrics metrics = measure(fm, vertical);

                int iconAlong = metrics.iconAlong();
                int iconAcross = metrics.iconAcross();

                int textAlongOffset = metrics.textOffset();
                int textBaseline = (acrossSize - fm.height()) / 2 + fm.ascent();

                int textRoom = Math.max(0, alongSize - textAlongOffset - PAD_ALONG - MARGIN);

                String text;
                if (myText.isEmpty()) {
                    text = "";
                }
                else if (metrics.textAlong() <= textRoom) {
                    text = myText;
                }
                else {
                    text = fm.elidedText(myText, Qt.TextElideMode.ElideRight, textRoom);
                }

                if (myIconPixmap != null) {
                    int iconAcrossOffset = (acrossSize - iconAcross) / 2;
                    int iconAlongOffset = MARGIN + PAD_ALONG;

                    // the icon itself is never rotated, only the text is
                    if (anchor == ToolWindowAnchor.LEFT) {
                        painter.drawPixmap(iconAcrossOffset, h - iconAlongOffset - iconAlong, iconAcross, iconAlong, myIconPixmap);
                    }
                    else if (anchor == ToolWindowAnchor.RIGHT) {
                        painter.drawPixmap(iconAcrossOffset, iconAlongOffset, iconAcross, iconAlong, myIconPixmap);
                    }
                    else {
                        painter.drawPixmap(iconAlongOffset, iconAcrossOffset, iconAlong, iconAcross, myIconPixmap);
                    }
                }

                if (!text.isEmpty()) {
                    QPalette.ColorGroup group = isEnabled() ? QPalette.ColorGroup.Active : QPalette.ColorGroup.Disabled;
                    painter.setPen(palette().color(group, QPalette.ColorRole.ButtonText));

                    painter.save();
                    try {
                        if (anchor == ToolWindowAnchor.LEFT) {
                            painter.rotate(-90);
                            painter.translate(-h, 0);
                        }
                        else if (anchor == ToolWindowAnchor.RIGHT) {
                            painter.rotate(90);
                            painter.translate(0, -w);
                        }

                        painter.drawText(textAlongOffset, textBaseline, text);
                    }
                    finally {
                        painter.restore();
                    }
                }
            }
            finally {
                painter.end();
            }
        }
    }
}
