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

import consulo.desktop.qt.ui.impl.DesktopQtStyleApplier;
import consulo.ui.style.ComponentColors;
import io.qt.core.QRect;
import io.qt.core.Qt;
import io.qt.gui.QFontMetrics;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.gui.QWindow;
import io.qt.widgets.QHBoxLayout;
import io.qt.widgets.QMenuBar;
import io.qt.widgets.QSizePolicy;
import io.qt.widgets.QStyle;
import io.qt.widgets.QStyleOption;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * The header a titleless window is drawn with - the menu bar of the window on the left, the window buttons on the
 * right, and the title of the window in what is left between them, the way a kde application which draws its own
 * decoration is built.
 * <p/>
 * Nothing here moves or resizes the window itself. A press which is not on a button is handed back to the display
 * server through {@link QWindow#startSystemMove()}, which on wayland is the {@code xdg_toplevel.move} request and on
 * x11 is {@code _NET_WM_MOVERESIZE} - so edge snapping, tiling previews and everything else the window manager does
 * with a dragged title bar keep working, and none of it is reimplemented.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtTitleBar extends QWidget {
    public static final String OBJECT_NAME = "consuloTitleBar";

    /**
     * The height of the whole header, which is what the window decoration of the desktop is drawn with. It is the
     * header which states it and never a widget the header carries - a menu bar left to ask for the height it wants
     * is what puts its text above the middle of the row and off the line the title is drawn on.
     */
    private static final int ourHeight = DesktopQtWindowButton.getCellHeight();

    /**
     * What the buttons keep between themselves and the right edge of the window, and what the title keeps between
     * itself and the menu bar and the buttons.
     */
    private static final int ourEdgePadding = 8;
    private static final int ourTitlePadding = 12;

    private final QHBoxLayout myLayout;
    private final DesktopQtWindowControls myControls;

    private @Nullable QMenuBar myMenuBar;

    public DesktopQtTitleBar(QWidget parent) {
        super(parent);

        setObjectName(OBJECT_NAME);
        setFixedHeight(ourHeight);
        setSizePolicy(QSizePolicy.Policy.Preferred, QSizePolicy.Policy.Fixed);

        myLayout = new QHBoxLayout();
        myLayout.setContentsMargins(0, 0, ourEdgePadding, 0);
        myLayout.setSpacing(0);
        setLayout(myLayout);

        // the menu bar goes in front of it, and the stretch is what keeps the buttons at the right edge while the
        // space between the two stays the header itself - which is what a drag and the title land on
        myLayout.addStretch(1);

        myControls = new DesktopQtWindowControls(this);
        myLayout.addWidget(myControls);
    }

    public static int getBarHeight() {
        return ourHeight;
    }

    /**
     * Takes the menu bar of the window into the header, which is where a titleless window shows it.
     * <p/>
     * The menu bar is fitted into the header rather than the other way round. A menu bar lays its items out at the
     * top of whatever height it is given - {@code QMenuBarPrivate::calcActionRects} places them at the frame width
     * and gives them the height of the tallest one - so a menu bar stretched to the height of the header carries
     * its text above the middle of it, next to a title which is centered. Held at the height it asks for and
     * centered in the row instead, both sit on the same line.
     */
    public void setMenuBar(@Nullable QMenuBar menuBar) {
        QMenuBar oldMenuBar = myMenuBar;
        if (oldMenuBar == menuBar) {
            return;
        }

        if (oldMenuBar != null) {
            myLayout.removeWidget(oldMenuBar);
            oldMenuBar.setParent(null);
        }

        myMenuBar = menuBar;

        if (menuBar != null) {
            // a menu bar left to expand would cover the whole header, and there would be nothing left to drag by,
            // and one held to a height of its own would hide every item it could not fit behind the overflow arrow
            // - a fixed policy with an alignment is what hands it the height it asks for and no other, laid out
            // again whenever a theme rewrites what a menu item is padded by
            menuBar.setSizePolicy(QSizePolicy.Policy.Maximum, QSizePolicy.Policy.Fixed);
            menuBar.setParent(this);
            myLayout.insertWidget(0, menuBar, 0, Qt.AlignmentFlag.AlignVCenter);
            menuBar.setVisible(true);
        }

        update();
    }

    public @Nullable QMenuBar getMenuBar() {
        return myMenuBar;
    }

    /**
     * Follows the state of the window, so the button offers to restore a maximized frame rather than to maximize it
     * again.
     */
    public void updateWindowState() {
        myControls.updateWindowState();
    }

    /**
     * The title of the window, in the room left between the menu bar and the buttons.
     * <p/>
     * The background comes first and by hand: qt fills the background a style sheet gives a widget only for a
     * widget whose painting it does entirely, and this one paints itself.
     */
    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);

        QStyleOption option = new QStyleOption();
        option.initFrom(this);
        style().drawPrimitive(QStyle.PrimitiveElement.PE_Widget, option, painter, this);

        QRect titleRect = titleRect();
        if (titleRect.width() > 0) {
            QFontMetrics metrics = new QFontMetrics(font());
            String title = metrics.elidedText(window().windowTitle(), Qt.TextElideMode.ElideRight, titleRect.width());

            painter.setPen(new QPen(DesktopQtStyleApplier.themeColor(
                ComponentColors.TEXT_FOREGROUND,
                palette().color(QPalette.ColorRole.WindowText)
            )));
            painter.drawText(titleRect, Qt.AlignmentFlag.AlignCenter.value(), title);
        }

        painter.end();
    }

    /**
     * What is free between the menu bar and the buttons. The title is centered in that rather than in the window:
     * the header of the frame carries the whole main menu, and a title centered on the window would land on it.
     */
    private QRect titleRect() {
        QMenuBar menuBar = myMenuBar;

        int left = (menuBar == null ? 0 : menuBar.geometry().right() + 1) + ourTitlePadding;
        int right = myControls.geometry().left() - ourTitlePadding;

        return new QRect(left, 0, Math.max(0, right - left), height());
    }

    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() != Qt.MouseButton.LeftButton) {
            super.mousePressEvent(event);
            return;
        }

        QWindow handle = window().windowHandle();
        if (handle != null) {
            handle.startSystemMove();
        }

        event.accept();
    }

    @Override
    protected void mouseDoubleClickEvent(QMouseEvent event) {
        if (event.button() != Qt.MouseButton.LeftButton) {
            super.mouseDoubleClickEvent(event);
            return;
        }

        myControls.toggleMaximized();

        event.accept();
    }
}
