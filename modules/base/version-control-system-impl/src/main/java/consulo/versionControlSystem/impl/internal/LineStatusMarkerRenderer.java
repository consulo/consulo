/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.versionControlSystem.impl.internal;

import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.versionControlSystem.internal.VcsRange;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;

/**
 * Per-range popup renderer and highlighter factory for VCS line-status markers.
 *
 * <p>Gutter painting is delegated to {@link LineStatusGutterMarkerRenderer} (document-level)
 * and all drawing primitives are in {@link LineStatusMarkerDrawUtil}.
 */
public abstract class LineStatusMarkerRenderer implements ActiveGutterRenderer {

    protected final VcsRange myRange;

    public LineStatusMarkerRenderer(VcsRange range) {
        myRange = range;
    }

    @Override
    public LocalizeValue getTooltipValue() {
        return LocalizeValue.empty();
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a range highlighter for the error stripe (scrollbar coloured dots) only.
     * No gutter painting is attached here – that is handled by {@link LineStatusGutterMarkerRenderer}.
     */
    public static RangeHighlighter createRangeHighlighter(
        VcsRange range,
        TextRange textRange,
        MarkupModel markupModel
    ) {
        TextAttributes attributes = getTextAttributes(range);
        RangeHighlighter highlighter = markupModel.addRangeHighlighter(
            textRange.getStartOffset(),
            textRange.getEndOffset(),
            HighlighterLayer.FIRST - 1,
            attributes,
            HighlighterTargetArea.LINES_IN_RANGE
        );
        highlighter.setThinErrorStripeMark(true);
        highlighter.setGreedyToLeft(true);
        highlighter.setGreedyToRight(true);
        highlighter.setErrorStripeTooltip(getTooltipText(range));
        return highlighter;
    }

    /** Creates a simple colored-bar renderer for a line range (used by external callers). */
    public static LineMarkerRenderer createRenderer(
        int line1,
        int line2,
        ColorValue color,
        LocalizeValue tooltip,
        @Nullable BiConsumer<Editor, MouseEvent> action
    ) {
        return new ActiveGutterRenderer() {
            @Override
            public void paint(Editor editor, Graphics g, Rectangle r) {
                Rectangle area = LineStatusMarkerDrawUtil.getMarkerArea(editor, r, line1, line2);
                ColorValue borderColor = LineStatusMarkerDrawUtil.getGutterBorderColor(editor);
                if (area.height != 0) {
                    LineStatusMarkerDrawUtil.paintRect(
                        (Graphics2D) g, color, borderColor,
                        area.x, area.y, area.x + area.width, area.y + area.height
                    );
                }
                else {
                    LineStatusMarkerDrawUtil.paintTriangle(
                        (Graphics2D) g, editor, color, borderColor,
                        area.x, area.x + area.width, area.y
                    );
                }
            }

            @Override
            public LocalizeValue getTooltipValue() {
                return tooltip;
            }

            @Override
            public boolean canDoAction(MouseEvent e) {
                return LineStatusMarkerDrawUtil.isInsideMarkerArea(e);
            }

            @Override
            public void doAction(Editor editor, MouseEvent e) {
                if (action != null) {
                    action.accept(editor, e);
                }
            }
        };
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static TextAttributes getTextAttributes(final VcsRange range) {
        return new TextAttributes() {
            @Override
            public ColorValue getErrorStripeColor() {
                return LineStatusMarkerDrawUtil.getErrorStripeColor(range, null);
            }
        };
    }

    private static LocalizeValue getTooltipText(VcsRange range) {
        if (range.getLine1() == range.getLine2()) {
            return range.getVcsLine1() + 1 == range.getVcsLine2()
                ? VcsLocalize.tooltipTextLineBeforeDeleted(range.getLine1() + 1)
                : VcsLocalize.tooltipTextLinesBeforeDeleted(range.getLine1() + 1, range.getVcsLine2() - range.getVcsLine1());
        }
        else if (range.getLine1() + 1 == range.getLine2()) {
            return VcsLocalize.tooltipTextLineChanged(range.getLine1() + 1);
        }
        else {
            return VcsLocalize.tooltipTextLinesChanged(range.getLine1() + 1, range.getLine2());
        }
    }

    // -------------------------------------------------------------------------
    // Gutter painting — delegates to LineStatusMarkerDrawUtil
    // -------------------------------------------------------------------------

    @Override
    public void paint(Editor editor, Graphics g, Rectangle r) {
        LineStatusMarkerDrawUtil.paintRange(myRange, editor, g, r);
    }

    // -------------------------------------------------------------------------
    // Popup — no-op on base class; overridden by createRenderer(VcsRange, popup)
    // -------------------------------------------------------------------------

    @Override
    public boolean canDoAction(MouseEvent e) {
        return false;
    }

    @Override
    public void doAction(Editor editor, MouseEvent e) {
    }
}
