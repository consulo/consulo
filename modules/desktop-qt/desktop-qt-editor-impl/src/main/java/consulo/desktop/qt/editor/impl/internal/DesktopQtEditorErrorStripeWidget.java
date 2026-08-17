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

import consulo.application.Application;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.MarkupModelListener;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.document.Document;
import consulo.codeEditor.markup.MarkupModel;
import consulo.language.editor.impl.internal.markup.AnalyzerStatus;
import consulo.language.editor.impl.internal.markup.AnalyzingType;
import consulo.language.editor.impl.internal.markup.ErrorStripeRenderer;
import consulo.language.editor.impl.internal.markup.PassWrapper;
import consulo.language.editor.impl.internal.markup.StatusItem;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import io.qt.core.QRect;
import io.qt.core.QTimer;
import io.qt.core.Qt;
import io.qt.gui.QCursor;
import io.qt.gui.QMouseEvent;
import io.qt.gui.QPaintEvent;
import io.qt.gui.QPainter;
import io.qt.widgets.QWidget;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
    private static final int STRIPE_WIDTH = 21;

    /**
     * A single line is a fraction of a pixel in a long file, so a mark is drawn at least this tall to stay
     * visible - the awt strip calls the same thing the minimum mark height.
     */
    private static final int MIN_MARK_HEIGHT = 3;

    private static final int MARK_INSET = 3;

    private static final int ICON_SIZE = 12;

    private static final int ICON_PADDING = 2;

    private static final int REFRESH_DELAY = 50;

    private final DesktopQtEditorImpl myEditor;

    private boolean myVisible;

    private @Nullable Image myStatusIcon;

    private final QTimer myStatusRefreshQueue;

    public DesktopQtEditorErrorStripeWidget(QWidget parent, DesktopQtEditorImpl editor) {
        super(parent);
        myEditor = editor;

        setCursor(new QCursor(Qt.CursorShape.ArrowCursor));

        myStatusRefreshQueue = new QTimer(this);
        myStatusRefreshQueue.setSingleShot(true);
        myStatusRefreshQueue.setInterval(REFRESH_DELAY);
        myStatusRefreshQueue.timeout.connect(this::refreshStatus);
    }

    /**
     * The room the icon takes at the top, which the marks then start below.
     */
    private int iconPanelSize() {
        return myStatusIcon == null ? ICON_PADDING : ICON_SIZE + 2 * ICON_PADDING;
    }

    public void refresh() {
        myStatusRefreshQueue.start();
    }

    private void refreshStatus() {
        AnalyzerStatus status = readStatus();

        Image icon = status == null ? null : status.getIcon();
        String tooltip = status == null ? "" : buildTooltip(status);

        if (myStatusIcon != icon) {
            myStatusIcon = icon;
        }

        setToolTip(tooltip);
        update();
    }

    private @Nullable AnalyzerStatus readStatus() {
        DesktopQtMarkupModelImpl markupModel = (DesktopQtMarkupModelImpl) myEditor.getMarkupModel();

        ErrorStripeRenderer renderer = markupModel.getErrorStripeRenderer();
        if (renderer == null || !markupModel.isErrorStripeVisible()) {
            return null;
        }

        return Application.get().runReadAction((Supplier<AnalyzerStatus>) () -> renderer.getStatus(myEditor));
    }

    private String buildTooltip(AnalyzerStatus status) {
        List<String> lines = new ArrayList<>();

        if (status.getTitle() != null && !status.getTitle().isEmpty()) {
            lines.add(status.getTitle());
        }
        if (status.getDetails() != null && !status.getDetails().isEmpty()) {
            lines.add(status.getDetails());
        }

        for (PassWrapper pass : status.getPasses()) {
            lines.add(pass.getPresentableName() + ": " + pass.toPercent() + "%");
        }

        if (lines.isEmpty()) {
            StringBuilder summary = new StringBuilder();
            for (StatusItem item : status.getExpandedStatus()) {
                if (!summary.isEmpty()) {
                    summary.append(", ");
                }

                summary.append(item.getText());

                if (item.getType() != null) {
                    summary.append(' ').append(item.getType());
                }
            }

            if (status.getAnalyzingType() != AnalyzingType.COMPLETE) {
                summary.append(" so far");
            }

            lines.add(summary.toString());
        }

        return String.join("\n", lines);
    }

    public void setStripeVisible(boolean visible) {
        if (myVisible != visible) {
            myVisible = visible;

            applyWidth();
        }
    }

    public boolean isStripeVisible() {
        return myVisible;
    }

    private void applyWidth() {
        setFixedWidth(preferredWidth());
        setVisible(myVisible);
    }

    public int preferredWidth() {
        return myVisible ? Math.max(STRIPE_WIDTH, ICON_SIZE + 2 * ICON_PADDING) : 0;
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

        paintStatusIcon(painter);
    }

    /**
     * The analyser's verdict for the whole file, at the top of the strip where awt paints it.
     */
    private void paintStatusIcon(QPainter painter) {
        if (!(myStatusIcon instanceof DesktopQtImage qtImage)) {
            return;
        }

        int x = Math.max(0, (width() - ICON_SIZE) / 2);

        qtImage.toQIcon().paint(painter, new QRect(x, ICON_PADDING, ICON_SIZE, ICON_SIZE));
    }

    /**
     * Where an offset falls on the strip. The strip stands for the whole document, so this is a proportion of the
     * text length rather than anything to do with the scroll offset - taken below the icon, which is not part of
     * the map.
     */
    private int offsetToY(int offset) {
        Document document = myEditor.getDocument();

        int length = Math.max(1, document.getTextLength());
        int top = iconPanelSize();
        int usable = Math.max(1, height() - top - MIN_MARK_HEIGHT);

        return top + (int) ((long) Math.max(0, Math.min(offset, length)) * usable / length);
    }

    private int yToOffset(int y) {
        Document document = myEditor.getDocument();

        int length = document.getTextLength();
        int top = iconPanelSize();
        int usable = Math.max(1, height() - top - MIN_MARK_HEIGHT);

        return (int) Math.max(0, Math.min((long) Math.max(0, y - top) * length / usable, length));
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
        // the strip outlives none of this: the editor keeps its markup models while the widget is thrown away and
        // built again whenever the component is bound anew, so a listener tied to the editor would go on calling
        // a widget qt has already destroyed. It is tied to the widget instead, and taken off when that goes.
        Disposable lifetime = Disposable.newDisposable("qt editor error stripe markup");
        Disposer.register(myEditor.getDisposable(), lifetime);

        destroyed.connect(() -> Disposer.dispose(lifetime));

        MarkupModelEx editorMarkup = (MarkupModelEx) myEditor.getMarkupModel();
        editorMarkup.addMarkupModelListener(lifetime, new RepaintOnMarkupChange());

        MarkupModelEx documentMarkup = DocumentMarkupModel.forDocument(myEditor.getDocument(), myEditor.getProject(), true);
        if (documentMarkup != null) {
            documentMarkup.addMarkupModelListener(lifetime, new RepaintOnMarkupChange());
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
         * The icon is read off the same analysis the marks come from, so they are refreshed together.
         */
        private void markupChanged() {
            // the markup can change inside the same event the widget is destroyed in
            if (isDisposed()) {
                return;
            }

            update();

            myStatusRefreshQueue.start();
        }
    }
}
