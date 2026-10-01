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

import consulo.ui.color.ColorValue;

/**
 * Chessboard drawn behind an image, so its transparent parts show as such.
 *
 * @param cellSize   side of one cell in pixels of the screen - it does not grow with the zoom
 * @param lightColor colour of the cells between the dark ones
 * @param darkColor  colour of the cell in the top left corner and every other one
 * @author VISTALL
 * @since 2026-10-01
 */
public record ImageViewerChessboard(int cellSize, ColorValue lightColor, ColorValue darkColor) {
}
