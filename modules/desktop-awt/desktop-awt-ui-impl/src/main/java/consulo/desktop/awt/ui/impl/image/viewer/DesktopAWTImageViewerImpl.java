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
package consulo.desktop.awt.ui.impl.image.viewer;

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.Point2D;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Magnificator;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.paint.LinePainter2D;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.viewer.ImageViewer;
import consulo.ui.image.viewer.ImageViewerChessboard;
import consulo.ui.image.viewer.ImageViewerGrid;
import consulo.ui.image.viewer.ImageViewerZoom;
import consulo.ui.impl.image.viewer.ImageViewerState;
import consulo.ui.style.StyleManager;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;

/**
 * The canvas is a component of the size of the image at its zoom, centred in a container which is as big as the
 * viewport or as the canvas, whichever is bigger - so a small image stands in the middle and a big one scrolls.
 *
 * @author VISTALL
 * @since 2026-10-01
 */
public class DesktopAWTImageViewerImpl extends SwingComponentDelegate<DesktopAWTImageViewerImpl.MyScrollPane>
    implements ImageViewer, ImageViewerState.Host {

    public class MyScrollPane extends JBScrollPane implements FromSwingComponentWrapper {
        MyScrollPane(java.awt.Component view) {
            super(view);
            setBorder(JBUI.Borders.empty());
        }

        @Override
        protected void processMouseWheelEvent(MouseWheelEvent e) {
            if (e.isControlDown() && e.getWheelRotation() != 0) {
                Point point = SwingUtilities.convertPoint(this, e.getPoint(), getViewport());
                if (myState.wheelZoom(e.getWheelRotation() < 0, new Point2D(point.x, point.y))) {
                    e.consume();
                    return;
                }
            }
            super.processMouseWheelEvent(e);
        }

        @Override
        public Component toUIComponent() {
            return DesktopAWTImageViewerImpl.this;
        }
    }

    private class CanvasContainer extends JPanel implements Scrollable {
        CanvasContainer() {
            super(null);

            add(myCanvas);

            putClientProperty(Magnificator.CLIENT_PROPERTY_KEY, (Magnificator) (scale, at) -> {
                Point locationBefore = myCanvas.getLocation();
                myState.magnify(scale, getViewportCenter());
                return new Point(
                    (int) ((at.x - Math.max(scale > 1.0 ? locationBefore.x : 0, 0)) * scale),
                    (int) ((at.y - Math.max(scale > 1.0 ? locationBefore.y : 0, 0)) * scale)
                );
            });
        }

        @Override
        public void doLayout() {
            Dimension canvasSize = myCanvas.getPreferredSize();
            myCanvas.setBounds(
                Math.max(0, (getWidth() - canvasSize.width) / 2),
                Math.max(0, (getHeight() - canvasSize.height) / 2),
                canvasSize.width,
                canvasSize.height
            );
        }

        @Override
        public Dimension getPreferredSize() {
            return myCanvas.getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (StyleManager.get().getCurrentStyle().isDark()) {
                g.setColor(UIUtil.getControlColor().brighter());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return JBUI.scale(16);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            // as wide as the viewport while the canvas is narrower, so the canvas can be centred in it
            return getParent() instanceof JViewport viewport && viewport.getWidth() > getPreferredSize().width;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
        }
    }

    private class Canvas extends JComponent {
        Canvas() {
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    myState.pointerMoved(e.getX(), e.getY());
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    myState.pointerMoved(e.getX(), e.getY());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    myState.pointerExited();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    toAWTComponent().requestFocusInWindow();
                    redispatch(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    redispatch(e);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    redispatch(e);
                }
            };

            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }

        /**
         * The listeners of the viewer - a click, a context menu - sit on the scroll pane, which does not see what this
         * component took for its own listeners.
         */
        private void redispatch(MouseEvent e) {
            MyScrollPane scrollPane = toAWTComponent();
            scrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, scrollPane));
        }

        @Override
        public Dimension getPreferredSize() {
            Size2D size = myState.getCanvasSize();
            return new Dimension(size.width(), size.height());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Image image = myState.getImage();
            Size2D area = myState.getImageAreaSize();
            if (image == null || area.isEmpty()) {
                return;
            }

            int insets = ImageViewerState.INSETS;
            Graphics2D g2d = (Graphics2D) g.create(insets, insets, area.width(), area.height());
            try {
                paintChessboard(g2d, area);
                paintImage(g2d, image, area);
                paintGrid(g2d, image, area);
            }
            finally {
                g2d.dispose();
            }
        }

        private void paintChessboard(Graphics2D g2d, Size2D area) {
            ImageViewerChessboard chessboard = myState.getChessboard();
            if (chessboard == null) {
                return;
            }

            double scaleX = g2d.getTransform().getScaleX();
            double scaleY = g2d.getTransform().getScaleY();
            int cellSize = Math.max(1, chessboard.cellSize());
            double cellW = Math.round(cellSize * scaleX) / scaleX;
            double cellH = Math.round(cellSize * scaleY) / scaleY;

            Rectangle clip = clipOf(g2d, area);

            Object oldAntialiasing = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            g2d.setColor(TargetAWT.to(chessboard.lightColor()));
            g2d.fill(clip);

            g2d.setColor(TargetAWT.to(chessboard.darkColor()));
            Rectangle2D.Double cell = new Rectangle2D.Double();
            int fromColumn = (int) Math.floor(clip.x / cellW);
            int toColumn = (int) Math.ceil((clip.x + clip.width) / cellW);
            int fromRow = (int) Math.floor(clip.y / cellH);
            int toRow = (int) Math.ceil((clip.y + clip.height) / cellH);
            for (int column = fromColumn; column <= toColumn; column++) {
                for (int row = fromRow; row <= toRow; row++) {
                    if ((column + row) % 2 == 0) {
                        cell.setRect(column * cellW, row * cellH, cellW, cellH);
                        g2d.fill(cell);
                    }
                }
            }

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
        }

        private void paintImage(Graphics2D g2d, Image image, Size2D area) {
            Icon icon = TargetAWT.to(image);
            int iconWidth = icon.getIconWidth();
            int iconHeight = icon.getIconHeight();
            if (iconWidth <= 0 || iconHeight <= 0) {
                return;
            }

            Graphics2D imageGraphics = (Graphics2D) g2d.create();
            try {
                if (myState.isSmoothScaling()) {
                    imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    imageGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                }
                else {
                    // no source pixel is mixed with another when zoomed in - each one shows as a square
                    imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    imageGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                }

                // a svg is drawn as vectors under the scale, so it stays sharp at every zoom
                imageGraphics.scale(area.width() / (double) iconWidth, area.height() / (double) iconHeight);
                icon.paintIcon(this, imageGraphics, 0, 0);
            }
            finally {
                imageGraphics.dispose();
            }
        }

        @SuppressWarnings("UseJBColor")
        private void paintGrid(Graphics2D g2d, Image image, Size2D area) {
            ImageViewerGrid grid = myState.getVisibleGrid();
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            if (grid == null || imageWidth <= 0 || imageHeight <= 0) {
                return;
            }

            double zoomX = (double) area.width() / imageWidth;
            double zoomY = (double) area.height() / imageHeight;

            int lineRGB = TargetAWT.to(grid.lineColor()).getRGB() & 0xFFFFFF;
            Color auxColor = new Color(lineRGB | 0x26000000, true);
            Color mainColor = new Color(lineRGB | 0x4D000000, true);
            int lineSpan = Math.max(1, grid.lineSpan());

            Rectangle clip = clipOf(g2d, area);
            int fromX = Math.max(1, (int) Math.floor(clip.x / zoomX));
            int toX = Math.min(imageWidth - 1, (int) Math.ceil((clip.x + clip.width) / zoomX));
            for (int dx = fromX; dx <= toX; dx++) {
                g2d.setColor(dx % lineSpan == 0 ? mainColor : auxColor);
                double x = dx * zoomX;
                LinePainter2D.paint(g2d, x, clip.y, x, clip.y + clip.height, LinePainter2D.StrokeType.CENTERED_CAPS_SQUARE, 0.5);
            }

            int fromY = Math.max(1, (int) Math.floor(clip.y / zoomY));
            int toY = Math.min(imageHeight - 1, (int) Math.ceil((clip.y + clip.height) / zoomY));
            for (int dy = fromY; dy <= toY; dy++) {
                g2d.setColor(dy % lineSpan == 0 ? mainColor : auxColor);
                double y = dy * zoomY;
                LinePainter2D.paint(g2d, clip.x, y, clip.x + clip.width, y, LinePainter2D.StrokeType.CENTERED_CAPS_SQUARE, 0.5);
            }
        }

        private static Rectangle clipOf(Graphics2D g2d, Size2D area) {
            Rectangle clip = g2d.getClipBounds();
            Rectangle whole = new Rectangle(0, 0, area.width(), area.height());
            return clip == null ? whole : clip.intersection(whole);
        }
    }

    private final ImageViewerState myState = new ImageViewerState(this, this);
    private final Canvas myCanvas = new Canvas();

    private boolean myPointerCheckQueued;

    @Override
    protected MyScrollPane createComponent() {
        MyScrollPane scrollPane = new MyScrollPane(new CanvasContainer());
        scrollPane.setFocusable(true);
        // the scroll pane, not the viewport - which gets smaller whenever a scroll bar is shown
        scrollPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                myState.viewportResized();
            }
        });
        // a scroll moves the canvas under the pointer
        scrollPane.getViewport().addChangeListener(e -> queuePointerCheck());
        return scrollPane;
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
        if (!isInitialized()) {
            return null;
        }

        // the whole scroll pane - the viewport loses the room of a scroll bar while one is shown
        Rectangle inner = SwingUtilities.calculateInnerArea(toAWTComponent(), null);
        return new Size2D(inner.width, inner.height);
    }

    @RequiredUIAccess
    @Override
    public void imageChanged() {
        relayout();
        myCanvas.repaint();
        queuePointerCheck();
    }

    @RequiredUIAccess
    @Override
    public void zoomChanged(double oldZoomFactor, @Nullable Point2D anchor) {
        MyScrollPane scrollPane = toAWTComponent();
        JViewport viewport = scrollPane.getViewport();
        Point position = viewport.getViewPosition();
        // a canvas smaller than the viewport is centred in it, not scrolled
        Point origin = new Point(position.x - myCanvas.getX(), position.y - myCanvas.getY());
        Point2D point = anchor != null ? anchor : getViewportCenter();

        relayout();

        Dimension viewSize = viewport.getViewSize();
        Dimension extent = viewport.getExtentSize();
        int x = Math.min(myState.anchoredScroll(origin.x, point.x(), oldZoomFactor), Math.max(0, viewSize.width - extent.width));
        int y = Math.min(myState.anchoredScroll(origin.y, point.y(), oldZoomFactor), Math.max(0, viewSize.height - extent.height));
        viewport.setViewPosition(new Point(x, y));

        myCanvas.repaint();
        queuePointerCheck();
    }

    @RequiredUIAccess
    @Override
    public void appearanceChanged() {
        myCanvas.repaint();
    }

    /**
     * Swing tells nothing when the canvas moves or grows under a pointer which stands still - so it is checked where
     * the pointer is, once everything of a zoom or a scroll is in place.
     */
    private void queuePointerCheck() {
        if (myPointerCheckQueued) {
            return;
        }

        myPointerCheckQueued = true;
        SwingUtilities.invokeLater(() -> {
            myPointerCheckQueued = false;

            Point point = myCanvas.isShowing() ? myCanvas.getMousePosition() : null;
            if (point != null) {
                myState.pointerMoved(point.x, point.y);
            }
            else {
                myState.pointerExited();
            }
        });
    }

    private void relayout() {
        // laid out right away, the view position set after a zoom has to be checked against the new size
        myCanvas.revalidate();
        toAWTComponent().validate();
    }

    private Point2D getViewportCenter() {
        Dimension extent = toAWTComponent().getViewport().getExtentSize();
        return new Point2D(extent.width / 2, extent.height / 2);
    }
}
