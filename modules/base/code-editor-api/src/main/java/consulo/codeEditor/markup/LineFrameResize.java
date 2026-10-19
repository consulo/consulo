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
package consulo.codeEditor.markup;

import consulo.localize.LocalizeValue;

/**
 * Lets the user drag the top or the bottom edge of a {@link LineFramePresentation}. The frontend offers it over an
 * edge - the resize pointer and the tooltip while it is enabled, the tooltip alone while it is not - and reports the
 * drag to the listener.
 *
 * @param tooltip  shown over an edge: what dragging it does, or why it cannot be dragged
 * @param enabled  whether the edges can be dragged
 * @param listener told about the drag
 * @author VISTALL
 * @since 2026-10-19
 */
public record LineFrameResize(LocalizeValue tooltip, boolean enabled, LineFrameResizeListener listener) {
}
