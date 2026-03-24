// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision.ui.renderers;

import consulo.codeEditor.Inlay;
import consulo.ide.impl.codeInsight.codeVision.ui.model.CodeVisionListData;
import consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters.CodeVisionTheme;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.ui.ex.awt.JBUI;

import java.awt.Rectangle;

/**
 * Renderer for Right-anchored (after-line-end) code vision inlays.
 * <p>
 * Port of JB's {@code InlineCodeVisionInlayRenderer.kt}:
 * uses 5 px left/right horizontal padding so that the hints don't run up to the text edge.
 */
public class InlineCodeVisionInlayRenderer extends CodeVisionInlayRendererBase {

    public InlineCodeVisionInlayRenderer() {
        super(new CodeVisionTheme(JBUI.scale(2), JBUI.scale(5), JBUI.scale(5), 0, 0));
    }

    @Override
    public Rectangle calculateCodeVisionEntryBounds(CodeVisionEntry element) {
        return painter.hoveredEntryBounds(
            inlay.getEditor(),
            inlayState(inlay),
            inlay.getUserData(CodeVisionListData.KEY),
            element
        );
    }

    @Override
    public int calcWidthInPixels(Inlay<?> inlay) {
        CodeVisionListData userData = inlay.getUserData(CodeVisionListData.KEY);
        return painter.size(inlay.getEditor(), inlayState(inlay), userData).width;
    }
}
