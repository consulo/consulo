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
package consulo.desktop.awt.editor.impl.gutter;

import consulo.codeEditor.markup.SeparatorPlacement;

/**
 * Where a separator resolved to in this editor's pixels.
 * <p>
 * Lives on the AWT side rather than beside the presentation types: pixel coordinates are not a
 * concept every editor implementation can supply, so they must not appear in the shared API.
 *
 * @param startX    left edge, already clipped to the visible area
 * @param endX      right edge, already clipped to the visible area
 * @param y         the separator's line
 * @param placement whether the separator sits above or below its highlighter
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public record AwtLineSeparatorBounds(int startX, int endX, int y, SeparatorPlacement placement) {
}
