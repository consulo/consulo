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

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.image.Image;
import consulo.ui.internal.UIInternal;
import org.jspecify.annotations.Nullable;

/**
 * Shows one image at a zoom of its own, scrolled when it does not fit and centred when it does - the viewer an image
 * file is opened in.

 * @author VISTALL
 * @since 2026-10-01
 */
public interface ImageViewer extends Component {
    static ImageViewer create() {
        return UIInternal.get()._Components_imageViewer();
    }

    @Nullable Image getImage();

    /**
     * Shows another image. The zoom is kept when the user changed it, otherwise - and always after no image - it is
     * picked anew, see {@link ImageViewerZoom#setSmartZoom}.
     */
    @RequiredUIAccess
    void setImage(@Nullable Image image);

    ImageViewerZoom getZoom();

    @Nullable ImageViewerChessboard getChessboard();

    /**
     * @param chessboard drawn behind the image so its transparent parts are told apart, null for none
     */
    @RequiredUIAccess
    void setChessboard(@Nullable ImageViewerChessboard chessboard);

    @Nullable ImageViewerGrid getGrid();

    /**
     * @param grid lines between the pixels of the image, null for none
     */
    @RequiredUIAccess
    void setGrid(@Nullable ImageViewerGrid grid);

    boolean isWheelZoomEnabled();

    /**
     * @param enabled whether control and the mouse wheel zoom, true by default
     */
    @RequiredUIAccess
    void setWheelZoomEnabled(boolean enabled);

    /**
     * Told which pixel of the image the pointer is over whenever it is another one - when the pointer moves, and when
     * a scroll, a zoom or another image moves the image under it. Not every move, the frontend may skip some.
     */
    default Disposable addPointerListener(ComponentEventListener<ImageViewer, ImageViewerPointerEvent> listener) {
        return addListener(ImageViewerPointerEvent.class, listener);
    }

    /**
     * Told every time the zoom changed, whoever changed it.
     */
    default Disposable addZoomListener(ComponentEventListener<ImageViewer, ImageViewerZoomEvent> listener) {
        return addListener(ImageViewerZoomEvent.class, listener);
    }
}
