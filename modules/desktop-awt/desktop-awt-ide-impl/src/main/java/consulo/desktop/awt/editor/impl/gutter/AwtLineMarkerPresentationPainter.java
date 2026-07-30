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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.function.Function;

/**
 * Draws one kind of {@link LineMarkerPresentation} with AWT.
 * <p>
 * The extension point is deliberately <b>platform-specific</b> — there is no shared painter
 * abstraction, because a cross-platform drawing interface would just reintroduce an AWT-shaped API
 * one level down. SWT and web declare their own painter extension points over their own drawing
 * types, and {@link LineMarkerPresentation} stays the only contract they share.
 * <p>
 * Lookup goes through {@link ByClassGrouper}, which resolves a presentation class against the exact type
 * first and then walks superclasses and interfaces. Two consequences are intentional: a painter
 * registered for a supertype serves its subtypes ("extend the semantics, inherit the look"), and an
 * unregistered presentation type resolves to {@code null} so the presentation is simply skipped instead of failing.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AwtLineMarkerPresentationPainter<T extends LineMarkerPresentation> {
    ExtensionPointCacheKey<AwtLineMarkerPresentationPainter, Function<Class, AwtLineMarkerPresentationPainter>> KEY =
        ExtensionPointCacheKey.create(
            "AwtLineMarkerPresentationPainter",
            ByClassGrouper.build(AwtLineMarkerPresentationPainter::getPresentationType)
        );

    /**
     * @return the painter for this presentation, or {@code null} when nothing renders this kind — in which
     * case the presentation must be skipped for hit-testing too, otherwise the gutter grows invisible
     * clickable regions.
     */
    @SuppressWarnings("unchecked")
    static <T extends LineMarkerPresentation> @Nullable AwtLineMarkerPresentationPainter<T> findPainter(T presentation) {
        Function<Class, AwtLineMarkerPresentationPainter> call =
            Application.get().getExtensionPoint(AwtLineMarkerPresentationPainter.class).getOrBuildCache(KEY);
        return call.apply(presentation.getClass());
    }

    /**
     * Presentation class this painter handles. Subclasses of it are handled too, unless they register their
     * own painter.
     */
    Class<T> getPresentationType();

    /**
     * @param bounds the presentation's line range already resolved to gutter pixels — x/width from
     *               {@link LineMarkerPresentation#area()}, y/height from the line range. Painters must not
     *               recompute either; independently recomputed geometry is what let painting and
     *               hit-testing drift apart in the imperative version.
     */
    void paint(T presentation, Editor editor, Graphics2D g, Rectangle bounds);
}
