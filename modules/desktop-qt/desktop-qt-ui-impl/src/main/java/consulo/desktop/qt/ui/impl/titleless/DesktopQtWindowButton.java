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
package consulo.desktop.qt.ui.impl.titleless;

import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import io.qt.core.QEvent;
import io.qt.core.QPointF;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QEnterEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;
import io.qt.widgets.QAbstractButton;
import io.qt.widgets.QWidget;

/**
 * One of the buttons a titleless window is minimized, maximized and closed by.
 * <p/>
 * What is drawn is the image {@link DesktopQtWindowControlIcons} holds for the desktop the ide runs on, and never a
 * {@code QStyle.StandardPixmap}: the standard pixmaps of a desktop style are the buttons of an mdi sub window, and
 * a style answers them with its generic dialog icons rather than with the controls its window decoration draws.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtWindowButton extends QAbstractButton {
    public enum Kind {
        MINIMIZE,
        MAXIMIZE,
        RESTORE,
        CLOSE
    }

    /**
     * The cell one button takes, measured off the decoration of the desktop.
     */
    private static final int ourCellWidth = 24;
    private static final int ourCellHeight = 30;

    private Kind myKind;

    public DesktopQtWindowButton(QWidget parent, Kind kind, Runnable action) {
        super(parent);

        myKind = kind;

        // a window control never carries the focus - it would take it away from whatever the user is editing and
        // paint a focus ring in the header
        setFocusPolicy(Qt.FocusPolicy.NoFocus);
        setAttribute(Qt.WidgetAttribute.WA_Hover, true);
        setCursor(Qt.CursorShape.ArrowCursor);
        setFixedSize(new QSize(ourCellWidth, ourCellHeight));

        clicked.connect(checked -> action.run());
    }

    public static int getCellHeight() {
        return ourCellHeight;
    }

    public void setKind(Kind kind) {
        if (myKind == kind) {
            return;
        }

        myKind = kind;
        update();
    }

    @Override
    protected void enterEvent(QEnterEvent event) {
        super.enterEvent(event);

        update();
    }

    @Override
    protected void leaveEvent(QEvent event) {
        super.leaveEvent(event);

        update();
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPixmap pixmap = DesktopQtImage.toQPixmap(DesktopQtWindowControlIcons.iconOf(myKind, currentState()));

        // the pixmap is built at physical size and tagged with the ratio of the screen it is shown on, so the room
        // it takes in the cell is its own size divided back down into the coordinates the widget is laid out in
        double ratio = pixmap.devicePixelRatio();
        double iconWidth = pixmap.width() / ratio;
        double iconHeight = pixmap.height() / ratio;

        QPainter painter = new QPainter(this);
        painter.drawPixmap(new QPointF((width() - iconWidth) / 2, (height() - iconHeight) / 2), pixmap);
        painter.end();
    }

    private DesktopQtWindowControlIcons.State currentState() {
        if (isDown()) {
            return DesktopQtWindowControlIcons.State.PRESSED;
        }

        if (underMouse()) {
            return DesktopQtWindowControlIcons.State.HOVER;
        }

        return isActiveWindow() ? DesktopQtWindowControlIcons.State.NORMAL : DesktopQtWindowControlIcons.State.INACTIVE;
    }
}
