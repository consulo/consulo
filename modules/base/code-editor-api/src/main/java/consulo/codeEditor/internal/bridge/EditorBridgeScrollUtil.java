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
package consulo.codeEditor.internal.bridge;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollingModel;
import consulo.util.collection.ArrayUtil;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public final class EditorBridgeScrollUtil {
    private EditorBridgeScrollUtil() {
    }

    /**
     * The vertical scroll offsets which bring the ranges into sight, level with each other where the editors allow it.
     *
     * @param maximumOffset the largest vertical scroll offset of an editor
     */
    public static int[] getTargetOffsets(
        List<? extends Editor> editors,
        int[] startLines,
        int[] endLines,
        int preferredTopShift,
        ToIntFunction<Editor> maximumOffset
    ) {
        int count = editors.size();
        assert startLines.length == count;
        assert endLines.length == count;

        int[] topOffsets = new int[count];
        int[] bottomOffsets = new int[count];
        int[] rangeHeights = new int[count];
        int[] gapLines = new int[count];
        int[] editorHeights = new int[count];
        int[] maximumOffsets = new int[count];
        int[] topShifts = new int[count];

        for (int i = 0; i < count; i++) {
            Editor editor = editors.get(i);
            topOffsets[i] = editor.logicalPositionToXY(new LogicalPosition(startLines[i], 0)).y;
            bottomOffsets[i] = editor.logicalPositionToXY(new LogicalPosition(endLines[i] + 1, 0)).y;
            rangeHeights[i] = bottomOffsets[i] - topOffsets[i];

            gapLines[i] = 2 * editor.getLineHeight();
            editorHeights[i] = editor.getScrollingModel().getVisibleArea().height;

            maximumOffsets[i] = maximumOffset.applyAsInt(editor);

            // 'shift' here - distance between editor's top and first line of range

            // make whole range visible. If possible, locate it at 'center' (1/3 of height) (or at 'preferredTopShift' if it was specified)
            // If can't show whole range - show as much as we can
            boolean canShow = 2 * gapLines[i] + rangeHeights[i] <= editorHeights[i];

            int shift = preferredTopShift != -1 ? preferredTopShift : editorHeights[i] / 3;
            topShifts[i] = canShow ? Math.min(editorHeights[i] - gapLines[i] - rangeHeights[i], shift) : gapLines[i];
        }

        int topShift = ArrayUtil.min(topShifts);

        // check if we're at the top of file
        topShift = Math.min(topShift, ArrayUtil.min(topOffsets));

        int[] offsets = new int[count];
        boolean haveEnoughSpace = true;
        for (int i = 0; i < count; i++) {
            offsets[i] = topOffsets[i] - topShift;
            haveEnoughSpace &= maximumOffsets[i] > offsets[i];
        }

        if (haveEnoughSpace) {
            return offsets;
        }

        // One of the ranges is at end of file - we can't scroll where we want to.
        topShift = 0;
        for (int i = 0; i < count; i++) {
            topShift = Math.max(topOffsets[i] - maximumOffsets[i], topShift);
        }

        for (int i = 0; i < count; i++) {
            // Try to show as much of range as we can (even if it breaks alignment)
            offsets[i] = topOffsets[i] - topShift + Math.max(topShift + rangeHeights[i] + gapLines[i] - editorHeights[i], 0);

            // always show top of the range
            offsets[i] = Math.min(offsets[i], topOffsets[i] - gapLines[i]);
        }

        return offsets;
    }

    /**
     * The largest vertical scroll offset, taken from the height of the document's lines.
     */
    public static int getDocumentMaximumOffset(Editor editor) {
        int lastLine = Math.max(editor.getDocument().getLineCount(), 1);
        int contentHeight = editor.logicalPositionToXY(new LogicalPosition(lastLine, 0)).y;
        return Math.max(contentHeight - editor.getScrollingModel().getVisibleArea().height, 0);
    }

    public static void scrollVertically(Editor editor, int offset, boolean animated) {
        ScrollingModel model = editor.getScrollingModel();
        if (!animated) {
            model.disableAnimation();
        }
        model.scrollVertically(offset);
        if (!animated) {
            model.enableAnimation();
        }
    }

    public static void scrollHorizontally(Editor editor, int offset, boolean animated) {
        ScrollingModel model = editor.getScrollingModel();
        if (!animated) {
            model.disableAnimation();
        }
        model.scrollHorizontally(offset);
        if (!animated) {
            model.enableAnimation();
        }
    }
}
