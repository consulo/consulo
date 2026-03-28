package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.codeInsight.codeVision.ui.model.RangeCodeVisionModel;
import consulo.ui.ex.awt.JBUI;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

public class DelimiterPainter implements ICodeVisionGraphicPainter {
    @Override
    public void paint(
        Editor editor,
        TextAttributes textAttributes,
        Graphics g,
        Point point,
        RangeCodeVisionModel.InlayState state,
        boolean hovered
    ) {
        // no-op
    }

    @Override
    public Dimension size(Editor editor, RangeCodeVisionModel.InlayState state) {
        int width = (int) CodeVisionThemeInfoProvider.getInstance().lensFontSize(editor);
        return new Dimension(JBUI.scale(width), editor.getLineHeight());
    }
}
