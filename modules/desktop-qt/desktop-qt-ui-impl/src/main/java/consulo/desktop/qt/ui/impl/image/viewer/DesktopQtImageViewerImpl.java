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
package consulo.desktop.qt.ui.impl.image.viewer;

import consulo.desktop.qt.ui.impl.QtComponentDelegate;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.image.DesktopQtBytesImageImpl;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.ui.Point2D;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.viewer.ImageViewer;
import consulo.ui.image.viewer.ImageViewerChessboard;
import consulo.ui.image.viewer.ImageViewerGrid;
import consulo.ui.image.viewer.ImageViewerZoom;
import consulo.ui.impl.image.viewer.ImageViewerState;
import io.qt.core.QEvent;
import io.qt.core.QLineF;
import io.qt.core.QPoint;
import io.qt.core.QPointF;
import io.qt.core.QRect;
import io.qt.core.QSize;
import io.qt.core.Qt;
import io.qt.gui.QColor;
import io.qt.gui.QCursor;
import io.qt.gui.QImage;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QNativeGestureEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.gui.QPen;
import io.qt.gui.QPixmap;
import io.qt.gui.QResizeEvent;
import io.qt.gui.QWheelEvent;
import io.qt.widgets.QFrame;
import io.qt.widgets.QScrollArea;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

/**
 * A canvas widget of the size of the image at its zoom, in a scroll area which centres it while it is smaller than
 * the viewport. Everything but the widgets lives in the {@link ImageViewerState}, since qt builds the widgets anew
 * whenever the component is bound again.
 *
 * @author VISTALL
 * @since 2026-10-01
 */
public class DesktopQtImageViewerImpl extends QtComponentDelegate<QScrollArea> implements ImageViewer, ImageViewerState.Host {
    private class ViewerScrollArea extends QScrollArea {
        ViewerScrollArea(QWidget parent) {
            super(parent);
        }

        /**
         * The viewport got another size - also when a scroll bar is shown or hidden, which qt does in a queued call
         * after the zoom, so the centred canvas moves under the pointer once more.
         */
        @Override
        protected void resizeEvent(QResizeEvent event) {
            super.resizeEvent(event);
            myState.viewportResized();
            checkPointer();
        }

        /**
         * Gets what is over the empty viewport around a small image, and what the canvas left to its parent - so the
         * wheel and the pinch zoom wherever they are over the viewer. Positions are in the viewport.
         */
        @Override
        protected boolean viewportEvent(QEvent event) {
            if (event instanceof QWheelEvent wheel) {
                int delta = wheel.angleDelta().y();
                if (delta != 0 && wheel.modifiers().testFlag(Qt.KeyboardModifier.ControlModifier)) {
                    QPoint point = wheel.position().toPoint();
                    if (myState.wheelZoom(delta > 0, new Point2D(point.x(), point.y()))) {
                        wheel.accept();
                        return true;
                    }
                }
            }
            else if (event instanceof QNativeGestureEvent gesture && gesture.gestureType() == Qt.NativeGestureType.ZoomNativeGesture) {
                QPoint point = gesture.position().toPoint();
                myState.magnify(1 + gesture.value(), new Point2D(point.x(), point.y()));
                gesture.accept();
                return true;
            }
            // the scroll bars scroll
            return super.viewportEvent(event);
        }
    }

    private class Canvas extends QWidget {
        private @Nullable Image myPixmapImage;
        private @Nullable QPixmap myPixmap;

        Canvas(QWidget parent) {
            super(parent);

            setMouseTracking(true);
        }

        void imageChanged() {
            myPixmapImage = null;
            myPixmap = null;
        }

        @Override
        protected void paintEvent(QPaintEvent event) {
            Image image = myState.getImage();
            Size2D area = myState.getImageAreaSize();
            if (image == null || area.isEmpty()) {
                return;
            }

            int insets = ImageViewerState.INSETS;
            QRect areaRect = new QRect(insets, insets, area.width(), area.height());
            QRect exposed = event.rect().intersected(areaRect);
            if (exposed.isEmpty()) {
                return;
            }

            QPainter painter = new QPainter(this);
            try {
                painter.setClipRect(exposed);

                paintChessboard(painter, areaRect, exposed);
                paintImage(painter, image, areaRect, exposed);
                paintGrid(painter, image, areaRect, exposed);
            }
            finally {
                painter.end();
            }
        }

        private void paintChessboard(QPainter painter, QRect areaRect, QRect exposed) {
            ImageViewerChessboard chessboard = myState.getChessboard();
            if (chessboard == null) {
                return;
            }

            int cellSize = Math.max(1, chessboard.cellSize());
            painter.fillRect(exposed, TargetQt.to(chessboard.lightColor()));

            QColor darkColor = TargetQt.to(chessboard.darkColor());
            int fromColumn = (exposed.x() - areaRect.x()) / cellSize;
            int toColumn = (exposed.x() + exposed.width() - areaRect.x()) / cellSize;
            int fromRow = (exposed.y() - areaRect.y()) / cellSize;
            int toRow = (exposed.y() + exposed.height() - areaRect.y()) / cellSize;
            for (int column = fromColumn; column <= toColumn; column++) {
                for (int row = fromRow; row <= toRow; row++) {
                    if ((column + row) % 2 == 0) {
                        painter.fillRect(areaRect.x() + column * cellSize, areaRect.y() + row * cellSize, cellSize, cellSize, darkColor);
                    }
                }
            }
        }

        private void paintImage(QPainter painter, Image image, QRect areaRect, QRect exposed) {
            if (image instanceof DesktopQtBytesImageImpl bytesImage && bytesImage.isVector()) {
                // only the visible part of a svg is rendered, at the size it is shown - sharp at every zoom, and a
                // high zoom does not need an image of the whole
                double ratio = devicePixelRatioF();
                QRect region = new QRect(
                    (int) Math.floor((exposed.x() - areaRect.x()) * ratio),
                    (int) Math.floor((exposed.y() - areaRect.y()) * ratio),
                    (int) Math.ceil(exposed.width() * ratio),
                    (int) Math.ceil(exposed.height() * ratio)
                );

                QImage part = bytesImage.renderVectorRegion(
                    (int) Math.round(areaRect.width() * ratio),
                    (int) Math.round(areaRect.height() * ratio),
                    region
                );
                if (part != null) {
                    part.setDevicePixelRatio(ratio);
                    painter.drawImage(exposed.topLeft(), part);
                    return;
                }
            }

            // no source pixel is mixed with another when zoomed in - each one shows as a square
            painter.setRenderHint(QPainter.RenderHint.SmoothPixmapTransform, myState.isSmoothScaling());
            painter.drawPixmap(areaRect, getPixmap(image));
        }

        private QPixmap getPixmap(Image image) {
            QPixmap pixmap = myPixmap;
            if (pixmap == null || myPixmapImage != image) {
                pixmap = DesktopQtImage.toQPixmap(image);
                myPixmap = pixmap;
                myPixmapImage = image;
            }
            return pixmap;
        }

        private void paintGrid(QPainter painter, Image image, QRect areaRect, QRect exposed) {
            ImageViewerGrid grid = myState.getVisibleGrid();
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            if (grid == null || imageWidth <= 0 || imageHeight <= 0) {
                return;
            }

            double zoomX = (double) areaRect.width() / imageWidth;
            double zoomY = (double) areaRect.height() / imageHeight;

            QColor lineColor = TargetQt.to(grid.lineColor());
            QPen auxPen = new QPen(new QColor(lineColor.red(), lineColor.green(), lineColor.blue(), 0x26));
            QPen mainPen = new QPen(new QColor(lineColor.red(), lineColor.green(), lineColor.blue(), 0x4D));
            auxPen.setCosmetic(true);
            mainPen.setCosmetic(true);
            int lineSpan = Math.max(1, grid.lineSpan());
            double ratio = devicePixelRatioF();

            double top = exposed.y();
            double bottom = exposed.y() + exposed.height();
            int fromX = Math.max(1, (int) Math.floor((exposed.x() - areaRect.x()) / zoomX));
            int toX = Math.min(imageWidth - 1, (int) Math.ceil((exposed.x() + exposed.width() - areaRect.x()) / zoomX));
            for (int dx = fromX; dx <= toX; dx++) {
                painter.setPen(dx % lineSpan == 0 ? mainPen : auxPen);
                double x = pixelStart(areaRect.x() + dx * zoomX, ratio);
                painter.drawLine(new QLineF(x, top, x, bottom));
            }

            double left = exposed.x();
            double right = exposed.x() + exposed.width();
            int fromY = Math.max(1, (int) Math.floor((exposed.y() - areaRect.y()) / zoomY));
            int toY = Math.min(imageHeight - 1, (int) Math.ceil((exposed.y() + exposed.height() - areaRect.y()) / zoomY));
            for (int dy = fromY; dy <= toY; dy++) {
                painter.setPen(dy % lineSpan == 0 ? mainPen : auxPen);
                double y = pixelStart(areaRect.y() + dy * zoomY, ratio);
                painter.drawLine(new QLineF(left, y, right, y));
            }
        }

        /**
         * The image is drawn without smoothing, so every device pixel shows the pixel of the image under its centre -
         * a pixel of the image starts at its edge rounded to a device column. A line one pixel wide fills the column
         * its position falls in, so it is put through the middle of that column.
         */
        private static double pixelStart(double edge, double ratio) {
            return (Math.floor(edge * ratio + 0.5) + 0.5) / ratio;
        }

        @Override
        protected void mouseMoveEvent(QMouseEvent event) {
            myState.pointerMoved(event.position().x(), event.position().y());
            super.mouseMoveEvent(event);
        }

        @Override
        protected void leaveEvent(QEvent event) {
            myState.pointerExited();
            super.leaveEvent(event);
        }

        @Override
        public boolean event(QEvent event) {
            // not sure every platform passes a gesture on to the parent like a wheel event
            if (event instanceof QNativeGestureEvent gesture && gesture.gestureType() == Qt.NativeGestureType.ZoomNativeGesture) {
                QPoint point = mapToParent(gesture.position().toPoint());
                myState.magnify(1 + gesture.value(), new Point2D(point.x(), point.y()));
                event.accept();
                return true;
            }
            return super.event(event);
        }
    }

    private final ImageViewerState myState = new ImageViewerState(this, this);

    private @Nullable Canvas myCanvas;

    private boolean myCanvasMoving;

    @Override
    protected QScrollArea createQt(QWidget parent) {
        ViewerScrollArea area = new ViewerScrollArea(parent);
        area.setWidgetResizable(false);
        area.setAlignment(Qt.AlignmentFlag.AlignCenter);
        area.setFrameShape(QFrame.Shape.NoFrame);

        Canvas canvas = new Canvas(area);
        area.setWidget(canvas);
        myCanvas = canvas;

        // a scroll moves the canvas under the pointer
        area.horizontalScrollBar().valueChanged.connect(value -> checkPointer());
        area.verticalScrollBar().valueChanged.connect(value -> checkPointer());
        return area;
    }

    @Override
    protected void initialize(QScrollArea component) {
        updateCanvasSize();
    }

    @Override
    public @Nullable Image getImage() {
        return myState.getImage();
    }

    @RequiredUIAccess
    @Override
    public void setImage(@Nullable Image image) {
        myState.setImage(image);
    }

    @Override
    public ImageViewerZoom getZoom() {
        return myState;
    }

    @Override
    public @Nullable ImageViewerChessboard getChessboard() {
        return myState.getChessboard();
    }

    @RequiredUIAccess
    @Override
    public void setChessboard(@Nullable ImageViewerChessboard chessboard) {
        myState.setChessboard(chessboard);
    }

    @Override
    public @Nullable ImageViewerGrid getGrid() {
        return myState.getGrid();
    }

    @RequiredUIAccess
    @Override
    public void setGrid(@Nullable ImageViewerGrid grid) {
        myState.setGrid(grid);
    }

    @Override
    public boolean isWheelZoomEnabled() {
        return myState.isWheelZoomEnabled();
    }

    @RequiredUIAccess
    @Override
    public void setWheelZoomEnabled(boolean enabled) {
        myState.setWheelZoomEnabled(enabled);
    }

    @Override
    public @Nullable Size2D getViewportSize() {
        QScrollArea area = myComponent;
        if (area == null || area.isDisposed()) {
            return null;
        }

        // as if no scroll bar was shown
        QSize size = area.maximumViewportSize();
        return new Size2D(size.width(), size.height());
    }

    @RequiredUIAccess
    @Override
    public void imageChanged() {
        Canvas canvas = getLiveCanvas();
        if (canvas == null) {
            return;
        }

        canvas.imageChanged();
        myCanvasMoving = true;
        try {
            updateCanvasSize();
        }
        finally {
            myCanvasMoving = false;
        }
        canvas.update();
        checkPointer();
    }

    @RequiredUIAccess
    @Override
    public void zoomChanged(double oldZoomFactor, @Nullable Point2D anchor) {
        Canvas canvas = getLiveCanvas();
        QScrollArea area = myComponent;
        if (canvas == null || area == null) {
            return;
        }

        // the canvas sits at minus the scroll offset, or at the gap before it while centred - taken before the
        // resize moves it
        QPoint origin = canvas.pos();
        QSize viewport = area.viewport().size();
        Point2D point = anchor != null ? anchor : new Point2D(viewport.width() / 2, viewport.height() / 2);

        myCanvasMoving = true;
        try {
            // a visible widget gets its resize event right away, so the scroll bars know the new range below
            updateCanvasSize();

            area.horizontalScrollBar().setValue(myState.anchoredScroll(-origin.x(), point.x(), oldZoomFactor));
            area.verticalScrollBar().setValue(myState.anchoredScroll(-origin.y(), point.y(), oldZoomFactor));
        }
        finally {
            myCanvasMoving = false;
        }
        canvas.update();
        checkPointer();
    }

    @RequiredUIAccess
    @Override
    public void appearanceChanged() {
        Canvas canvas = getLiveCanvas();
        if (canvas != null) {
            canvas.update();
        }
    }

    /**
     * Qt tells nothing when the canvas moves or grows under a pointer which stands still - so it is checked where the
     * pointer is, once everything of a zoom or a scroll is in place.
     */
    @RequiredUIAccess
    private void checkPointer() {
        Canvas canvas = getLiveCanvas();
        QScrollArea area = myComponent;
        if (myCanvasMoving || canvas == null || area == null) {
            return;
        }

        // the viewport is under the mouse while the canvas in it is - not while it is over a scroll bar
        if (!area.viewport().underMouse()) {
            myState.pointerExited();
            return;
        }

        QPointF point = canvas.mapFromGlobal(new QPointF(QCursor.pos()));
        myState.pointerMoved(point.x(), point.y());
    }

    private void updateCanvasSize() {
        Canvas canvas = getLiveCanvas();
        if (canvas == null) {
            return;
        }

        Size2D size = myState.getCanvasSize();
        canvas.setFixedSize(size.width(), size.height());
    }

    private @Nullable Canvas getLiveCanvas() {
        Canvas canvas = myCanvas;
        return canvas == null || canvas.isDisposed() || myComponent == null ? null : canvas;
    }
}
