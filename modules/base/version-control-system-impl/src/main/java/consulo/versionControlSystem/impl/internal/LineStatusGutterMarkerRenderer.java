/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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

import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.codeEditor.markup.*;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.versionControlSystem.internal.LineStatusTrackerListener;
import consulo.versionControlSystem.internal.VcsRange;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Manages a single document-level gutter highlighter that paints all VCS changed-line
 * markers in one pass at repaint time. Equivalent to JetBrains'
 * {@code LineStatusMarkerRenderer} (outer highlighter lifecycle) combined with its inner
 * {@code LineStatusGutterMarkerRenderer} (the actual painter).
 *
 * <p>Error-stripe (scrollbar) markers are still managed per-range via
 * {@link LineStatusTracker#createHighlighter(VcsRange)}.
 *
 * <p>Updates are debounced via {@link MergingUpdateQueue} (100 ms).
 */
public class LineStatusGutterMarkerRenderer {
    private static final Logger LOG = Logger.getInstance(LineStatusGutterMarkerRenderer.class);

    private final LineStatusTracker myTracker;
    private final Document myDocument;
    private final @Nullable Project myProject;

    private final MergingUpdateQueue myUpdateQueue;

    /** Disposable that controls the EditorFactory listener lifetime. */
    private final Disposable myEditorListenerDisposable = Disposable.newDisposable();

    /** Single document-wide gutter highlighter. Repainted on demand, never recreated. */
    private @Nullable RangeHighlighter myGutterHighlighter;

    private volatile boolean myDisposed = false;

    // -------------------------------------------------------------------------

    public LineStatusGutterMarkerRenderer(LineStatusTracker tracker) {
        myTracker = tracker;
        myDocument = tracker.getDocument();
        myProject = tracker.getProject();

        myUpdateQueue = new MergingUpdateQueue(
            "LineStatusGutterMarkerRenderer", 100, true,
            MergingUpdateQueue.ANY_COMPONENT, null
        );

        myGutterHighlighter = createGutterHighlighter();

        // Revalidate existing editors so the gutter allocates the right free-painters area.
        // calcLineMarkerAreaWidth() detects our RIGHT renderer and sets myRightFreePaintersAreaShown=true.
        // Without this the paint area has 0 width and nothing is visible.
        revalidateAllEditors();

        // Also revalidate any editor opened AFTER we are created.
        EditorFactory.getInstance().addEditorFactoryListener(new MyEditorFactoryListener(), myEditorListenerDisposable);

        tracker.addListener(new LineStatusTrackerListener() {
            @Override
            public void onRangesChanged() {
                scheduleUpdate();
            }

            @Override
            public void onBecomingValid() {
                scheduleUpdate();
            }
        });

        scheduleUpdate();
    }

    // -------------------------------------------------------------------------
    // Highlighter lifecycle
    // -------------------------------------------------------------------------

    private RangeHighlighter createGutterHighlighter() {
        MarkupModel markupModel = DocumentMarkupModel.forDocument(myDocument, myProject, true);
        RangeHighlighter h = markupModel.addRangeHighlighter(
            0,
            myDocument.getTextLength(),
            HighlighterLayer.FIRST - 1,
            null,  // no text-attributes; this highlighter is for gutter painting only
            HighlighterTargetArea.LINES_IN_RANGE
        );
        h.setGreedyToLeft(true);
        h.setGreedyToRight(true);
        h.setLineMarkerRenderer(new MyGutterMarkerRenderer());
        return h;
    }

    /**
     * Calls {@code revalidateMarkup()} on every editor showing {@link #myDocument}.
     * This triggers {@code calcLineMarkerAreaWidth()} in the gutter, which detects our
     * {@code RIGHT}-positioned renderer and allocates the free-painters area.
     */
    private void revalidateAllEditors() {
        for (Editor editor : EditorFactory.getInstance().getEditors(myDocument)) {
            if (editor instanceof EditorEx) {
                ((EditorEx) editor).getGutterComponentEx().revalidateMarkup();
            }
        }
    }

    /**
     * Schedules a gutter repaint via the merge queue (100 ms debounce).
     * Can be called from any thread; the actual repaint runs on EDT.
     */
    public void scheduleUpdate() {
        myUpdateQueue.queue(new Update("update") {
            @Override
            public void run() {
                if (myDisposed) return;
                repaintGutter();
            }
        });
    }

    @RequiredUIAccess
    private void repaintGutter() {
        for (Editor editor : EditorFactory.getInstance().getEditors(myDocument)) {
            if (editor instanceof EditorEx) {
                ((EditorEx) editor).getGutterComponentEx().repaint();
            }
        }
    }

    public void dispose() {
        myDisposed = true;
        Disposer.dispose(myEditorListenerDisposable);
        RangeHighlighter h = myGutterHighlighter;
        myGutterHighlighter = null;
        if (h != null) {
            try {
                h.dispose();
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
        myUpdateQueue.dispose();
    }

    // -------------------------------------------------------------------------
    // Gutter marker renderer (inner class)
    // -------------------------------------------------------------------------

    /**
     * Single renderer instance that paints ALL changed-line ranges on each repaint.
     * Equivalent to JB's inner {@code LineStatusGutterMarkerRenderer}.
     */
    private class MyGutterMarkerRenderer implements ActiveGutterRenderer {

        // ActiveGutterRenderer has mutual recursion between getTooltipValue() ↔ getTooltipText().
        // Break the cycle by overriding getTooltipValue() directly.
        @Override
        public LocalizeValue getTooltipValue() {
            return LocalizeValue.empty();
        }

        @Override
        public void paint(Editor editor, Graphics g, Rectangle r) {
            if (myDisposed) return;
            List<VcsRange> ranges = myTracker.getRanges();
            if (ranges == null) return;
            for (VcsRange range : ranges) {
                LineStatusMarkerDrawUtil.paintRange(range, editor, g, r);
            }
        }

        @Override
        public boolean canDoAction(MouseEvent e) {
            if (myDisposed) return false;
            return LineStatusMarkerDrawUtil.isInsideMarkerArea(e);
        }

        @Override
        @RequiredUIAccess
        public void doAction(Editor editor, MouseEvent e) {
            if (myDisposed) return;
            VcsRange range = findRangeAtEvent(editor, e);
            if (range != null) {
                new LineStatusTrackerDrawing.MyLineStatusMarkerPopup(myTracker, editor, range).showHint(e);
            }
        }

        /** Finds the VCS range whose gutter area contains the click y-coordinate. */
        private @Nullable VcsRange findRangeAtEvent(Editor editor, MouseEvent e) {
            int line = editor.xyToLogicalPosition(e.getPoint()).line;
            // First try the clicked line; for DELETED ranges (displayed as triangles between
            // lines) also check the line below.
            VcsRange range = myTracker.getRangeForLine(line);
            if (range == null) {
                range = myTracker.getRangeForLine(line + 1);
            }
            return range;
        }
    }

    // -------------------------------------------------------------------------
    // EditorFactory listener – revalidate newly opened editors
    // -------------------------------------------------------------------------

    private class MyEditorFactoryListener implements EditorFactoryListener {
        @Override
        public void editorCreated(EditorFactoryEvent event) {
            Editor editor = event.getEditor();
            if (editor.getDocument() == myDocument && editor instanceof EditorEx) {
                ((EditorEx) editor).getGutterComponentEx().revalidateMarkup();
            }
        }
    }
}
