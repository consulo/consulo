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
import consulo.util.dataholder.Key;
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
    public static final Key<MarkerData> TOOLTIP_KEY = Key.create("LineStatusMarkerRenderer.Tooltip.Id");

    public static class MarkerData {
        private final byte myType;

        public MarkerData(byte type) {
            myType = type;
        }

        public byte getType() {
            return myType;
        }
    }

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
        highlighter.putUserData(TOOLTIP_KEY, new MarkerData(range.getType()));
        return highlighter;
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
