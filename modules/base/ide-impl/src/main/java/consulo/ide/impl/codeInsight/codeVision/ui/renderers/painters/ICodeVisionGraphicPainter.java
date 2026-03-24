package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.codeInsight.codeVision.ui.model.RangeCodeVisionModel;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

public interface ICodeVisionGraphicPainter extends ICodeVisionPainter {
    void paint(
        Editor editor,
        TextAttributes textAttributes,
        Graphics g,
        Point point,
        RangeCodeVisionModel.InlayState state,
        boolean hovered
    );

    Dimension size(Editor editor, RangeCodeVisionModel.InlayState state);
}
