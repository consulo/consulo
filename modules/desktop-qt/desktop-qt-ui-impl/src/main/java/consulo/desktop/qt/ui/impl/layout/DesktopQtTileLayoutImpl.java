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
package consulo.desktop.qt.ui.impl.layout;

import consulo.desktop.qt.ui.impl.DesktopQtSpace;
import consulo.ui.Length;
import consulo.ui.Space;
import consulo.ui.layout.LayoutConstraint;
import consulo.ui.layout.TileLayout;
import io.qt.widgets.QLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class DesktopQtTileLayoutImpl extends DesktopQtLayoutComponent<LayoutConstraint, Object> implements TileLayout {
    private final Length myMinTileWidth;
    private final Space myGap;

    public DesktopQtTileLayoutImpl(Length minTileWidth, Space gap) {
        myMinTileWidth = minTileWidth;
        myGap = gap;
    }

    @Override
    protected @Nullable QLayout createLayout() {
        return new DesktopQtTileLayout(myMinTileWidth, DesktopQtSpace.toPixels(myGap));
    }
}
