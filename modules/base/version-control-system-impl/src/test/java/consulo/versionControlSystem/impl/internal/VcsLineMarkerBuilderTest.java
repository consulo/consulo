/*
 * Copyright 2013-2026 consulo.io
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

import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.LineMarkerPresentationContext;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.ui.color.ColorValue;
import consulo.ui.color.RGBColor;
import consulo.versionControlSystem.internal.VcsChangePresentation;
import consulo.versionControlSystem.internal.VcsRange;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The point of the declarative split: gutter mark generation is asserted directly, with no editor,
 * no gutter, no {@code Graphics} and no pixels. None of this was reachable while the logic lived
 * inside {@code LineStatusMarkerDrawUtil.paintRange}.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public class VcsLineMarkerBuilderTest {
    private static final ColorValue ADDED = new RGBColor(0, 100, 0);
    private static final ColorValue DELETED = new RGBColor(100, 0, 0);
    private static final ColorValue MODIFIED = new RGBColor(0, 0, 100);
    private static final ColorValue WHITESPACE = new RGBColor(50, 50, 50);
    private static final ColorValue BORDER = new RGBColor(10, 10, 10);
    private static final ColorValue BACKGROUND = new RGBColor(255, 255, 255);

    @Test
    public void insertedRangeProducesSingleChangeMark() {
        VcsRange range = new VcsRange(3, 6, 3, 3);

        List<VcsChangePresentation> marks = VcsLineMarkerBuilder.buildPresentations(List.of(range), context());

        assertEquals(1, marks.size());
        VcsChangePresentation mark = marks.get(0);
        assertEquals(VcsChangePresentation.Kind.CHANGE, mark.kind());
        assertEquals(VcsRange.INSERTED, mark.changeType());
        assertEquals(3, mark.startLine());
        assertEquals(6, mark.endLine());
        assertEquals(ADDED, mark.color());
        assertSame(range, mark.payload());
    }

    @Test
    public void modifiedRangeUsesModifiedColor() {
        List<VcsChangePresentation> marks = VcsLineMarkerBuilder.buildPresentations(List.of(new VcsRange(1, 4, 1, 5)), context());

        assertEquals(1, marks.size());
        assertEquals(VcsRange.MODIFIED, marks.get(0).changeType());
        assertEquals(MODIFIED, marks.get(0).color());
    }

    /**
     * A deleted range has no lines of its own, so it becomes a boundary mark rather than a span.
     */
    @Test
    public void deletedRangeProducesBoundaryMark() {
        List<VcsChangePresentation> marks = VcsLineMarkerBuilder.buildPresentations(List.of(new VcsRange(7, 7, 7, 9)), context());

        assertEquals(1, marks.size());
        VcsChangePresentation mark = marks.get(0);
        assertEquals(VcsChangePresentation.Kind.DELETION, mark.kind());
        assertEquals(mark.startLine(), mark.endLine());
        assertEquals(7, mark.startLine());
        assertEquals(DELETED, mark.color());
    }

    /**
     * The interesting case. Inner ranges are relative to the outer range's start, deletions are
     * held back from the fill pass, and the outline sits between the two passes — order is part of
     * the contract, since later marks paint over earlier ones.
     */
    @Test
    public void smartModeEmitsFillsThenOutlineThenDeletions() {
        VcsRange range = new VcsRange(10, 14, 10, 15, List.of(
            new VcsRange.InnerRange(0, 2, VcsRange.MODIFIED),
            new VcsRange.InnerRange(2, 2, VcsRange.DELETED),
            new VcsRange.InnerRange(2, 4, VcsRange.EQUAL)
        ));

        List<VcsChangePresentation> marks = VcsLineMarkerBuilder.buildPresentations(List.of(range), context());

        assertEquals(4, marks.size());

        // fills first, in declaration order, with inner lines rebased onto the outer start
        assertEquals(VcsChangePresentation.Kind.CHANGE, marks.get(0).kind());
        assertEquals(10, marks.get(0).startLine());
        assertEquals(12, marks.get(0).endLine());
        assertEquals(MODIFIED, marks.get(0).color());

        assertEquals(VcsChangePresentation.Kind.CHANGE, marks.get(1).kind());
        assertEquals(12, marks.get(1).startLine());
        assertEquals(14, marks.get(1).endLine());
        assertEquals(WHITESPACE, marks.get(1).color());

        // then the outline spanning the whole outer range, border only
        assertEquals(VcsChangePresentation.Kind.OUTLINE, marks.get(2).kind());
        assertEquals(10, marks.get(2).startLine());
        assertEquals(14, marks.get(2).endLine());
        assertNull(marks.get(2).color());
        assertEquals(BORDER, marks.get(2).borderColor());

        // deletions last, so they sit on top
        assertEquals(VcsChangePresentation.Kind.DELETION, marks.get(3).kind());
        assertEquals(12, marks.get(3).startLine());
        assertEquals(12, marks.get(3).endLine());
    }

    @Test
    public void onlyHoveredRangeIsMarkedHovered() {
        List<VcsRange> ranges = List.of(new VcsRange(0, 3, 0, 4), new VcsRange(10, 12, 10, 13));

        List<VcsChangePresentation> marks = VcsLineMarkerBuilder.buildPresentations(ranges, context(11));

        assertFalse(marks.get(0).hovered());
        assertTrue(marks.get(1).hovered());
    }

    /**
     * Hovering the end line of a half-open range must not count — line2 is exclusive.
     */
    @Test
    public void hoverIsHalfOpen() {
        VcsRange range = new VcsRange(4, 6, 4, 7);

        assertTrue(VcsLineMarkerBuilder.isHovered(range, 4));
        assertTrue(VcsLineMarkerBuilder.isHovered(range, 5));
        assertFalse(VcsLineMarkerBuilder.isHovered(range, 6));
    }

    /**
     * A deletion marker straddles a line boundary, so its hover zone reaches one line above —
     * otherwise the marker is nearly unclickable.
     */
    @Test
    public void deletedRangeHoverZoneCoversLineAbove() {
        VcsRange range = new VcsRange(8, 8, 8, 10);

        assertTrue(VcsLineMarkerBuilder.isHovered(range, 7));
        assertTrue(VcsLineMarkerBuilder.isHovered(range, 8));
        assertFalse(VcsLineMarkerBuilder.isHovered(range, 9));
        assertFalse(VcsLineMarkerBuilder.isHovered(range, -1));
    }

    /**
     * Today's renderer walks every range in the document on every repaint; the visible window makes
     * clipping possible. Deletions stay eligible one line above the window because of where they
     * draw.
     */
    @Test
    public void rangesOutsideVisibleWindowAreSkipped() {
        List<VcsRange> ranges = List.of(
            new VcsRange(0, 2, 0, 3),
            new VcsRange(50, 52, 50, 53),
            new VcsRange(200, 202, 200, 203)
        );

        List<VcsChangePresentation> marks = VcsLineMarkerBuilder.buildPresentations(ranges, context(-1, 40, 100));

        assertEquals(1, marks.size());
        assertEquals(50, marks.get(0).startLine());
    }

    // -------------------------------------------------------------------------

    private static LineMarkerPresentationContext context() {
        return context(-1);
    }

    private static LineMarkerPresentationContext context(int hoveredLine) {
        return context(hoveredLine, 0, Integer.MAX_VALUE - 1);
    }

    private static LineMarkerPresentationContext context(int hoveredLine, int firstVisibleLine, int lastVisibleLine) {
        return new TestContext(hoveredLine, firstVisibleLine, lastVisibleLine);
    }

    /**
     * The entire fake needed to test gutter mark generation.
     */
    private record TestContext(int hoveredLine, int firstVisibleLine, int lastVisibleLine) implements LineMarkerPresentationContext {
        @Override
        public int startLine() {
            return 0;
        }

        @Override
        public int endLine() {
            return lastVisibleLine;
        }

        @Override
        public int lineCount() {
            return lastVisibleLine;
        }

        @Override
        public boolean isLineNumbersShown() {
            return true;
        }

        @Override
        public boolean isAnnotationsShown() {
            return false;
        }

        @Override
        public boolean isMirrored() {
            return false;
        }

        @Override
        public @Nullable ColorValue getColor(EditorColorKey key) {
            if (EditorColors.ADDED_LINES_COLOR.equals(key)) {
                return ADDED;
            }
            if (EditorColors.DELETED_LINES_COLOR.equals(key)) {
                return DELETED;
            }
            if (EditorColors.MODIFIED_LINES_COLOR.equals(key)) {
                return MODIFIED;
            }
            if (EditorColors.WHITESPACES_MODIFIED_LINES_COLOR.equals(key)) {
                return WHITESPACE;
            }
            if (EditorColors.BORDER_LINES_COLOR.equals(key)) {
                return BORDER;
            }
            return null;
        }

        @Override
        public ColorValue getEditorBackgroundColor() {
            return BACKGROUND;
        }

        @Override
        public TextAttributes getAttributes(TextAttributesKey key) {
            return new TextAttributes();
        }
    }
}
