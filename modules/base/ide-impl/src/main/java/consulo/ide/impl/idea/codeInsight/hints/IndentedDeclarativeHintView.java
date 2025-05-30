// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;


import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.Inlay;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.util.DocumentUtil;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.Pair;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class IndentedDeclarativeHintView<View extends DeclarativeHintView<Model>, Model>
    implements DeclarativeHintView<Model> {

    private final View view;
    private final int initialIndentAnchorOffset;
    private Inlay<?> inlay;

    public IndentedDeclarativeHintView(View view, int initialIndentAnchorOffset) {
        this.view = view;
        this.initialIndentAnchorOffset = initialIndentAnchorOffset;
    }

    public View getView() {
        return view;
    }

    public Inlay<?> getInlay() {
        return inlay;
    }

    public void setInlay(Inlay<?> inlay) {
        this.inlay = inlay;
    }

    @RequiredUIAccess
    @Override
    public void updateModel(Model newModel) {
        view.updateModel(newModel);
    }

    private int getViewIndentMargin() {
        return getViewIndentMargin(null);
    }

    private int getViewIndentMargin(Inlay<?> inlayParam) {
        if (this.inlay != null) {
            return calcViewIndentMargin(this.inlay.getOffset(), this.inlay.getEditor());
        }
        else if (inlayParam != null) {
            return calcViewIndentMargin(initialIndentAnchorOffset, inlayParam.getEditor());
        }
        else {
            return 0;
        }
    }

    @RequiredUIAccess
    @Override
    public int calcWidthInPixels(Inlay<?> inlay, InlayTextMetricsStorage fontMetricsStorage) {
        return getViewIndentMargin(inlay) + view.calcWidthInPixels(inlay, fontMetricsStorage);
    }

    @Override
    public void paint(Inlay<?> inlay,
                      Graphics2D g,
                      Rectangle2D targetRegion,
                      TextAttributes textAttributes,
                      InlayTextMetricsStorage fontMetricsStorage) {
        view.paint(inlay, g, toViewRectangle(targetRegion), textAttributes, fontMetricsStorage);
    }

    @Override
    public void handleLeftClick(EditorMouseEvent e,
                                Point pointInsideInlay,
                                InlayTextMetricsStorage fontMetricsStorage,
                                boolean controlDown) {
        Point translated = toPointInsideViewOrNull(pointInsideInlay);
        if (translated == null) return;
        view.handleLeftClick(e, translated, fontMetricsStorage, controlDown);
    }

    @Override
    public LightweightHint handleHover(EditorMouseEvent e,
                                       Point pointInsideInlay,
                                       InlayTextMetricsStorage fontMetricsStorage) {
        Point translated = toPointInsideViewOrNull(pointInsideInlay);
        if (translated == null) return null;
        return view.handleHover(e, translated, fontMetricsStorage);
    }

    @Override
    public void handleRightClick(EditorMouseEvent e,
                                 Point pointInsideInlay,
                                 InlayTextMetricsStorage fontMetricsStorage) {
        Point translated = toPointInsideViewOrNull(pointInsideInlay);
        if (translated == null) return;
        view.handleRightClick(e, translated, fontMetricsStorage);
    }

    @Override
    public InlayMouseArea getMouseArea(Point pointInsideInlay,
                                       InlayTextMetricsStorage fontMetricsStorage) {
        Point translated = toPointInsideViewOrNull(pointInsideInlay);
        if (translated == null) return null;
        return view.getMouseArea(translated, fontMetricsStorage);
    }

    private Point toPointInsideViewOrNull(Point pointInsideInlay) {
        int indentMargin = getViewIndentMargin();
        if (pointInsideInlay.x < indentMargin) {
            return null;
        }
        return new Point(pointInsideInlay.x - indentMargin, pointInsideInlay.y);
    }

    private Rectangle2D toViewRectangle(Rectangle2D rect) {
        int indentMargin = getViewIndentMargin();
        return new Rectangle2D.Double(
            rect.getX() + indentMargin,
            rect.getY(),
            rect.getWidth() - indentMargin,
            rect.getHeight()
        );
    }

    public static Pair<Integer, Integer> calcIndentAnchorOffset(int offset, Document document) {
        int lineStartOffset = DocumentUtil.getLineStartOffset(offset, document);
        int textStartOffset = CharArrayUtil.shiftForward(document.getImmutableCharSequence(), lineStartOffset, " \t");
        return new Pair<>(lineStartOffset, textStartOffset);
    }

    @RequiredReadAction
    private static int calcViewIndentMargin(int offset, Editor editor) {
        Document document = editor.getDocument();
        CharSequence text = document.getImmutableCharSequence();
        Pair<Integer, Integer> pair = calcIndentAnchorOffset(offset, document);
        int lineStartOffset = pair.getFirst();
        int textStartOffset = pair.getSecond();
        if (editor.getInlayModel().isInBatchMode()) {
            return measureIndentSafely(text, lineStartOffset, textStartOffset, editor);
        }
        else {
            int vfmtRightShift = VirtualFormattingInlaysInfo
                .measureVirtualFormattingInlineInlays(editor, textStartOffset, textStartOffset);
            return editor.offsetToXY(textStartOffset, false, false).x + vfmtRightShift;
        }
    }

    private static int measureIndentSafely(CharSequence text, int start, int end, Editor editor) {
        int spaceWidth = EditorUtil.getPlainSpaceWidth(editor);
        int tabSize = EditorUtil.getTabSize(editor);
        int columns = 0;
        int off = start;
        while (off < end) {
            char c = text.charAt(off++);
            if (c == '\t') {
                columns += ((columns / tabSize) + 1) * tabSize;
            }
            else {
                columns++;
            }
        }
        return columns * spaceWidth;
    }
}
