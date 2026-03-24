package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.colorScheme.TextAttributes;
import consulo.ide.impl.codeInsight.codeVision.ui.model.CodeVisionListData;
import consulo.ide.impl.codeInsight.codeVision.ui.model.RangeCodeVisionModel;
import consulo.language.editor.codeVision.CodeVisionEntry;
import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

public class CodeVisionListPainter implements ICodeVisionEntryBasePainter<CodeVisionListData> {
    private final ICodeVisionGraphicPainter delimiterPainter;
    private final CodeVisionTheme theme;

    private final CodeVisionStringPainter loadingPainter = new CodeVisionStringPainter("Loading...");

    public CodeVisionListPainter(CodeVisionTheme theme) {
        this(new DelimiterPainter(), theme);
    }

    public CodeVisionListPainter(ICodeVisionGraphicPainter delimiterPainter, CodeVisionTheme theme) {
        this.delimiterPainter = delimiterPainter;
        this.theme = theme;
    }

    private Map<CodeVisionEntry, Rectangle> getRelativeBounds(
        Editor editor,
        RangeCodeVisionModel.InlayState state,
        @Nullable CodeVisionListData value
    ) {
        Map<CodeVisionEntry, Rectangle> map = new HashMap<>();
        if (value == null) return map;

        int x = theme.left;
        int y = 0;
        int delimiterWidth = delimiterPainter.size(editor, state).width;

        java.util.List<CodeVisionEntry> visibleLens = value.visibleLens;
        for (int index = 0; index < visibleLens.size(); index++) {
            CodeVisionEntry it = visibleLens.get(index);
            ICodeVisionEntryBasePainter<CodeVisionEntry> painter = CodeVisionPainters.getPainter(it);

            Dimension size = painter.size(editor, state, it);
            map.put(it, new Rectangle(x, y, size.width, size.height));

            x += size.width;

            if (index < visibleLens.size() - 1) {
                x += delimiterWidth;
            }
        }
        return map;
    }

    @Override
    public void paint(
        Editor editor,
        TextAttributes textAttributes,
        Graphics g,
        @Nullable CodeVisionListData value,
        Point point,
        RangeCodeVisionModel.InlayState state,
        boolean hovered,
        @Nullable CodeVisionEntry hoveredEntry
    ) {
        int x = point.x + theme.left;
        Integer textAscent = inlayTextAscent(editor, value != null ? value.inlay : null);
        int ascent = textAscent != null ? textAscent : editor.getAscent();
        int y = point.y + theme.top + ascent;

        if (value == null || value.visibleLens.isEmpty()) {
            loadingPainter.paint(editor, textAttributes, g, new Point(x, y), state, hovered);
            return;
        }

        Map<CodeVisionEntry, Rectangle> relativeBounds = getRelativeBounds(editor, state, value);

        int delimiterWidth = delimiterPainter.size(editor, state).width;
        java.util.List<CodeVisionEntry> visibleLens = value.visibleLens;
        for (int index = 0; index < visibleLens.size(); index++) {
            CodeVisionEntry it = visibleLens.get(index);
            ICodeVisionEntryBasePainter<CodeVisionEntry> painter = CodeVisionPainters.getPainter(it);

            Rectangle size = relativeBounds.get(it);
            if (size == null) continue;

            painter.paint(editor, textAttributes, g, it, new Point(x, y), state, it == hoveredEntry, hoveredEntry);
            x += size.width;

            if (painter.shouldBeDelimited(it) && (index < visibleLens.size() - 1 || hovered)) {
                delimiterPainter.paint(editor, textAttributes, g, new Point(x, y), state, false);
                x += delimiterWidth;
            }
        }
    }

    @Override
    public Dimension size(Editor editor, RangeCodeVisionModel.InlayState state, @Nullable CodeVisionListData value) {
        if (value == null) {
            return loadingSize(editor, state);
        }
        if (value.visibleLens.isEmpty()) {
            return loadingSize(editor, state);
        }

        int delimiterWidth = delimiterPainter.size(editor, state).width;
        // isMoreLensActive() is always false in Consulo (no ProjectCodeVisionModel)
        int moreWidth = value.isMoreLensActive() ? 0 : 0;
        int listSum = value.visibleLens.stream()
            .mapToInt(entry -> CodeVisionPainters.getPainter(entry).size(editor, state, entry).width)
            .sum();
        return new Dimension(
            listSum + (delimiterWidth * (value.visibleLens.size() - 1)) + theme.left + theme.right + moreWidth,
            editor.getLineHeight() + theme.top + theme.bottom
        );
    }

    public @Nullable Integer inlayHeightInPixels(Editor editor, Inlay<?> inlay) {
        return null;
    }

    public @Nullable Integer inlayTextAscent(Editor editor, @Nullable Inlay<?> inlay) {
        return editor.getAscent();
    }

    private Dimension loadingSize(Editor editor, RangeCodeVisionModel.InlayState state) {
        return new Dimension(
            loadingPainter.size(editor, state).width + theme.left + theme.right,
            editor.getLineHeight() + theme.top + theme.bottom
        );
    }

    private boolean isHovered(int x, int y, Rectangle size) {
        return x >= size.x && x <= (size.x + size.width);
    }

    public @Nullable CodeVisionEntry hoveredEntry(
        Editor editor,
        RangeCodeVisionModel.InlayState state,
        @Nullable CodeVisionListData value,
        int x,
        int y
    ) {
        Map<CodeVisionEntry, Rectangle> relativeBounds = getRelativeBounds(editor, state, value);
        for (Map.Entry<CodeVisionEntry, Rectangle> entry : relativeBounds.entrySet()) {
            if (isHovered(x, y, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public @Nullable Rectangle hoveredEntryBounds(
        Editor editor,
        RangeCodeVisionModel.InlayState state,
        @Nullable CodeVisionListData value,
        CodeVisionEntry element
    ) {
        return getRelativeBounds(editor, state, value).get(element);
    }
}
