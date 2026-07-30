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
import consulo.ui.color.ColorValue;
import consulo.versionControlSystem.internal.VcsChangePresentation;
import consulo.versionControlSystem.internal.VcsRange;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns {@link VcsRange}s into semantic {@link VcsChangePresentation}s: which presentations exist, in what order,
 * in what colour, and which one is hovered.
 *
 * @author VISTALL
 * @since 2026-07-30
 */
public final class VcsLineMarkerBuilder {
    private VcsLineMarkerBuilder() {
    }

    /**
     * Builds presentations for every range overlapping the context's visible line window.
     */
    public static List<VcsChangePresentation> buildPresentations(List<? extends VcsRange> ranges, LineMarkerPresentationContext context) {
        List<VcsChangePresentation> result = new ArrayList<>();
        for (VcsRange range : ranges) {
            if (isVisible(range, context)) {
                appendRange(range, context, result);
            }
        }
        return result;
    }

    /**
     * A deleted range renders as a marker sitting on the boundary line, so it stays relevant one
     * line above the visible window.
     */
    private static boolean isVisible(VcsRange range, LineMarkerPresentationContext context) {
        return range.getLine2() >= context.firstVisibleLine() - 1 && range.getLine1() <= context.lastVisibleLine();
    }

    private static void appendRange(VcsRange range, LineMarkerPresentationContext context, List<VcsChangePresentation> result) {
        ColorValue borderColor = context.getColor(EditorColors.BORDER_LINES_COLOR);
        boolean hovered = isHovered(range, context.hoveredLine());
        int line1 = range.getLine1();
        int line2 = range.getLine2();

        List<VcsRange.InnerRange> innerRanges = range.getInnerRanges();

        if (innerRanges == null || line1 == line2) {
            // DEFAULT mode, or a deletion that has no interior to describe
            VcsChangePresentation.Kind kind = line1 == line2 ? VcsChangePresentation.Kind.DELETION : VcsChangePresentation.Kind.CHANGE;
            result.add(new VcsChangePresentation(
                line1, line2, kind, range.getType(),
                getGutterColor(range.getType(), context), borderColor, hovered, range
            ));
            return;
        }

        // SMART mode. Order matters and is preserved from paintRange: interior fills first, then
        // the outline over the whole range, then deletions on top.
        for (VcsRange.InnerRange inner : innerRanges) {
            if (inner.getType() == VcsRange.DELETED) {
                continue;
            }
            result.add(new VcsChangePresentation(
                line1 + inner.getLine1(), line1 + inner.getLine2(),
                VcsChangePresentation.Kind.CHANGE, inner.getType(),
                getGutterColor(inner.getType(), context), null, hovered, range
            ));
        }

        result.add(new VcsChangePresentation(
            line1, line2, VcsChangePresentation.Kind.OUTLINE, range.getType(),
            null, borderColor, hovered, range
        ));

        for (VcsRange.InnerRange inner : innerRanges) {
            if (inner.getType() != VcsRange.DELETED) {
                continue;
            }
            int deletionLine = line1 + inner.getLine1();
            result.add(new VcsChangePresentation(
                deletionLine, deletionLine,
                VcsChangePresentation.Kind.DELETION, inner.getType(),
                getGutterColor(inner.getType(), context), borderColor, hovered, range
            ));
        }
    }

    /**
     * Mirrors the former {@code isRangeHovered}: a deleted range draws as a small marker on a line
     * boundary, so its hover zone is widened by one line to stay clickable.
     */
    static boolean isHovered(VcsRange range, int hoveredLine) {
        if (hoveredLine < 0) {
            return false;
        }

        int line1 = range.getLine1();
        int line2 = range.getLine2();
        if (line1 == line2) {
            return hoveredLine >= line1 - 1 && hoveredLine <= line1;
        }
        return hoveredLine >= line1 && hoveredLine < line2;
    }

    private static @Nullable ColorValue getGutterColor(byte type, LineMarkerPresentationContext context) {
        return switch (type) {
            case VcsRange.INSERTED -> context.getColor(EditorColors.ADDED_LINES_COLOR);
            case VcsRange.DELETED -> context.getColor(EditorColors.DELETED_LINES_COLOR);
            case VcsRange.MODIFIED -> context.getColor(EditorColors.MODIFIED_LINES_COLOR);
            case VcsRange.EQUAL -> context.getColor(EditorColors.WHITESPACES_MODIFIED_LINES_COLOR);
            default -> throw new AssertionError("Unknown range type: " + type);
        };
    }
}
