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
package consulo.desktop.awt.codeInsight.parameterInfo;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.util.EditorUtil;
import consulo.document.util.TextRange;
import consulo.language.editor.internal.parameterInfo.ParameterInfoAnchor;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.hint.HintManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
class ParameterInfoHintPosition {
    private final Editor myEditor;
    private int myPreviousOffset = -1;
    private Point myPreviousBestPoint;
    private Short myPreviousBestPosition;

    ParameterInfoHintPosition(Editor editor) {
        myEditor = editor;
    }

    @RequiredUIAccess
    Pair<Point, Short> getBestPointPosition(
        LightweightHint hint,
        @Nullable ParameterInfoAnchor anchor,
        int offset,
        @Nullable VisualPosition pos,
        short preferredPosition
    ) {
        if (anchor != null) {
            TextRange range = anchor.range();
            TextRange rangeWithoutParens = TextRange.from(range.getStartOffset() + 1, Math.max(range.getLength() - 2, 0));
            if (!rangeWithoutParens.contains(offset)) {
                offset = offset < rangeWithoutParens.getStartOffset()
                    ? rangeWithoutParens.getStartOffset()
                    : rangeWithoutParens.getEndOffset();
                pos = null;
            }
        }

        if (myPreviousOffset == offset) {
            return Pair.create(myPreviousBestPoint, myPreviousBestPosition);
        }

        boolean isMultiline = anchor != null && anchor.multiline();
        if (pos == null) {
            pos = EditorUtil.inlayAwareOffsetToVisualPosition(myEditor, offset);
        }

        Pair<Point, Short> position;
        if (!isMultiline) {
            position = chooseBestHintPosition(myEditor, pos, hint, preferredPosition, false);
        }
        else {
            Point p = HintManagerImpl.getInstanceImpl().getHintPosition(hint, myEditor, pos, HintManager.ABOVE);
            position = new Pair<>(p, HintManager.ABOVE);
        }

        myPreviousBestPoint = position.getFirst();
        myPreviousBestPosition = position.getSecond();
        myPreviousOffset = offset;
        return position;
    }

    /**
     * Returned Point is in layered pane coordinate system.
     * Second value is a {@link HintManager.PositionFlags position flag}.
     */
    @RequiredUIAccess
    static Pair<Point, Short> chooseBestHintPosition(
        Editor editor,
        @Nullable VisualPosition pos,
        LightweightHint hint,
        short preferredPosition,
        boolean showLookupHint
    ) {
        if (Application.get().isUnitTestMode() || Application.get().isHeadlessEnvironment()) {
            return Pair.pair(new Point(), HintManager.DEFAULT);
        }

        HintManagerImpl hintManager = HintManagerImpl.getInstanceImpl();
        Dimension hintSize = ((LightweightHintImpl) hint).getComponent().getPreferredSize();
        JComponent editorComponent = editor.getComponent();
        JLayeredPane layeredPane = editorComponent.getRootPane().getLayeredPane();

        Point p1;
        Point p2;
        if (showLookupHint) {
            p1 = hintManager.getHintPosition(hint, editor, HintManager.UNDER);
            p2 = hintManager.getHintPosition(hint, editor, HintManager.ABOVE);
        }
        else {
            p1 = hintManager.getHintPosition(hint, editor, pos, HintManager.UNDER);
            p2 = hintManager.getHintPosition(hint, editor, pos, HintManager.ABOVE);
        }

        boolean p1Ok = p1.y + hintSize.height < layeredPane.getHeight();
        boolean p2Ok = p2.y >= 0;

        if (!showLookupHint) {
            if (preferredPosition != HintManager.DEFAULT) {
                if (preferredPosition == HintManager.ABOVE) {
                    if (p2Ok) {
                        return new Pair<>(p2, HintManager.ABOVE);
                    }
                }
                else if (preferredPosition == HintManager.UNDER) {
                    if (p1Ok) {
                        return new Pair<>(p1, HintManager.UNDER);
                    }
                }
            }
        }
        if (p1Ok) {
            return new Pair<>(p1, HintManager.UNDER);
        }
        if (p2Ok) {
            return new Pair<>(p2, HintManager.ABOVE);
        }

        int underSpace = layeredPane.getHeight() - p1.y;
        int aboveSpace = p2.y;
        return aboveSpace > underSpace ? new Pair<>(new Point(p2.x, 0), HintManager.UNDER) : new Pair<>(p1, HintManager.ABOVE);
    }
}
