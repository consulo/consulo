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
import org.jspecify.annotations.Nullable;

/**
 * A run of lines marked with a colour.
 * <p>
 * Every editor implementation ships a painter for this type, so a provider can emit it without
 * supplying one — unlike the feature-owned presentation types, which are rendered only where a
 * painter is registered.
 *
 * @param startLine first logical line, inclusive
 * @param endLine   logical line past the last, exclusive
 * @param area      gutter band to occupy; unlike the feature-owned presentations this is chosen by
 *                  the caller, since the type carries no notion of where it belongs
 * @param color     the colour to mark the lines with
 * @param payload   handed back to the provider on click or tooltip
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public record FillColorLineMarkerPresentation(
    int startLine,
    int endLine,
    EditorGutterArea area,
    ColorValue color,
    @Nullable Object payload
) implements LineMarkerPresentation {
}
