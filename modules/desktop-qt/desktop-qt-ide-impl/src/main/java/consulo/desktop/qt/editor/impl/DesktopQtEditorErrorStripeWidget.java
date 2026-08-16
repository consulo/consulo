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
package consulo.desktop.qt.editor.impl;

import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.MarkupModelListener;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.document.Document;
import consulo.codeEditor.markup.MarkupModel;
import consulo.ui.color.ColorValue;
import io.qt.core.Qt;
import io.qt.gui.QCursor;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The strip of marks down the right of the editor - the map of everything the analyser found in the file, not just
 * in the part of it on screen.
 * <p>
 * The whole document is squeezed into the height of the strip, so a mark says where a problem is in the file rather
 * than where it is on screen. Clicking one jumps there, which is the point of it.
 * <p>
 * Marks come from two markup models: the one belonging to the editor, and the one belonging to the document, which
 * is where the daemon puts its results. Reading only the editor's would leave the strip empty.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorErrorStripeWidget extends QWidget {
    private static final int STRIPE_WIDTH = 14;

    /**
     * A single line is a fraction of a pixel in a long file, so a mark is drawn at least this tall to stay
     * visible - the awt strip calls the same thing the minimum mark height.
     */
    private static final int MIN_MARK_HEIGHT = 3;

    private static final int MARK_INSET = 3;

    private final DesktopQtEditorWidget mySurface;
    private final DesktopQtEditorImpl myEditor;

    private boolean myVisible;

    public DesktopQtEditorErrorStripeWidget(DesktopQtEditorWidget surface, DesktopQtEditorImpl editor) {
        super(surface);
        mySurface = surface;
        myEditor = editor;

        setCursor(new QCursor(Qt.CursorShape.ArrowCursor));
    }

    public void setStripeVisible(boolean visible) {
        if (myVisible != visible) {
            myVisible = visible;

            mySurface.updateSideAreas();
        }
    }

    public boolean isStripeVisible() {
        return myVisible;
    }

    public int preferredWidth() {
        return myVisible ? STRIPE_WIDTH : 0;
    }

    @Override
    protected void paintEvent(QPaintEvent event) {
        QPainter painter = new QPainter(this);
        try {
            paint(painter);
        }
        finally {
            painter.end();
        }
    }

    private void paint(QPainter painter) {
        EditorColorsScheme scheme = myEditor.getColorsScheme();

        painter.fillRect(0, 0, width(), height(), TargetQt.to(scheme.getDefaultBackground()));

        for (RangeHighlighter highlighter : collectHighlighters()) {
            ColorValue color = highlighter.getErrorStripeMarkColor(scheme);
            if (color == null) {
                continue;
            }

            int top = offsetToY(highlighter.getStartOffset());
            int bottom = offsetToY(highlighter.getEndOffset());
            int markHeight = Math.max(MIN_MARK_HEIGHT, bottom - top);

            painter.fillRect(MARK_INSET, top, width() - 2 * MARK_INSET, markHeight, TargetQt.to(color));
        }
    }

    /**
     * Where an offset falls on the strip. The strip stands for the whole document, so this is a proportion of the
     * text length rather than anything to do with the scroll offset.
     */
    private int offsetToY(int offset) {
        Document document = myEditor.getDocument();

        int length = Math.max(1, document.getTextLength());
        int usable = Math.max(1, height() - MIN_MARK_HEIGHT);

        return (int) ((long) Math.max(0, Math.min(offset, length)) * usable / length);
    }

    private int yToOffset(int y) {
        Document document = myEditor.getDocument();

        int length = document.getTextLength();
        int usable = Math.max(1, height() - MIN_MARK_HEIGHT);

        return (int) Math.max(0, Math.min((long) y * length / usable, length));
    }

    private List<RangeHighlighter> collectHighlighters() {
        List<RangeHighlighter> highlighters = new ArrayList<>();

        collectFrom(myEditor.getMarkupModel(), highlighters);
        collectFrom(DocumentMarkupModel.forDocument(myEditor.getDocument(), myEditor.getProject(), false), highlighters);

        return highlighters;
    }

    private static void collectFrom(@Nullable MarkupModel model, List<RangeHighlighter> into) {
        if (model == null) {
            return;
        }

        for (RangeHighlighter highlighter : model.getAllHighlighters()) {
            if (highlighter.isValid()) {
                into.add(highlighter);
            }
        }
    }

    /**
     * Jumps to what was clicked. The strip is a map of the file, so a click on it is a request to go there.
     */
    @Override
    protected void mousePressEvent(QMouseEvent event) {
        if (event.button() != Qt.MouseButton.LeftButton) {
            super.mousePressEvent(event);
            return;
        }

        myEditor.getCaretModel().moveToOffset(yToOffset(event.pos().y()));
        myEditor.getScrollingModel().scrollToCaret(ScrollType.CENTER);

        event.accept();
    }

    /**
     * Listens to both markup models for as long as the editor lives, so the strip redraws when the analyser
     * reports something new.
     */
    public void listenToMarkup() {
        MarkupModelEx editorMarkup = (MarkupModelEx) myEditor.getMarkupModel();
        editorMarkup.addMarkupModelListener(myEditor.getDisposable(), new RepaintOnMarkupChange());

        MarkupModelEx documentMarkup = DocumentMarkupModel.forDocument(myEditor.getDocument(), myEditor.getProject(), true);
        if (documentMarkup != null) {
            documentMarkup.addMarkupModelListener(myEditor.getDisposable(), new RepaintOnMarkupChange());
        }
    }

    private class RepaintOnMarkupChange implements MarkupModelListener {
        @Override
        public void afterAdded(RangeHighlighterEx highlighter) {
            markupChanged();
        }

        @Override
        public void afterRemoved(RangeHighlighterEx highlighter) {
            markupChanged();
        }

        @Override
        public void attributesChanged(RangeHighlighterEx highlighter, boolean renderersChanged, boolean fontStyleChanged) {
            markupChanged();
        }

        /**
         * The counters are read off the same analysis the marks come from, so they are refreshed together.
         */
        private void markupChanged() {
            update();

            mySurface.getStatusPanel().refresh();
        }
    }
}
