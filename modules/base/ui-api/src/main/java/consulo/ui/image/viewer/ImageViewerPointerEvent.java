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

import consulo.ui.event.ComponentEvent;

/**
 * Where the pointer is over the image of an {@link ImageViewer}, in pixels of the image - fractional while zoomed in.
 *
 * @author VISTALL
 * @since 2026-10-01
 */
public final class ImageViewerPointerEvent extends ComponentEvent<ImageViewer> {
    private final boolean myOverImage;
    private final double myX;
    private final double myY;

    /**
     * The pointer left the image.
     */
    public ImageViewerPointerEvent(ImageViewer viewer) {
        super(viewer);
        myOverImage = false;
        myX = -1;
        myY = -1;
    }

    public ImageViewerPointerEvent(ImageViewer viewer, double x, double y) {
        super(viewer);
        myOverImage = true;
        myX = x;
        myY = y;
    }

    /**
     * @return false when the pointer is outside of the image - then {@link #getX()} and {@link #getY()} mean nothing
     */
    public boolean isOverImage() {
        return myOverImage;
    }

    public double getX() {
        return myX;
    }

    public double getY() {
        return myY;
    }
}
