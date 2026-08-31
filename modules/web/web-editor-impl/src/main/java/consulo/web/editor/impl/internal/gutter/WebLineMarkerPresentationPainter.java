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
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Renders one kind of {@link LineMarkerPresentation} in the browser. The counterpart of
 * {@code AwtLineMarkerPresentationPainter} - the presentation is all the two frontends share, each draws it
 * with what it has, and here that is a {@link GutterBand} for the css rather than a graphics call.
 * <p>
 * Lookup walks superclasses and interfaces, so a painter registered for a supertype serves its subtypes, and a
 * presentation nothing renders resolves to null and is skipped rather than failing.
 *
 * @author VISTALL
 * @since 2026-08-01
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface WebLineMarkerPresentationPainter<T extends LineMarkerPresentation> {
    ExtensionPointCacheKey<WebLineMarkerPresentationPainter, Function<Class, WebLineMarkerPresentationPainter>> KEY =
        ExtensionPointCacheKey.create(
            "WebLineMarkerPresentationPainter",
            ByClassGrouper.build(WebLineMarkerPresentationPainter::getPresentationType)
        );

    @SuppressWarnings("unchecked")
    static <T extends LineMarkerPresentation> @Nullable WebLineMarkerPresentationPainter<T> findPainter(T presentation) {
        Function<Class, WebLineMarkerPresentationPainter> call =
            Application.get().getExtensionPoint(WebLineMarkerPresentationPainter.class).getOrBuildCache(KEY);
        return call.apply(presentation.getClass());
    }

    /**
     * Presentation class this painter handles. Subclasses are handled too, unless they register their own.
     */
    Class<T> getPresentationType();

    /**
     * @return the band to draw, or null to draw nothing for this presentation
     */
    @Nullable
    GutterBand paint(T presentation, Editor editor);
}
