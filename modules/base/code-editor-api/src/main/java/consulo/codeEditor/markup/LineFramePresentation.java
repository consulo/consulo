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

import consulo.ui.color.ColorValue;

/**
 * A frame around the lines of a range highlighter - from the line of its start to the line of its end - drawn through
 * the gutter and the text: closed on the left in the gutter, closed on the right in the text, open where the two
 * meet. What a code review draws around the lines a discussion is about.
 *
 * @param color the line of the frame
 * @author VISTALL
 * @since 2026-10-18
 */
public record LineFramePresentation(ColorValue color) {
}
