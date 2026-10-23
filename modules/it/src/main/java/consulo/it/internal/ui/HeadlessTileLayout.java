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

import consulo.ui.Component;
import consulo.ui.Length;
import consulo.ui.Space;
import consulo.ui.TransferHandler;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.TileLayout;
import org.jspecify.annotations.Nullable;

/**
 * Headless {@link TileLayout}: the tiles are kept in order, there is no width to wrap them at.
 *
 * @author VISTALL
 */
public class HeadlessTileLayout extends HeadlessLayoutBase<LayoutConstraint> implements TileLayout {
    private final Length myMinTileWidth;
    private final Space myGap;
    private @Nullable TransferHandler<Component> myTransferHandler;

    public HeadlessTileLayout(Length minTileWidth, Space gap) {
        myMinTileWidth = minTileWidth;
        myGap = gap;
    }

    public Length getMinTileWidth() {
        return myMinTileWidth;
    }

    public Space getGap() {
        return myGap;
    }

    @Override
    public void setTransferHandler(@Nullable TransferHandler<Component> handler) {
        myTransferHandler = handler;
    }

    @Override
    public @Nullable TransferHandler<Component> getTransferHandler() {
        return myTransferHandler;
    }
}
