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

import org.jspecify.annotations.Nullable;

/**
 * Declarative replacement for {@link LineSeparatorRenderer}.
 * <p>
 * A provider rather than a bare presentation so that colours are resolved when the separator is
 * drawn, and therefore follow colour scheme changes.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public interface LineSeparatorPresentationProvider {
    /**
     * @return the separator to draw, or {@code null} to draw nothing this time
     */
    @Nullable
    LineSeparatorPresentation buildPresentation(LineMarkerPresentationContext context);
}
