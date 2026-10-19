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
package consulo.desktop.qt.editor.impl.internal.gutter;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import io.qt.core.QRect;
import io.qt.gui.QPainter;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * Paints one kind of {@link LineMarkerPresentation} in the qt gutter - the counterpart of
 * {@code AwtLineMarkerPresentationPainter} and {@code WebLineMarkerPresentationPainter}. A presentation nothing paints
 * resolves to null and is skipped rather than failing.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface QtLineMarkerPresentationPainter<T extends LineMarkerPresentation> {
    ExtensionPointCacheKey<QtLineMarkerPresentationPainter, Function<Class, QtLineMarkerPresentationPainter>> KEY =
        ExtensionPointCacheKey.create(
            "QtLineMarkerPresentationPainter",
            ByClassGrouper.build(QtLineMarkerPresentationPainter::getPresentationType)
        );

    @SuppressWarnings("unchecked")
    static <T extends LineMarkerPresentation> @Nullable QtLineMarkerPresentationPainter<T> findPainter(T presentation) {
        Function<Class, QtLineMarkerPresentationPainter> call =
            Application.get().getExtensionPoint(QtLineMarkerPresentationPainter.class).getOrBuildCache(KEY);
        return call.apply(presentation.getClass());
    }

    /**
     * Presentation class this painter handles. Subclasses are handled too, unless they register their own.
     */
    Class<T> getPresentationType();

    /**
     * @param bounds the part of the gutter the presentation covers - its area across, its lines down
     */
    void paint(T presentation, Editor editor, QPainter painter, QRect bounds);
}
