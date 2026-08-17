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
package consulo.desktop.qt.codeInsight.intention;

import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.Style;
import consulo.ui.style.StyleColorValue;
import consulo.ui.style.StyleManager;
import io.qt.core.QEvent;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QCursor;
import io.qt.gui.QEnterEvent;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPen;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * The bulb itself: an icon floating over the text, drawn straight onto a widget of the editor rather than raised
 * as a popup.
 * <p>
 * A popup of the frontend carries a border and a background of its own and is sized by its layout, none of which
 * suits something that has to sit unobtrusively on a line of code until the pointer finds it - so the bulb paints
 * what it needs and nothing else, and only shows its frame once it is hovered.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
public class DesktopQtIntentionBulbWidget extends QWidget {
    private static final int PADDING = 5;

    private static final int ARC = 6;

    private final Runnable myOnClick;

    private Image myIcon;

    private boolean myHovered;

    public DesktopQtIntentionBulbWidget(QWidget parent, Image icon, Runnable onClick) {
        super(parent);

        myIcon = icon;
        myOnClick = onClick;

        setCursor(new QCursor(Qt.CursorShape.ArrowCursor));
        setToolTip("Show Intention Actions");
    }

    public void setIcon(Image icon) {
        myIcon = icon;

        applyGeometry(x(), y());
    }

    /**
     * Puts the bulb with its top left corner where it is told, sized to the icon it draws.
     */
    public void applyGeometry(int x, int y) {
        int size = Math.max(1, myIcon.getWidth()) + 2 * PADDING;

        setGeometry(x, y, size, Math.max(1, myIcon.getHeight()) + 2 * PADDING);
    }

    public int iconHeight() {
        return Math.max(1, myIcon.getHeight()) + 2 * PADDING;
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
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() == Qt.MouseButton.LeftButton) {
            myOnClick.run();

            event.accept();
            return;
        }

        super.mousePressEvent(event);
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            paint(painter);
        }
        finally {
            painter.end();
        }
    }

    private void paint(QPainter painter) {
        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);

        // nothing but the icon until the pointer is on it - a frame around the bulb at rest would read as a
        // control sitting in the middle of the code
        if (myHovered) {
            ColorValue background = color(ComponentColors.LAYOUT);
            ColorValue border = color(ComponentColors.BORDER);

            if (background != null) {
                painter.setBrush(new QBrush(TargetQt.to(background)));
            }

            painter.setPen(border == null ? new QPen(Qt.PenStyle.NoPen) : new QPen(TargetQt.to(border)));
            painter.drawRoundedRect(new QRect(0, 0, width() - 1, height() - 1), ARC, ARC);
        }

        if (myIcon instanceof DesktopQtImage qtImage) {
            qtImage.toQIcon().paint(
                painter,
                new QRect(PADDING, PADDING, Math.max(1, myIcon.getWidth()), Math.max(1, myIcon.getHeight()))
            );
        }
    }

    private static @Nullable ColorValue color(StyleColorValue key) {
        Style style = StyleManager.get().getCurrentStyle();

        return style == null ? null : style.getColorValue(key);
    }
}
