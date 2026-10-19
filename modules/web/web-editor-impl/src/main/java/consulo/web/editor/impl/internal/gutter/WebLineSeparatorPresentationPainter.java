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
package consulo.web.editor.impl.internal.gutter;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.LineSeparatorPresentation;
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Turns a {@link LineSeparatorPresentation} into what the browser draws on the boundary of a line.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface WebLineSeparatorPresentationPainter<T extends LineSeparatorPresentation> {
    ExtensionPointCacheKey<WebLineSeparatorPresentationPainter, Function<Class, WebLineSeparatorPresentationPainter>> KEY =
        ExtensionPointCacheKey.create(
            "WebLineSeparatorPresentationPainter",
            ByClassGrouper.build(WebLineSeparatorPresentationPainter::getPresentationType)
        );

    @SuppressWarnings("unchecked")
    static <T extends LineSeparatorPresentation> @Nullable WebLineSeparatorPresentationPainter<T> findPainter(T presentation) {
        Function<Class, WebLineSeparatorPresentationPainter> call =
            Application.get().getExtensionPoint(WebLineSeparatorPresentationPainter.class).getOrBuildCache(KEY);
        return call.apply(presentation.getClass());
    }

    Class<T> getPresentationType();

    /**
     * @return what to draw, or null to draw nothing for this presentation
     */
    @Nullable
    LineSeparatorBand paint(T presentation, Editor editor);
}
