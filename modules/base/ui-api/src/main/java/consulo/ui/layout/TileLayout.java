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
package consulo.ui.layout;

import consulo.ui.Component;
import consulo.ui.HasTransferHandler;
import consulo.ui.Length;
import consulo.ui.PseudoComponent;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

/**
 * Places its children as tiles in rows, in the order they were added.
 * <p/>
 * There are as many columns as tiles of the minimum width fit side by side, never more columns than tiles, and the
 * columns share the whole width equally - so two tiles stand side by side at half the width each, and drop under one
 * another once the layout is too narrow for both. Every tile of a row is as tall as the tallest of them.
 * <p/>
 * The height follows the width the layout is given, so inside a {@link ScrollableLayout} it scrolls up and down only.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
/**
 * Lays out its children in equal-width tiles, wrapping into rows.
 * <p>
 * Given a {@link consulo.ui.DragAndDropTransferHandler}, its children can be dragged. A drag carries the dragged child as
 * its only item, and a drop lands before ({@link consulo.ui.DragAndDropTransferHandler.DropPosition#ABOVE}) or after
 * ({@link consulo.ui.DragAndDropTransferHandler.DropPosition#BELOW}) the child it is made on; a drop past the last child
 * lands after it.
 */
public interface TileLayout extends Layout<LayoutConstraint>, HasTransferHandler<Component> {
    static TileLayout create(Length minTileWidth) {
        return create(minTileWidth, Space.MEDIUM);
    }

    static TileLayout create(Length minTileWidth, Space gap) {
        return UIInternal.get()._Layouts_tile(minTileWidth, gap);
    }

    @RequiredUIAccess
    default TileLayout add(PseudoComponent component) {
        return add(component.getComponent());
    }

    @RequiredUIAccess
    default TileLayout add(Component component) {
        return (TileLayout) add(component, LayoutConstraint.NONE);
    }
}
