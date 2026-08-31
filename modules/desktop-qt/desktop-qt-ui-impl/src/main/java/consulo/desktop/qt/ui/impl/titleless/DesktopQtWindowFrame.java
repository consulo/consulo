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
import io.qt.core.QEvent;
import io.qt.core.QMargins;
import io.qt.core.QObject;
import io.qt.core.QPoint;
import io.qt.core.QRect;
import io.qt.core.QRectF;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QBrush;
import io.qt.gui.QColor;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPalette;
import io.qt.gui.QPen;
import io.qt.gui.QRegion;
import io.qt.gui.QWindow;
import io.qt.widgets.QBoxLayout;
import io.qt.widgets.QLayout;
import io.qt.widgets.QMainWindow;
import io.qt.widgets.QMenuBar;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * Turns a window into one which carries its own decoration: the frame drops the title bar of the display server and
 * draws what the server no longer does - a border, a drop shadow, and for a {@link DesktopQtTitleBarPlacement#STRIP}
 * window a {@link DesktopQtTitleBar}. The margin the shadow is painted in is also the strip the frame is resized by.
 * <p/>
 * On wayland {@code xdg-decoration} is all or nothing - there is no server border without a title bar - so drawing
 * the decoration is the native path there rather than a workaround. None of the window management is reimplemented:
 * a press on an edge is handed back through {@link QWindow#startSystemResize}, which is the
 * {@code xdg_toplevel.resize} request, and a press on the background of a header-less window through
 * {@link QWindow#startSystemMove}, so the compositor runs the drag and keeps tiling previews, edge snapping and
 * quarter tiling.
 * <p/>
 * It is an event filter rather than a widget subclass so that nothing but the window it is installed on has to know
 * about it.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtWindowFrame extends QObject {
    /**
     * How wide the margin around the content is. It carries the drop shadow, and it is the strip the frame is
     * resized by - the same thing a window decoration of the desktop offers, and what keeps the handles belonging
     * to the window rather than to whichever widget of the ide happens to reach the edge, since a mouse press only
     * ever reaches the topmost widget under it.
     */
    private static final int ourFrameMargin = 10;

    /**
     * The corner radius of the decoration of the desktop, measured off a breeze frame: the corner of a window on
     * screen is an arc of {@value #ourCornerRadius} points, and the decoration plugin draws it with
     * {@code QPainter#drawRoundedRect}.
     */
    private static final int ourCornerRadius = 5;

    /**
     * What the floating controls of a window with no header keep between themselves and its right edge - the same
     * padding the header of a frame holds.
     */
    private static final int ourOverlayPadding = 8;

    /**
     * How dark the shadow is right against the border. The window which has the focus casts the stronger one, the
     * way the decoration of a desktop draws it.
     */
    private static final int ourActiveShadowAlpha = 110;
    private static final int ourInactiveShadowAlpha = 55;

    private final QMainWindow myWindow;
    private final QWidget myCentralWidget;
    private final @Nullable DesktopQtTitleBar myTitleBar;
    private final @Nullable DesktopQtWindowControls myOverlayControls;

    private boolean myCursorOverridden;

    public DesktopQtWindowFrame(QMainWindow window, QWidget centralWidget, DesktopQtTitleBarPlacement placement) {
        super(window);

        myWindow = window;
        myCentralWidget = centralWidget;

        window.setWindowFlag(Qt.WindowType.FramelessWindowHint, true);
        // the shadow fades out into the desktop, so the margin around the content has to be able to show it
        window.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground, true);
        window.setMouseTracking(true);

        if (placement == DesktopQtTitleBarPlacement.STRIP) {
            DesktopQtTitleBar titleBar = new DesktopQtTitleBar(centralWidget);
            myTitleBar = titleBar;

            QLayout layout = centralWidget.layout();
            if (layout instanceof QBoxLayout boxLayout) {
                // the header sits directly on the content, and the spacing a box layout keeps between its widgets
                // would show as a band of the window background under it
                boxLayout.setSpacing(0);
                boxLayout.insertWidget(0, titleBar);
            }

            window.windowTitleChanged.connect(title -> titleBar.update());
        }
        else {
            myTitleBar = null;
        }

        if (placement == DesktopQtTitleBarPlacement.OVERLAY) {
            // the controls are a child of the content and no part of its layout, so they are placed by hand over
            // whatever the window shows - the room they need is stated by DesktopQtTitlelessDecorator instead
            myOverlayControls = new DesktopQtWindowControls(centralWidget);
        }
        else {
            myOverlayControls = null;
        }

        applyFrameMargin();

        centralWidget.installEventFilter(this);
        window.installEventFilter(this);
    }

    /**
     * @return whether the header took the menu bar, and it is not left to the window to place it
     */
    public boolean setMenuBar(@Nullable QMenuBar menuBar) {
        DesktopQtTitleBar titleBar = myTitleBar;
        if (titleBar == null) {
            return false;
        }

        titleBar.setMenuBar(menuBar);
        return true;
    }

    /**
     * The border and the shadow of the window, painted under everything the window holds.
     * <p/>
     * The border is the innermost point of the margin rather than the outermost point of the content: the children
     * of the window cover the content rect whole, and a border drawn inside it would be painted over. The corners
     * the content is clipped to are the ones drawn here, one point further in - see {@link #applyContentMask}.
     */
    public void paint() {
        int margin = currentFrameMargin();
        int radius = currentCornerRadius();
        int width = myWindow.width();
        int height = myWindow.height();

        QColor background = myWindow.palette().color(QPalette.ColorRole.Window);

        QPainter painter = new QPainter(myWindow);

        if (radius == 0) {
            if (margin > 0) {
                paintShadow(painter, width, height, margin, radius);

                painter.fillRect(
                    new QRect(margin - 1, margin - 1, width - 2 * margin + 2, height - 2 * margin + 2),
                    new QBrush(borderColor(background))
                );
            }

            // the window is translucent, so what the content stands on is painted rather than cleared
            painter.fillRect(new QRect(margin, margin, width - 2 * margin, height - 2 * margin), new QBrush(background));

            painter.end();
            return;
        }

        paintShadow(painter, width, height, margin, radius);

        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
        painter.setPen(Qt.PenStyle.NoPen);

        // the border is a point wide and is drawn around the content rather than inside it, so the arc the eye
        // reads the window off is half a point outside the shape the content is cut to - which is where breeze
        // puts it too, its outline being a pen of one point centered on the edge of the frame
        painter.setBrush(new QBrush(borderColor(background)));
        painter.drawRoundedRect(
            new QRectF(margin - 1, margin - 1, width - 2.0 * margin + 2, height - 2.0 * margin + 2),
            radius + 0.5,
            radius + 0.5
        );

        painter.setBrush(new QBrush(background));
        painter.drawRoundedRect(
            new QRectF(margin, margin, width - 2.0 * margin, height - 2.0 * margin),
            radius - 0.5,
            radius - 0.5
        );

        painter.end();
    }

    /**
     * Clips the content of the window to the same rounded corners the border is drawn with.
     * <p/>
     * Nothing else does it: every child of the window covers the content rect as a square, so a rounded background
     * painted under them is painted over at exactly the four corners which are meant to show the desktop. A mask
     * cuts them out for good, and the border drawn around it - a point further out and antialiased - is what the
     * eye reads the edge of the window off, so the step of the cut does not show.
     */
    private void applyContentMask() {
        int radius = currentCornerRadius();

        if (radius == 0) {
            myCentralWidget.clearMask();
            return;
        }

        myCentralWidget.setMask(roundedRegion(myCentralWidget.width(), myCentralWidget.height(), radius));
    }

    private static QRegion roundedRegion(int width, int height, int radius) {
        int diameter = 2 * radius;

        QRegion region = new QRegion(0, radius, width, height - diameter, QRegion.RegionType.Rectangle);
        region = region.plus(new QRegion(radius, 0, width - diameter, height, QRegion.RegionType.Rectangle));
        region = region.plus(new QRegion(0, 0, diameter, diameter, QRegion.RegionType.Ellipse));
        region = region.plus(new QRegion(width - diameter, 0, diameter, diameter, QRegion.RegionType.Ellipse));
        region = region.plus(new QRegion(0, height - diameter, diameter, diameter, QRegion.RegionType.Ellipse));
        region = region.plus(new QRegion(width - diameter, height - diameter, diameter, diameter, QRegion.RegionType.Ellipse));

        return region;
    }

    /**
     * Puts the floating controls of a window with no header in its top right corner, and keeps them over whatever
     * the content adds to itself afterwards.
     */
    private void placeOverlayControls() {
        DesktopQtWindowControls controls = myOverlayControls;
        if (controls == null) {
            return;
        }

        QSize size = controls.sizeHint();

        controls.setGeometry(
            myCentralWidget.width() - size.width() - ourOverlayPadding,
            0,
            size.width(),
            size.height()
        );
        controls.raise();
    }

    /**
     * The border color of the theme, laid over the background of the window first.
     * <p/>
     * A theme states a border as a color which is meant to be drawn over the background it borders - the dark
     * consulo themes state a white of a tenth of an alpha - and the margin the border of a window is drawn in shows
     * the desktop rather than that background, where the same color would come out as good as invisible.
     */
    private static QColor borderColor(QColor background) {
        QColor border = DesktopQtStyleApplier.themeColor(ComponentColors.BORDER, background.darker(140));

        double alpha = border.alphaF();

        return new QColor(
            (int) Math.round(border.red() * alpha + background.red() * (1 - alpha)),
            (int) Math.round(border.green() * alpha + background.green() * (1 - alpha)),
            (int) Math.round(border.blue() * alpha + background.blue() * (1 - alpha))
        );
    }

    /**
     * Rings of black, each one a little less transparent than the one outside it - a gradient which reads as a
     * shadow once the compositor has blended it over the desktop.
     * <p/>
     * Every ring is concentric with the corner of the window: a ring a point further out is a point rounder, so
     * the shadow follows the shape of the frame rather than squaring off around it.
     */
    private void paintShadow(QPainter painter, int width, int height, int margin, int radius) {
        if (margin <= 1) {
            return;
        }

        int alpha = myWindow.isActiveWindow() ? ourActiveShadowAlpha : ourInactiveShadowAlpha;

        painter.setRenderHint(QPainter.RenderHint.Antialiasing, true);
        painter.setBrush(new QBrush(Qt.BrushStyle.NoBrush));

        for (int i = 0; i < margin - 1; i++) {
            double ratio = (i + 1) / (double) (margin - 1);

            painter.setPen(new QPen(new QColor(0, 0, 0, (int) (alpha * ratio * ratio)), 1));
            painter.drawRoundedRect(
                new QRectF(i + 0.5, i + 0.5, width - 2.0 * i - 1, height - 2.0 * i - 1),
                radius + margin - i,
                radius + margin - i
            );
        }

        painter.setRenderHint(QPainter.RenderHint.Antialiasing, false);
    }

    @Override
    public boolean eventFilter(QObject watched, QEvent event) {
        QEvent.Type type = event.type();

        if (watched == myCentralWidget) {
            if (type == QEvent.Type.Resize || type == QEvent.Type.Show) {
                applyContentMask();
                placeOverlayControls();
            }
            else if (type == QEvent.Type.ChildAdded) {
                // a child added later is stacked over everything the content already held, and the controls are
                // meant to float over all of it
                placeOverlayControls();
            }

            return false;
        }

        if (watched != myWindow) {
            return false;
        }

        if (type == QEvent.Type.MouseButtonPress && event instanceof QMouseEvent mouseEvent) {
            return startDrag(mouseEvent);
        }

        if (type == QEvent.Type.MouseMove && event instanceof QMouseEvent mouseEvent) {
            updateCursor(edgesAt(mouseEvent.position().toPoint()));
            return false;
        }

        if (type == QEvent.Type.Leave) {
            updateCursor(new Qt.Edges());
            return false;
        }

        if (type == QEvent.Type.Resize || type == QEvent.Type.Show) {
            // a frame the compositor tiled changes its size and not its state, so the margin is answered here too
            applyFrameMargin();

            myWindow.update();
            return false;
        }

        if (type == QEvent.Type.ActivationChange) {
            // the controls are drawn dimmed while the window does not hold the focus, and the activation of a
            // window is not an event its children are sent
            updateWindowState();

            myWindow.update();
            return false;
        }

        if (type == QEvent.Type.WindowStateChange) {
            applyFrameMargin();

            updateWindowState();

            myWindow.update();
            return false;
        }

        return false;
    }

    /**
     * A press on the margin resizes the frame. A press which reached the window itself on a window with no header
     * moves it: every widget which answers a press keeps it, so what is left over is the background.
     */
    private boolean startDrag(QMouseEvent event) {
        if (event.button() != Qt.MouseButton.LeftButton) {
            return false;
        }

        QWindow handle = myWindow.windowHandle();
        if (handle == null) {
            return false;
        }

        Qt.Edges edges = edgesAt(event.position().toPoint());
        if (edges.value() != 0) {
            handle.startSystemResize(edges);
            return true;
        }

        if (myTitleBar == null) {
            handle.startSystemMove();
            return true;
        }

        return false;
    }

    private Qt.Edges edgesAt(QPoint point) {
        Qt.Edges edges = new Qt.Edges();

        int margin = currentFrameMargin();
        if (margin == 0) {
            return edges;
        }

        if (point.x() < margin) {
            edges.setFlag(Qt.Edge.LeftEdge);
        }
        else if (point.x() >= myWindow.width() - margin) {
            edges.setFlag(Qt.Edge.RightEdge);
        }

        if (point.y() < margin) {
            edges.setFlag(Qt.Edge.TopEdge);
        }
        else if (point.y() >= myWindow.height() - margin) {
            edges.setFlag(Qt.Edge.BottomEdge);
        }

        return edges;
    }

    private void updateCursor(Qt.Edges edges) {
        Qt.@Nullable CursorShape shape = cursorShape(edges);

        if (shape == null) {
            if (myCursorOverridden) {
                myCursorOverridden = false;
                myWindow.unsetCursor();
            }
            return;
        }

        myCursorOverridden = true;
        myWindow.setCursor(shape);
    }

    private static Qt.@Nullable CursorShape cursorShape(Qt.Edges edges) {
        boolean left = edges.testFlag(Qt.Edge.LeftEdge);
        boolean right = edges.testFlag(Qt.Edge.RightEdge);
        boolean top = edges.testFlag(Qt.Edge.TopEdge);
        boolean bottom = edges.testFlag(Qt.Edge.BottomEdge);

        if (left && top || right && bottom) {
            return Qt.CursorShape.SizeFDiagCursor;
        }

        if (right && top || left && bottom) {
            return Qt.CursorShape.SizeBDiagCursor;
        }

        if (left || right) {
            return Qt.CursorShape.SizeHorCursor;
        }

        if (top || bottom) {
            return Qt.CursorShape.SizeVerCursor;
        }

        return null;
    }

    private void updateWindowState() {
        if (myTitleBar != null) {
            myTitleBar.updateWindowState();
        }

        if (myOverlayControls != null) {
            myOverlayControls.updateWindowState();
        }
    }

    /**
     * The margin the window carries is read back off the window rather than remembered here: the state a frame
     * comes up in is handed to it before it is shown, which is a state change qt has no window to report against,
     * so a margin remembered from the constructor is one the frame never had.
     */
    private void applyFrameMargin() {
        int margin = currentFrameMargin();

        QMargins margins = new QMargins(margin, margin, margin, margin);
        if (margins.equals(myWindow.contentsMargins())) {
            return;
        }

        myWindow.setContentsMargins(margins);
        applyContentMask();
    }

    /**
     * A maximized or full screen frame is sized by the compositor and has no edge of its own to drag or to cast a
     * shadow off, so the margin is given back to the content there - and with it go the rounded corners, which at
     * a screen edge would be four notches of desktop cut out of the frame.
     */
    private int currentFrameMargin() {
        return isSizedByCompositor() ? 0 : ourFrameMargin;
    }

    private int currentCornerRadius() {
        return isSizedByCompositor() ? 0 : ourCornerRadius;
    }

    /**
     * Whether the compositor owns the size of the frame.
     * <p/>
     * Only the two states the window reports of itself are read. A tiled frame is not one of them - kwin hands a
     * wayland top level the tiled edges of {@code xdg_toplevel} and qt keeps them to itself - and it must not be
     * guessed at from the size of the frame against the screen: the margin is the only thing the frame can be
     * resized by, so a guess which takes it away can never be revised afterwards. A tiled frame keeps its margin
     * instead, which costs a gap at the screen edge and nothing else.
     */
    private boolean isSizedByCompositor() {
        return myWindow.isMaximized() || myWindow.isFullScreen();
    }
}
