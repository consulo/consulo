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
package consulo.desktop.awt.ui.impl.image;

import consulo.ui.image.Image;

/**
 * An image of an item cell which reacts to the mouse over it - the table forwards the moves and presses over the
 * icon area of the cell.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface DesktopAWTCellImage extends Image {
    /**
     * The mouse moved over the image, at {@code x} from its left edge and {@code y} from the top of a cell
     * {@code height} high.
     */
    default void mouseMoved(int x, int y, int height) {
    }

    default void mouseClicked(int x, int y, int height) {
    }

    default void mouseExited() {
    }
}
