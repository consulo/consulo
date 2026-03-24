package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.codeInsight.codeVision.ui.model.RangeCodeVisionModel;
import consulo.language.editor.codeVision.CodeVisionEntry;
import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

public class CodeVisionStringPainter implements ICodeVisionGraphicPainter {
    private final String text;
    private final CodeVisionVisionTextPainter<String> painter;

    public CodeVisionStringPainter(String text) {
        this(text, null);
    }

    public CodeVisionStringPainter(String text, @Nullable CodeVisionTheme theme) {
        this.text = text;
        this.painter = new CodeVisionVisionTextPainter<>(s -> s, theme);
    }

    @Override
    public void paint(
        Editor editor,
        TextAttributes textAttributes,
        Graphics g,
        Point point,
        RangeCodeVisionModel.InlayState state,
        boolean hovered
    ) {
        painter.paint(editor, textAttributes, g, text, point, state, hovered, null);
    }

    @Override
    public Dimension size(Editor editor, RangeCodeVisionModel.InlayState state) {
        return painter.size(editor, state, text);
    }
}
