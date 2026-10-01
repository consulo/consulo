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
 * Lines between the pixels of an image: a faint one between every two pixels, a stronger one every {@code lineSpan}
 * pixels. Drawn only once the image is zoomed far enough for the pixels to be told apart.
 *
 * @param lineSpan  pixels between two strong lines
 * @param minZoom   zoom from which on the grid is drawn
 * @param lineColor colour of the lines - drawn translucent over the image
 * @author VISTALL
 * @since 2026-10-01
 */
public record ImageViewerGrid(int lineSpan, int minZoom, ColorValue lineColor) {
}
