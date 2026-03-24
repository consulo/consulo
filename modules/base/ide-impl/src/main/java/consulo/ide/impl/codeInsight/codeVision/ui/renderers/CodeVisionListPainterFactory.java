// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision.ui.renderers;

import consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters.CodeVisionListPainter;
import consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters.CodeVisionTheme;

/**
 * Factory for creating {@link CodeVisionListPainter} instances.
 * <p>
 * Equivalent to JetBrains {@code CodeVisionListPainterFactory}.
 * In Consulo, the default implementation creates a standard {@link CodeVisionListPainter}.
 */
public interface CodeVisionListPainterFactory {
    static CodeVisionListPainterFactory getDefault() {
        return DefaultCodeVisionListPainterFactory.INSTANCE;
    }

    CodeVisionListPainter createCodeVisionListPainter(CodeVisionTheme theme);

    class DefaultCodeVisionListPainterFactory implements CodeVisionListPainterFactory {
        static final DefaultCodeVisionListPainterFactory INSTANCE = new DefaultCodeVisionListPainterFactory();

        @Override
        public CodeVisionListPainter createCodeVisionListPainter(CodeVisionTheme theme) {
            return new CodeVisionListPainter(theme);
        }
    }
}
