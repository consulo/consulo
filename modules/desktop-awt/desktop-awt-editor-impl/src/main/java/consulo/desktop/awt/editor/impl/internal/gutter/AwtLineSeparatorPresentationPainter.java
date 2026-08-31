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
package consulo.desktop.awt.editor.impl.internal.gutter;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;

import consulo.codeEditor.markup.LineSeparatorPresentation;
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.function.Function;

/**
 * Draws one kind of {@link LineSeparatorPresentation} with AWT.
 * <p>
 * Content-area counterpart of {@link AwtLineMarkerPresentationPainter}, with the same lookup rules:
 * a painter registered for a supertype serves its subtypes, and an unregistered type resolves to
 * {@code null} and is skipped.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface AwtLineSeparatorPresentationPainter<T extends LineSeparatorPresentation> {
    ExtensionPointCacheKey<AwtLineSeparatorPresentationPainter, Function<Class, AwtLineSeparatorPresentationPainter>> KEY =
        ExtensionPointCacheKey.create(
            "AwtLineSeparatorPresentationPainter",
            ByClassGrouper.build(AwtLineSeparatorPresentationPainter::getPresentationType)
        );

    @SuppressWarnings("unchecked")
    static <T extends LineSeparatorPresentation> @Nullable AwtLineSeparatorPresentationPainter<T> findPainter(T presentation) {
        Function<Class, AwtLineSeparatorPresentationPainter> call =
            Application.get().getExtensionPoint(AwtLineSeparatorPresentationPainter.class).getOrBuildCache(KEY);
        return call.apply(presentation.getClass());
    }

    Class<T> getPresentationType();

    void paint(T presentation, Editor editor, Graphics2D g, AwtLineSeparatorBounds bounds);
}
