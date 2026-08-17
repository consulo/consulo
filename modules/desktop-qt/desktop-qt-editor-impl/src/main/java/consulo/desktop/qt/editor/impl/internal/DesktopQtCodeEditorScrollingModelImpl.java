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
package consulo.desktop.qt.editor.impl.internal;

import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.impl.CodeEditorScrollingModelBase;
import io.qt.widgets.QScrollBar;
import org.jspecify.annotations.Nullable;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Moves the viewport of the surface, which is what keeps the caret on screen.
 * <p>
 * Nothing here is called by the qt widgets - the platform drives it. {@code CodeEditorCaretBase.moveToOffset}
 * asks for {@link ScrollType#RELATIVE} after every move, navigation asks for {@link ScrollType#CENTER}, and the
 * scroll offsets this reports are what the platform positions popups and hints against. While the model was
 * stubbed out the caret could walk off the bottom of the viewport with nothing following it.
 * <p>
 * The scroll offset lives in the scroll bars of the surface rather than in a field here, so the wheel, the bars
 * and this model can never disagree about where the viewport is.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtCodeEditorScrollingModelImpl extends CodeEditorScrollingModelBase {
    /**
     * How much of the document is kept beyond the caret when the viewport only just has to move. Scrolling the
     * caret exactly to the edge leaves nothing of what comes next in sight.
     */
    private static final int VERTICAL_MARGIN_LINES = 1;

    private final DesktopQtEditorImpl myQtEditor;

    public DesktopQtCodeEditorScrollingModelImpl(DesktopQtEditorImpl editor) {
        super(editor);
        myQtEditor = editor;
    }

    private @Nullable DesktopQtEditorWidget getSurface() {
        return myQtEditor.getSurface();
    }

    @Override
    public void accumulateViewportChanges() {
    }

    @Override
    public void flushViewportChanges() {
    }

    @Override
    public Rectangle getVisibleArea() {
        DesktopQtEditorWidget surface = getSurface();
        if (surface == null) {
            return new Rectangle(0, 0);
        }

        return new Rectangle(
            surface.horizontalScrollBar().value(),
            surface.verticalScrollBar().value(),
            surface.viewport().width(),
            surface.viewport().height()
        );
    }

    /**
     * The scrolling is not animated, so where it will end up is where it already is.
     */
    @Override
    public Rectangle getVisibleAreaOnScrollingFinished() {
        return getVisibleArea();
    }

    @Override
    public void scrollToCaret(ScrollType scrollType) {
        scrollTo(myQtEditor.getCaretModel().getLogicalPosition(), scrollType);
    }

    @Override
    public void scrollTo(LogicalPosition pos, ScrollType scrollType) {
        DesktopQtEditorWidget surface = getSurface();
        if (surface == null) {
            return;
        }

        Point point = myQtEditor.visualPositionToXY(myQtEditor.logicalToVisualPosition(pos));
        int lineHeight = myQtEditor.getLineHeight();

        scrollVertically(verticalOffsetFor(surface, point.y, lineHeight, scrollType));
        scrollHorizontally(horizontalOffsetFor(surface, point.x));
    }

    private int verticalOffsetFor(DesktopQtEditorWidget surface, int y, int lineHeight, ScrollType scrollType) {
        int current = surface.verticalScrollBar().value();
        int height = surface.viewport().height();

        int centered = y - (height - lineHeight) / 2;

        // the two biased kinds only centre a position which is not on screen already, so that a caret which is
        // merely walking down the visible text does not throw the whole document up and down under it
        boolean visible = y >= current && y + lineHeight <= current + height;

        return switch (scrollType) {
            case CENTER -> centered;
            case CENTER_UP, CENTER_DOWN -> visible ? current : centered;
            case RELATIVE, MAKE_VISIBLE -> minimalOffset(current, height, y, lineHeight);
        };
    }

    private int minimalOffset(int current, int height, int y, int lineHeight) {
        int margin = VERTICAL_MARGIN_LINES * lineHeight;

        // a margin bigger than the viewport would push the two bounds past each other
        int usableMargin = Math.min(margin, Math.max(0, (height - lineHeight) / 2));

        if (y - usableMargin < current) {
            return y - usableMargin;
        }

        int bottom = y + lineHeight + usableMargin;
        if (bottom > current + height) {
            return bottom - height;
        }

        return current;
    }

    private int horizontalOffsetFor(DesktopQtEditorWidget surface, int x) {
        int current = surface.horizontalScrollBar().value();
        int width = surface.viewport().width();

        // a line long enough to scroll sideways is the exception, so this always moves as little as it can
        if (x < current) {
            return x;
        }

        if (x > current + width) {
            return x - width / 2;
        }

        return current;
    }

    @Override
    public void disableAnimation() {
    }

    @Override
    public void enableAnimation() {
    }

    @Override
    public int getVerticalScrollOffset() {
        DesktopQtEditorWidget surface = getSurface();
        return surface == null ? 0 : surface.verticalScrollBar().value();
    }

    @Override
    public int getHorizontalScrollOffset() {
        DesktopQtEditorWidget surface = getSurface();
        return surface == null ? 0 : surface.horizontalScrollBar().value();
    }

    @Override
    public void scrollVertically(int scrollOffset) {
        DesktopQtEditorWidget surface = getSurface();
        if (surface != null) {
            setValue(surface.verticalScrollBar(), scrollOffset);
        }
    }

    @Override
    public void scrollHorizontally(int scrollOffset) {
        DesktopQtEditorWidget surface = getSurface();
        if (surface != null) {
            setValue(surface.horizontalScrollBar(), scrollOffset);
        }
    }

    @Override
    public void scroll(int horizontalOffset, int verticalOffset) {
        scrollHorizontally(horizontalOffset);
        scrollVertically(verticalOffset);
    }

    /**
     * A scroll bar clamps to its own range anyway, but doing it here keeps the value this model reports back the
     * same as the one it was handed.
     */
    private static void setValue(QScrollBar scrollBar, int value) {
        scrollBar.setValue(Math.max(scrollBar.minimum(), Math.min(scrollBar.maximum(), value)));
    }
}
