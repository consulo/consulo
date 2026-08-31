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

/**
 * A semantic description of a line separator drawn across the editor content.
 * <p>
 * Counterpart of {@link LineMarkerPresentation} for the content area rather than the gutter.
 * Carries no position: the platform already derives the separator's y from the highlighter's
 * {@link SeparatorPlacement}, and its horizontal extent from the visible clip.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public interface LineSeparatorPresentation {
}
