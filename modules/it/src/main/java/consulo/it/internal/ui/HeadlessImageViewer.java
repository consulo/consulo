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
package consulo.it.internal.ui;


import consulo.ui.Point2D;
import consulo.ui.Size2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.viewer.ImageViewer;
import consulo.ui.image.viewer.ImageViewerChessboard;
import consulo.ui.image.viewer.ImageViewerGrid;
import consulo.ui.image.viewer.ImageViewerZoom;
import consulo.ui.impl.image.viewer.ImageViewerState;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessImageViewer extends HeadlessComponentBase implements ImageViewer, ImageViewerState.Host {
    private final ImageViewerState myState = new ImageViewerState(this, this);

    @Override
    public @Nullable Image getImage() {
        return myState.getImage();
    }

    @Override
    @RequiredUIAccess
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

    @Override
    @RequiredUIAccess
    public void setChessboard(@Nullable ImageViewerChessboard chessboard) {
        myState.setChessboard(chessboard);
    }

    @Override
    public @Nullable ImageViewerGrid getGrid() {
        return myState.getGrid();
    }

    @Override
    @RequiredUIAccess
    public void setGrid(@Nullable ImageViewerGrid grid) {
        myState.setGrid(grid);
    }

    @Override
    public boolean isWheelZoomEnabled() {
        return myState.isWheelZoomEnabled();
    }

    @Override
    @RequiredUIAccess
    public void setWheelZoomEnabled(boolean enabled) {
        myState.setWheelZoomEnabled(enabled);
    }

    @Override
    public @Nullable Size2D getViewportSize() {
        return null;
    }

    @Override
    @RequiredUIAccess
    public void imageChanged() {
    }

    @Override
    @RequiredUIAccess
    public void zoomChanged(double oldZoomFactor, @Nullable Point2D anchor) {
    }

    @Override
    @RequiredUIAccess
    public void appearanceChanged() {
    }
}
