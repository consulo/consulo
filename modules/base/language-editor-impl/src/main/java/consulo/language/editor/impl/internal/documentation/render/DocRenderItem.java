// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.codeEditor.CustomFoldRegion;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.codeEditor.markup.RangeHighlighter;
import org.jspecify.annotations.Nullable;

public interface DocRenderItem {
    Editor getEditor();

    RangeHighlighter getHighlighter();

    @Nullable
    CustomFoldRegion getFoldRegion();

    @Nullable
    String getTextToRender();

    @Nullable
    GutterIconRenderer calcFoldingGutterIconRenderer();

    void setIconVisible(boolean visible);

    /**
     * Switches between rendered and raw representation of the documentation comment.
     */
    void toggle();
}
