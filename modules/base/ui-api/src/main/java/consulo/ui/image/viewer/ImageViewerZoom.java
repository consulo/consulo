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
package consulo.ui.image.viewer;

import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * Zoom of an {@link ImageViewer}: {@code 1} shows every pixel of the image as one pixel of the screen. Zooming in and
 * out steps by the square root of two, from a 32nd of the size up to 32 times of it.
 *
 * @author VISTALL
 * @since 2026-10-01
 */
public interface ImageViewerZoom {
    double getZoomFactor();

    /**
     * Sets the zoom as is, and forgets that the user changed it - so a {@link #setSmartZoom smart zoom} picks it anew
     * when the image or the size of the viewer changes. Does nothing while there is no image: the next one starts at
     * the smart zoom.
     */
    @RequiredUIAccess
    void setZoomFactor(double zoomFactor);

    @RequiredUIAccess
    void zoomIn();

    @RequiredUIAccess
    void zoomOut();

    /**
     * Zooms so the whole image fits into the viewer, within the limits of the zoom.
     */
    @RequiredUIAccess
    void zoomToFit();

    boolean canZoomIn();

    boolean canZoomOut();

    boolean canZoomToFit();

    /**
     * @return whether the zoom is one the user picked - by zooming in, out, to fit or by the wheel
     */
    boolean isZoomLevelChanged();

    void setZoomLevelChanged(boolean zoomLevelChanged);

    @Nullable Size2D getSmartZoom();

    /**
     * While the user did not pick a zoom, picks one whenever the image or the size of the viewer changes: an image
     * smaller than {@code preferredSize} on both sides is zoomed in by whole steps until about that size, an image
     * which does not fit is zoomed out to fit, any other is shown as is.
     *
     * @param preferredSize the size small images are zoomed up to, null to always show an image as is
     */
    @RequiredUIAccess
    void setSmartZoom(@Nullable Size2D preferredSize);
}
