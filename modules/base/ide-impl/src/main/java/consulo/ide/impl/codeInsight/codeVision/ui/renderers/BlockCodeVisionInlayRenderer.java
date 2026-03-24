package consulo.ide.impl.codeInsight.codeVision.ui.renderers;

import consulo.codeEditor.Inlay;
import consulo.document.util.DocumentUtil;
import consulo.ide.impl.codeInsight.codeVision.ui.model.CodeVisionListData;
import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.util.lang.CharArrayUtil;

import java.awt.Point;
import java.awt.Rectangle;

public class BlockCodeVisionInlayRenderer extends CodeVisionInlayRendererBase {
    @Override
    public Rectangle calculateCodeVisionEntryBounds(CodeVisionEntry element) {
        Rectangle hoveredEntryBounds = painter.hoveredEntryBounds(
            inlay.getEditor(),
            inlayState(inlay),
            inlay.getUserData(CodeVisionListData.KEY),
            element
        );
        if (hoveredEntryBounds == null) return null;
        hoveredEntryBounds.x += painterPosition(inlay);
        return hoveredEntryBounds;
    }

    @Override
    public int calcWidthInPixels(Inlay<?> inlay) {
        CodeVisionListData userData = inlay.getUserData(CodeVisionListData.KEY);
        if (userData != null && !userData.isPainted()) {
            return 0;
        }
        int painterPosition = painterPosition(inlay);
        return painter.size(inlay.getEditor(), inlayState(inlay), inlay.getUserData(CodeVisionListData.KEY)).width
               + painterPosition;
    }

    @Override
    public int calcHeightInPixels(Inlay<?> inlay) {
        Integer height = painter.inlayHeightInPixels(inlay.getEditor(), inlay);
        return height != null ? height : super.calcHeightInPixels(inlay);
    }

    private int painterPosition(Inlay<?> inlay) {
        if (!inlay.isValid()) return 0;

        int lineStartOffset = DocumentUtil.getLineStartOffset(inlay.getOffset(), inlay.getEditor().getDocument());
        int shiftForward = CharArrayUtil.shiftForward(
            inlay.getEditor().getDocument().getCharsSequence(),
            lineStartOffset,
            " \t"
        );
        // VirtualFormattingInlaysInfo does not exist in Consulo, so vfmtRightShift is skipped
        return inlay.getEditor().offsetToXY(shiftForward, false, false).x;
    }

    @Override
    protected Point getPoint(Inlay<?> inlay, Point targetPoint) {
        int painterPosition = painterPosition(inlay);
        return new Point(targetPoint.x + painterPosition, targetPoint.y);
    }

}
