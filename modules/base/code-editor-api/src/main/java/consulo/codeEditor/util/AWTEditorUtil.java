/*
 * Copyright 2013-2024 consulo.io
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
package consulo.codeEditor.util;

import consulo.application.util.registry.Registry;
import consulo.codeEditor.*;
import consulo.codeEditor.internal.CodeEditorAssertion;
import consulo.platform.Platform;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-12-06
 */
public final class AWTEditorUtil {
    private AWTEditorUtil() {
    }

    public static boolean isPasswordEditor(@Nullable Editor editor) {
        return editor != null && editor.getContentComponent() instanceof JPasswordField;
    }

    public static int columnsNumber(float width, float plainSpaceSize) {
        return (int) Math.ceil(width / plainSpaceSize);
    }

    public static float nextTabStop(float x, float plainSpaceWidth, int tabSize) {
        if (tabSize <= 0) {
            return x + plainSpaceWidth;
        }
        tabSize *= plainSpaceWidth;

        int nTabs = (int) (x / tabSize);
        return (nTabs + 1) * tabSize;
    }

    public static int yPositionToLogicalLine(Editor editor, MouseEvent event) {
        return EditorUtil.yPositionToLogicalLine(editor, event.getY());
    }

    public static int yPositionToLogicalLine(Editor editor, Point point) {
        return EditorUtil.yPositionToLogicalLine(editor, point.y);
    }

    public static boolean isPrimaryCaretVisible(Editor editor) {
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        Caret caret = editor.getCaretModel().getPrimaryCaret();
        Point caretPoint = editor.visualPositionToXY(caret.getVisualPosition());
        return visibleArea.contains(caretPoint);
    }

    /**
     * Virtual space (after line end, and after end of text), inlays and space between visual lines (where block inlays are located) is
     * excluded
     */
    public static boolean isPointOverText(Editor editor, Point point) {
        return CodeEditorAssertion.compute(() -> {
            VisualPosition visualPosition = editor.xyToVisualPosition(point);
            int visualLineStartY = editor.visualLineToY(visualPosition.line);
            if (point.y < visualLineStartY || point.y >= visualLineStartY + editor.getLineHeight()) {
                return false; // block inlay space
            }
            if (editor.getSoftWrapModel().isInsideOrBeforeSoftWrap(visualPosition)) {
                return false; // soft wrap
            }
            LogicalPosition logicalPosition = editor.visualToLogicalPosition(visualPosition);
            int offset = editor.logicalPositionToOffset(logicalPosition);
            if (editor.getFoldingModel().getCollapsedRegionAtOffset(offset) instanceof CustomFoldRegion) {
                return false;
            }
            if (!logicalPosition.equals(editor.offsetToLogicalPosition(offset))) {
                return false; // virtual space
            }
            List<Inlay<?>> inlays = editor.getInlayModel().getInlineElementsInRange(offset, offset);
            if (!inlays.isEmpty()) {
                VisualPosition inlaysStart = editor.offsetToVisualPosition(offset);
                if (inlaysStart.line == visualPosition.line) {
                    int relX = point.x - editor.visualPositionToXY(inlaysStart).x;
                    if (relX >= 0 && relX < inlays.stream().mapToInt(Inlay::getWidthInPixels).sum()) {
                        return false; // inline inlay
                    }
                }
            }
            return true;
        });
    }

    public static boolean isChangeFontSize(MouseWheelEvent e) {
        if (e.getWheelRotation() == 0) {
            return false;
        }
        return Platform.current().os().isMac()
            ? !e.isControlDown() && e.isMetaDown() && !e.isAltDown() && !e.isShiftDown()
            : e.isControlDown() && !e.isMetaDown() && !e.isAltDown() && !e.isShiftDown();
    }

    public static Image scaleIconAccordingEditorFont(Image icon, Editor editor) {
        if (Registry.is("editor.scale.gutter.icons") && editor instanceof RealEditor realEditor) {
            float scale = realEditor.getScale();
            if (Math.abs(1f - scale) > 0.1f) {
                return ImageEffects.resize(icon, scale);
            }
        }
        return icon;
    }
}
