package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.codeInsight.codeVision.ui.model.RangeCodeVisionModel;
import consulo.language.editor.codeVision.CodeVisionEntry;
import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

public interface ICodeVisionEntryBasePainter<T> extends ICodeVisionPainter {
    void paint(
        Editor editor,
        TextAttributes textAttributes,
        Graphics g,
        T value,
        Point point,
        RangeCodeVisionModel.InlayState state,
        boolean hovered,
        @Nullable CodeVisionEntry hoveredEntry
    );

    Dimension size(
        Editor editor,
        RangeCodeVisionModel.InlayState state,
        T value
    );

    default boolean shouldBeDelimited(T entry) {
        return true;
    }
}
