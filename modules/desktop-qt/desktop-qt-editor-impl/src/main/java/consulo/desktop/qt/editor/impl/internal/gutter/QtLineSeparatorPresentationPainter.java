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
import consulo.codeEditor.markup.LineSeparatorPresentation;
import consulo.component.extension.ByClassGrouper;
import consulo.component.extension.ExtensionPointCacheKey;
import io.qt.gui.QPainter;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface QtLineSeparatorPresentationPainter<T extends LineSeparatorPresentation> {
    ExtensionPointCacheKey<QtLineSeparatorPresentationPainter, Function<Class, QtLineSeparatorPresentationPainter>> KEY =
        ExtensionPointCacheKey.create(
            "QtLineSeparatorPresentationPainter",
            ByClassGrouper.build(QtLineSeparatorPresentationPainter::getPresentationType)
        );

    @SuppressWarnings("unchecked")
    static <T extends LineSeparatorPresentation> @Nullable QtLineSeparatorPresentationPainter<T> findPainter(T presentation) {
        Function<Class, QtLineSeparatorPresentationPainter> call =
            Application.get().getExtensionPoint(QtLineSeparatorPresentationPainter.class).getOrBuildCache(KEY);
        return call.apply(presentation.getClass());
    }

    Class<T> getPresentationType();

    void paint(T presentation, Editor editor, QPainter painter, QtLineSeparatorBounds bounds);
}
