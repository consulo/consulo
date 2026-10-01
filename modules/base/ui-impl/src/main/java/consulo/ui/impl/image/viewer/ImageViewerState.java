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
package consulo.ui.impl.image.viewer;

import consulo.ui.Point2D;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.viewer.ImageViewer;
import consulo.ui.image.viewer.ImageViewerChessboard;
import consulo.ui.image.viewer.ImageViewerGrid;
import consulo.ui.image.viewer.ImageViewerPointerEvent;
import consulo.ui.image.viewer.ImageViewerZoom;
import consulo.ui.image.viewer.ImageViewerZoomEvent;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Everything of an {@link ImageViewer} but its pixels: what is shown, at which zoom, and how the zoom moves. Every
 * frontend keeps one and only draws what it says - so the zoom steps, the limits and the smart zoom are the same
 * everywhere.
 * <p/>
 * Positions are in pixels of the screen. The canvas is the image at its zoom with {@link #INSETS} of empty border on
 * every side; the viewport is the visible part of the viewer the canvas is scrolled in.
 *
 * @author VISTALL
 * @since 2026-10-01
 */
public final class ImageViewerState implements ImageViewerZoom {
    /**
     * What the frontend does when the state changed.
     */
    public interface Host {
        /**
         * @return size of the visible part of the viewer while no scroll bar is shown, null while it was not laid out
         * yet - the scroll bars come and go with the zoom, which must not change the zoom again
         */
        @Nullable Size2D getViewportSize();

        /**
         * Another image is shown - the canvas has other content, and maybe another size and another zoom.
         */
        @RequiredUIAccess
        void imageChanged();

        /**
         * The canvas has another size because of the zoom - see {@link #anchoredScroll} for where to scroll.
         *
         * @param anchor point of the viewport which has to stay over the same point of the image, null for its centre
         */
        @RequiredUIAccess
        void zoomChanged(double oldZoomFactor, @Nullable Point2D anchor);

        /**
         * The chessboard or the grid changed - the canvas only has to be painted again.
         */
        @RequiredUIAccess
        void appearanceChanged();
    }

    /**
     * Empty border around the image, in pixels of the screen.
     */
    public static final int INSETS = 2;

    private static final double ZOOM_RATIO = Math.sqrt(2);
    // ten steps each way - exact, unlike the tenth power of the square root of two
    private static final double ZOOM_UPPER_LIMIT = 32;
    private static final double ZOOM_LOWER_LIMIT = 1 / 32d;

    // a zoom reached by steps is a few ulps off the exact one - that must not add a pixel to the image
    private static final double SIZE_TOLERANCE = 1.0e-6;

    private final ImageViewer myViewer;
    private final Host myHost;

    private @Nullable Image myImage;
    private @Nullable ImageViewerChessboard myChessboard;
    private @Nullable ImageViewerGrid myGrid;
    private boolean myWheelZoomEnabled = true;

    private double myZoomFactor = 1;
    private boolean myZoomLevelChanged;
    private @Nullable Size2D mySmartZoom;
    private @Nullable Size2D myViewportSize;

    // what the pointer listeners were told last - a frontend checks the pointer again whenever the canvas moved
    // under it, which mostly finds it where it was
    private boolean myPointerOverImage;
    private double myPointerX;
    private double myPointerY;

    public ImageViewerState(ImageViewer viewer, Host host) {
        myViewer = viewer;
        myHost = host;
    }

    public @Nullable Image getImage() {
        return myImage;
    }

    @RequiredUIAccess
    public void setImage(@Nullable Image image) {
        Image oldImage = myImage;
        double oldZoomFactor = myZoomFactor;
        myImage = image;

        // the first image of the viewer, or one at a zoom the user did not pick, starts at the smart zoom
        if (image != null && (oldImage == null || !myZoomLevelChanged)) {
            Double smartZoomFactor = getSmartZoomFactor();
            myZoomFactor = smartZoomFactor != null ? smartZoomFactor : 1;
            myZoomLevelChanged = false;
        }

        // the pointer is over another image now, even at the same place
        myPointerX = Double.NaN;
        myPointerY = Double.NaN;

        myHost.imageChanged();

        if (Double.compare(oldZoomFactor, myZoomFactor) != 0) {
            fireZoomChanged();
        }
        if (image == null) {
            pointerExited();
        }
    }

    public @Nullable ImageViewerChessboard getChessboard() {
        return myChessboard;
    }

    @RequiredUIAccess
    public void setChessboard(@Nullable ImageViewerChessboard chessboard) {
        myChessboard = chessboard;
        myHost.appearanceChanged();
    }

    public @Nullable ImageViewerGrid getGrid() {
        return myGrid;
    }

    @RequiredUIAccess
    public void setGrid(@Nullable ImageViewerGrid grid) {
        myGrid = grid;
        myHost.appearanceChanged();
    }

    public boolean isWheelZoomEnabled() {
        return myWheelZoomEnabled;
    }

    public void setWheelZoomEnabled(boolean wheelZoomEnabled) {
        myWheelZoomEnabled = wheelZoomEnabled;
    }

    /**
     * @return size of the image at the zoom, without the insets - empty while there is no image
     */
    public Size2D getImageAreaSize() {
        Image image = myImage;
        if (image == null) {
            return Size2D.ZERO;
        }
        return new Size2D(scaledSize(image.getWidth()), scaledSize(image.getHeight()));
    }

    private int scaledSize(int size) {
        return Math.max(1, (int) Math.ceil(size * myZoomFactor - SIZE_TOLERANCE));
    }

    /**
     * @return size of the canvas - the image at the zoom with the insets around it
     */
    public Size2D getCanvasSize() {
        Size2D area = getImageAreaSize();
        if (area.isEmpty()) {
            return Size2D.ZERO;
        }
        return new Size2D(area.width() + INSETS * 2, area.height() + INSETS * 2);
    }

    /**
     * @return whether the pixels of the image are smoothed when scaled - only when zoomed out, so single pixels stay
     * sharp when zoomed in
     */
    public boolean isSmoothScaling() {
        return myZoomFactor < 1;
    }

    /**
     * @return the grid when it is drawn at the current zoom, otherwise null
     */
    public @Nullable ImageViewerGrid getVisibleGrid() {
        ImageViewerGrid grid = myGrid;
        if (grid == null || myImage == null || myZoomFactor < grid.minZoom()) {
            return null;
        }
        return grid;
    }

    /**
     * The viewport may have got another size - picks the zoom anew when the user did not pick one.
     */
    @RequiredUIAccess
    public void viewportResized() {
        Size2D viewportSize = myHost.getViewportSize();
        if (Objects.equals(myViewportSize, viewportSize)) {
            return;
        }

        myViewportSize = viewportSize;
        applySmartZoom();
    }

    /**
     * Control and the mouse wheel were turned over the viewer.
     *
     * @param up     whether the wheel was turned up, away from the user - which zooms out
     * @param anchor where the pointer is in the viewport
     * @return whether the wheel zooms, even when at a limit it did not - when not, the frontend scrolls as usual
     */
    @RequiredUIAccess
    public boolean wheelZoom(boolean up, Point2D anchor) {
        if (!myWheelZoomEnabled || myImage == null) {
            return false;
        }

        // at a limit the wheel does nothing - and does not scroll either
        applyZoom(up ? getNextZoomOut() : getNextZoomIn(), anchor, true);
        return true;
    }

    /**
     * The user pinched a touchpad over the viewer.
     *
     * @param scale  how much bigger the image is wanted - below 1 for smaller
     * @param anchor where the pinch is in the viewport
     */
    @RequiredUIAccess
    public void magnify(double scale, Point2D anchor) {
        if (myImage == null || scale <= 0) {
            return;
        }

        double zoomFactor = scale > 1
            ? Math.max(myZoomFactor, Math.min(myZoomFactor * scale, getMaximumZoomFactor()))
            : Math.min(myZoomFactor, Math.max(myZoomFactor * scale, getMinimumZoomFactor()));
        applyZoom(zoomFactor, anchor, true);
    }

    /**
     * The pointer is over the canvas - it moved, or the canvas moved under it. Listeners are told only when the point
     * of the image under it is another one.
     *
     * @param canvasX position from the left of the canvas, the insets included
     * @param canvasY position from the top of the canvas, the insets included
     */
    @RequiredUIAccess
    public void pointerMoved(double canvasX, double canvasY) {
        Image image = myImage;
        Size2D area = getImageAreaSize();
        double x = canvasX - INSETS;
        double y = canvasY - INSETS;
        if (image == null || area.isEmpty() || x < 0 || y < 0 || x >= area.width() || y >= area.height()) {
            pointerExited();
            return;
        }

        double imageX = Math.round(x / area.width() * image.getWidth() * 1000) / 1000d;
        double imageY = Math.round(y / area.height() * image.getHeight() * 1000) / 1000d;
        if (myPointerOverImage && imageX == myPointerX && imageY == myPointerY) {
            return;
        }

        myPointerOverImage = true;
        myPointerX = imageX;
        myPointerY = imageY;
        myViewer.getListenerDispatcher(ImageViewerPointerEvent.class).onEvent(new ImageViewerPointerEvent(myViewer, imageX, imageY));
    }

    /**
     * The pointer is not over the canvas - it left, or the canvas moved away from under it.
     */
    @RequiredUIAccess
    public void pointerExited() {
        if (!myPointerOverImage) {
            return;
        }

        myPointerOverImage = false;
        myViewer.getListenerDispatcher(ImageViewerPointerEvent.class).onEvent(new ImageViewerPointerEvent(myViewer));
    }

    /**
     * Where to scroll along one axis after the zoom changed from {@code oldZoomFactor} to the current one, so that the
     * point of the viewport at {@code anchor} shows the same point of the image as before.
     *
     * @param origin where the edge of the viewport was on the canvas before the zoom: the scroll offset, or the gap
     *               before a canvas centred in the viewport, as a negative number
     * @param anchor position of the point which stays, from the edge of the viewport
     * @return new scroll offset, the frontend still limits it to its scroll range
     */
    public int anchoredScroll(int origin, int anchor, double oldZoomFactor) {
        if (oldZoomFactor <= 0) {
            return Math.max(0, origin);
        }
        // the insets around the image keep their size at every zoom
        double imagePoint = (origin + anchor - INSETS) / oldZoomFactor;
        return (int) Math.max(0, Math.round(imagePoint * myZoomFactor + INSETS - anchor));
    }

    @Override
    public double getZoomFactor() {
        return myZoomFactor;
    }

    @RequiredUIAccess
    @Override
    public void setZoomFactor(double zoomFactor) {
        // the next image starts at the smart zoom anyway
        if (zoomFactor <= 0 || myImage == null) {
            return;
        }

        myZoomLevelChanged = false;
        applyZoom(zoomFactor, null, false);
    }

    @RequiredUIAccess
    @Override
    public void zoomIn() {
        if (canZoomIn()) {
            applyZoom(getNextZoomIn(), null, true);
        }
    }

    @RequiredUIAccess
    @Override
    public void zoomOut() {
        if (canZoomOut()) {
            applyZoom(getNextZoomOut(), null, true);
        }
    }

    @RequiredUIAccess
    @Override
    public void zoomToFit() {
        Double fitZoomFactor = getFitZoomFactor();
        if (fitZoomFactor == null) {
            return;
        }

        // picked even when the image fits already - so it is kept for the next image
        myZoomLevelChanged = true;
        applyZoom(fitZoomFactor, null, true);
    }

    @Override
    public boolean canZoomIn() {
        // a zoom reached by steps may be a few ulps below the limit
        return myImage != null && getMaximumZoomFactor() - myZoomFactor > 1.0e-9;
    }

    @Override
    public boolean canZoomOut() {
        // ignore small differences caused by floating-point arithmetic
        return myImage != null && myZoomFactor - 1.0e-14 > getMinimumZoomFactor();
    }

    @Override
    public boolean canZoomToFit() {
        Double fitZoomFactor = getFitZoomFactor();
        return fitZoomFactor != null && Math.abs(fitZoomFactor - myZoomFactor) > 1.0e-9;
    }

    @Override
    public boolean isZoomLevelChanged() {
        return myZoomLevelChanged;
    }

    @Override
    public void setZoomLevelChanged(boolean zoomLevelChanged) {
        myZoomLevelChanged = zoomLevelChanged;
    }

    @Override
    public @Nullable Size2D getSmartZoom() {
        return mySmartZoom;
    }

    @RequiredUIAccess
    @Override
    public void setSmartZoom(@Nullable Size2D preferredSize) {
        mySmartZoom = preferredSize;
        applySmartZoom();
    }

    @RequiredUIAccess
    private void applySmartZoom() {
        if (myImage == null || myZoomLevelChanged || mySmartZoom == null) {
            return;
        }

        Double smartZoomFactor = getSmartZoomFactor();
        if (smartZoomFactor != null) {
            applyZoom(smartZoomFactor, null, false);
        }
    }

    /**
     * @param picked whether the user picked the zoom - remembered only when the zoom changed
     * @return whether the zoom changed
     */
    @RequiredUIAccess
    private boolean applyZoom(double zoomFactor, @Nullable Point2D anchor, boolean picked) {
        double oldZoomFactor = myZoomFactor;
        if (Double.compare(oldZoomFactor, zoomFactor) == 0 || zoomFactor <= 0) {
            return false;
        }

        myZoomFactor = zoomFactor;
        if (picked) {
            myZoomLevelChanged = true;
        }
        myHost.zoomChanged(oldZoomFactor, anchor);
        fireZoomChanged();
        return true;
    }

    @RequiredUIAccess
    private void fireZoomChanged() {
        myViewer.getListenerDispatcher(ImageViewerZoomEvent.class).onEvent(new ImageViewerZoomEvent(myViewer, myZoomFactor));
    }

    // a zoom set from outside of the limits never steps back the wrong way
    private double getNextZoomIn() {
        return Math.max(myZoomFactor, Math.min(myZoomFactor * ZOOM_RATIO, getMaximumZoomFactor()));
    }

    private double getNextZoomOut() {
        return Math.min(myZoomFactor, Math.max(myZoomFactor / ZOOM_RATIO, getMinimumZoomFactor()));
    }

    private double limitZoom(double zoomFactor) {
        return Math.max(getMinimumZoomFactor(), Math.min(zoomFactor, getMaximumZoomFactor()));
    }

    private double getMaximumZoomFactor() {
        return ZOOM_UPPER_LIMIT;
    }

    private double getMinimumZoomFactor() {
        Image image = myImage;
        double factor = image != null && image.getWidth() > 0 ? 1.0d / image.getWidth() : 0;
        return Math.max(factor, ZOOM_LOWER_LIMIT);
    }

    private @Nullable Double getFitZoomFactor() {
        Image image = myImage;
        Size2D canvas = getValidViewportArea();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0 || canvas == null) {
            return null;
        }
        return limitZoom(Math.min((double) canvas.width() / image.getWidth(), (double) canvas.height() / image.getHeight()));
    }

    private @Nullable Double getSmartZoomFactor() {
        Image image = myImage;
        Size2D preferredSize = mySmartZoom;
        if (image == null || preferredSize == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        if (width < preferredSize.width() && height < preferredSize.height()) {
            return limitZoom(Math.ceil((preferredSize.width() / (double) width + preferredSize.height() / (double) height) / 2.0d));
        }

        Size2D canvas = getValidViewportArea();
        if (canvas == null) {
            return null;
        }

        if (canvas.width() < width || canvas.height() < height) {
            return limitZoom(Math.min((double) canvas.height() / height, (double) canvas.width() / width));
        }
        return 1.0d;
    }

    private @Nullable Size2D getValidViewportArea() {
        Size2D viewport = myHost.getViewportSize();
        if (viewport == null) {
            return null;
        }

        int width = viewport.width() - INSETS * 2;
        int height = viewport.height() - INSETS * 2;
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new Size2D(width, height);
    }
}
