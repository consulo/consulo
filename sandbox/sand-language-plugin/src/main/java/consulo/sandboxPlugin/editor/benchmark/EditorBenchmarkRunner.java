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
package consulo.sandboxPlugin.editor.benchmark;

import consulo.codeEditor.CaretModel;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.FoldRegion;
import consulo.codeEditor.FoldingModel;
import consulo.codeEditor.InlayModel;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollingModel;
import consulo.codeEditor.internal.benchmark.EditorBenchmarkView;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.SeparatorPlacement;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;

import java.awt.Point;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
public class EditorBenchmarkRunner {
    private static final Logger LOG = Logger.getInstance(EditorBenchmarkRunner.class);

    private static final int WARMUP = 5;
    private static final int RUNS = 40;
    private static final long PAINT_TIMEOUT_NANOS = 250_000_000L;
    private static final Runnable NOTHING = () -> {
    };

    private final Project myProject;
    private final EditorEx myEditor;
    private final EditorBenchmarkView myView;
    private final EditorBenchmarkSource mySource;
    private final EditorBenchmarkReport myReport;
    private final ThreadMXBean myThreads = ManagementFactory.getThreadMXBean();

    public EditorBenchmarkRunner(
        Project project,
        EditorEx editor,
        EditorBenchmarkView view,
        EditorBenchmarkSource source,
        EditorBenchmarkReport report
    ) {
        myProject = project;
        myEditor = editor;
        myView = view;
        mySource = source;
        myReport = report;
    }

    @RequiredUIAccess
    public void decorate() {
        Document document = myEditor.getDocument();
        EditorColorsScheme scheme = myEditor.getColorsScheme();

        myEditor.getMarkupModel().setErrorStripeVisible(true);

        long start = System.nanoTime();
        EditorHighlighter highlighter =
            EditorHighlighterFactory.getInstance().createEditorHighlighter(new EditorBenchmarkSyntaxHighlighter(), scheme);
        myEditor.setHighlighter(highlighter);
        myReport.setup("lexer highlighter", System.nanoTime() - start);

        MarkupModelEx markup = (MarkupModelEx) Objects.requireNonNull(DocumentMarkupModel.forDocument(document, myProject, true));

        start = System.nanoTime();
        for (EditorBenchmarkSpan span : mySource.spans()) {
            markup.addRangeHighlighter(span.key(), span.startOffset(), span.endOffset(), span.layer(), HighlighterTargetArea.EXACT_RANGE);
        }
        myReport.setup("highlighters (" + mySource.spans().size() + ")", System.nanoTime() - start);

        start = System.nanoTime();
        ColorValue separatorColor = scheme.getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR);
        for (EditorBenchmarkMethod method : mySource.methods()) {
            if (separatorColor != null) {
                RangeHighlighter separator = markup.addRangeHighlighter(
                    null,
                    method.separatorOffset(),
                    method.separatorOffset(),
                    HighlighterLayer.ADDITIONAL_SYNTAX,
                    HighlighterTargetArea.EXACT_RANGE
                );
                separator.setLineSeparatorColor(TargetAWT.to(separatorColor));
                separator.setLineSeparatorPlacement(SeparatorPlacement.TOP);
            }
            if (method.overriding()) {
                addGutterIcon(markup, method, PlatformIconGroup.gutterImplementingmethod(), "Implements method");
            }
            if (method.overridden()) {
                addGutterIcon(markup, method, PlatformIconGroup.gutterOverridenmethod(), "Is overridden");
            }
        }
        myReport.setup("separators and gutter icons (" + mySource.methods().size() + " methods)", System.nanoTime() - start);

        start = System.nanoTime();
        FoldingModel foldingModel = myEditor.getFoldingModel();
        foldingModel.runBatchFoldingOperation(() -> {
            for (EditorBenchmarkFold fold : mySource.folds()) {
                FoldRegion region = foldingModel.addFoldRegion(fold.startOffset(), fold.endOffset(), fold.placeholder());
                if (region != null) {
                    region.setExpanded(!fold.collapsed());
                }
            }
        });
        myReport.setup("fold regions (" + mySource.folds().size() + ")", System.nanoTime() - start);

        start = System.nanoTime();
        InlayModel inlayModel = myEditor.getInlayModel();
        inlayModel.execute(true, () -> {
            for (EditorBenchmarkHint hint : mySource.hints()) {
                inlayModel.addInlineElement(hint.offset(), myView.createHintRenderer(hint.text()));
            }
        });
        myReport.setup("parameter hints (" + mySource.hints().size() + ")", System.nanoTime() - start);
    }

    @RequiredUIAccess
    public void run() {
        myReport.clearMeasurements();

        if (!myView.isShown()) {
            myReport.error("the editor is not shown");
            return;
        }

        describeEnvironment();

        myEditor.getScrollingModel().disableAnimation();
        myView.beginRun();
        try {
            runScenarios();
        }
        catch (RuntimeException e) {
            LOG.error(e);
            myReport.error(e.toString());
        }
        finally {
            myView.endRun();
            myEditor.getScrollingModel().enableAnimation();
        }

        LOG.info(myReport.toText());
    }

    private void describeEnvironment() {
        Document document = myEditor.getDocument();
        EditorColorsScheme scheme = myEditor.getColorsScheme();
        int lineHeight = myEditor.getLineHeight();
        MarkupModelEx markup = (MarkupModelEx) DocumentMarkupModel.forDocument(document, myProject, false);

        myReport.environment("frontend", myView.getFrontendName());
        myReport.environment("device pixel ratio", myView.getDevicePixelRatio());
        myReport.environment("java", Runtime.version());
        myReport.environment("cpu", System.getProperty("os.arch") + ", " + Runtime.getRuntime().availableProcessors() + " cores");
        myReport.environment("font", scheme.getEditorFontName() + " " + scheme.getEditorFontSize() + "px, line height " + lineHeight + "px");
        myReport.environment("viewport", myView.getViewportWidth() + "x" + myView.getViewportHeight()
            + " (" + myView.getViewportHeight() / lineHeight + " lines)");
        myReport.environment("document", document.getLineCount() + " lines, " + document.getTextLength() + " chars");
        myReport.environment("highlighters", markup == null ? 0 : markup.getAllHighlighters().length);
        myReport.environment("fold regions", myEditor.getFoldingModel().getAllFoldRegions().length);
        myReport.environment("inlays", myEditor.getInlayModel().getInlineElementsInRange(0, document.getTextLength()).size());
        myReport.environment("long lines", lineLength(mySource.typicalLine()) + " / " + lineLength(mySource.longLine()) + " / "
            + lineLength(mySource.veryLongLine()) + " chars");
    }

    @RequiredUIAccess
    private void runScenarios() {
        Document document = myEditor.getDocument();
        ScrollingModel scrolling = myEditor.getScrollingModel();
        CaretModel caretModel = myEditor.getCaretModel();
        int lineHeight = myEditor.getLineHeight();
        int viewportHeight = myView.getViewportHeight();
        int visibleLines = Math.max(1, viewportHeight / lineHeight);

        int typicalLine = mySource.typicalLine();
        int typicalStart = document.getLineStartOffset(typicalLine);
        int middle = Math.max(0, myEditor.logicalPositionToXY(new LogicalPosition(typicalLine, 0)).y - viewportHeight / 2);

        scrollTo(scrolling, 0);
        measure("paint text offscreen, top", WARMUP, RUNS, NOTHING, () -> myView.paintTextOffscreen(0), false);
        measure("paint text offscreen, middle", WARMUP, RUNS, NOTHING, () -> myView.paintTextOffscreen(middle), false);

        scrollTo(scrolling, middle);
        measure("repaint text area", WARMUP, RUNS, NOTHING, myView::repaintTextArea, false);
        measure("repaint gutter", WARMUP, RUNS, NOTHING, myView::repaintGutter, false);
        if (myView.hasErrorStripe()) {
            measure("repaint error stripe", WARMUP, RUNS, NOTHING, myView::repaintErrorStripe, false);
        }
        else {
            myReport.error("the error stripe is not shown, its repaint is not measured");
        }

        int scrollLimit = middle + viewportHeight * 20;
        Runnable rewind = () -> {
            if (scrolling.getVerticalScrollOffset() > scrollLimit) {
                scrollTo(scrolling, middle);
            }
        };
        measure("scroll by 3 lines", WARMUP, RUNS, rewind,
            () -> scrolling.scrollVertically(scrolling.getVerticalScrollOffset() + 3 * lineHeight), true);
        measure("scroll by a page", WARMUP, RUNS, rewind,
            () -> scrolling.scrollVertically(scrolling.getVerticalScrollOffset() + viewportHeight), true);

        scrollTo(scrolling, middle);
        caretModel.moveToOffset(typicalStart);
        flush();
        measure("move caret down", WARMUP, RUNS, () -> {
            if (caretModel.getLogicalPosition().line > typicalLine + visibleLines / 3) {
                caretModel.moveToOffset(typicalStart);
                flush();
            }
        }, () -> caretModel.moveCaretRelatively(0, 1, false, false, true), true);

        caretModel.moveToOffset(typicalStart + Math.min(40, lineLength(typicalLine)));
        flush();
        measure("type a character", WARMUP, RUNS, NOTHING, () -> WriteCommandAction.runWriteCommandAction(myProject, () -> {
            int offset = caretModel.getOffset();
            document.insertString(offset, "x");
            caretModel.moveToOffset(offset + 1);
        }), true);
        measure("delete a character", WARMUP, RUNS, NOTHING, () -> WriteCommandAction.runWriteCommandAction(myProject, () -> {
            int offset = caretModel.getOffset();
            document.deleteString(offset - 1, offset);
        }), true);

        hitTest("hit test, end of typical line", typicalLine, WARMUP, RUNS);
        hitTest("hit test, end of long line", mySource.longLine(), 2, 10);
        hitTest("hit test, end of very long line", mySource.veryLongLine(), 1, 3);

        int veryLongLine = mySource.veryLongLine();
        LogicalPosition veryLongEnd = new LogicalPosition(veryLongLine, lineLength(veryLongLine));
        measure("caret x, end of very long line", WARMUP, RUNS, NOTHING, () -> myEditor.logicalPositionToXY(veryLongEnd), false);

        measure("measure document width", 1, 3, NOTHING, () -> myEditor.getMaxWidthInRange(0, document.getTextLength()), false);

        FoldingModel foldingModel = myEditor.getFoldingModel();
        FoldRegion[] regions = foldingModel.getAllFoldRegions();
        scrollTo(scrolling, middle);
        measure("toggle all folds", 2, 10, NOTHING, () -> foldingModel.runBatchFoldingOperation(() -> {
            for (FoldRegion region : regions) {
                region.setExpanded(!region.isExpanded());
            }
        }), true);

        caretModel.moveToOffset(0);
        scrollTo(scrolling, 0);
    }

    private void hitTest(String scenario, int line, int warmup, int runs) {
        Point end = myEditor.logicalPositionToXY(new LogicalPosition(line, lineLength(line)));
        Point target = new Point(Math.max(0, end.x - 1), end.y + 1);
        measure(scenario, warmup, runs, NOTHING, () -> myEditor.xyToLogicalPosition(target), false);
    }

    private void measure(String scenario, int warmup, int runs, Runnable before, Runnable step, boolean awaitPaint) {
        long[] cpu = new long[runs];
        long[] wall = new long[runs];
        long[] painted = new long[runs];

        for (int i = -warmup; i < runs; i++) {
            before.run();

            myView.resetPaint();
            long cpuStart = cpuTime();
            long wallStart = System.nanoTime();

            step.run();
            if (awaitPaint) {
                myView.flushPaint(wallStart + PAINT_TIMEOUT_NANOS);
            }

            if (i >= 0) {
                cpu[i] = cpuTime() - cpuStart;
                wall[i] = System.nanoTime() - wallStart;
                painted[i] = myView.getPaintedPixels();
            }
        }

        long viewportPixels = (long) myView.getViewportWidth() * myView.getViewportHeight();
        myReport.result(EditorBenchmarkResult.of(scenario, cpu, wall, painted, viewportPixels));
    }

    private void scrollTo(ScrollingModel scrolling, int offset) {
        scrolling.scrollVertically(offset);
        flush();
    }

    private void flush() {
        myView.resetPaint();
        myView.flushPaint(System.nanoTime() + PAINT_TIMEOUT_NANOS);
    }

    private long cpuTime() {
        return myThreads.isCurrentThreadCpuTimeSupported() ? myThreads.getCurrentThreadCpuTime() : System.nanoTime();
    }

    private int lineLength(int line) {
        Document document = myEditor.getDocument();
        return document.getLineEndOffset(line) - document.getLineStartOffset(line);
    }

    private static void addGutterIcon(MarkupModelEx markup, EditorBenchmarkMethod method, Image icon, String tooltip) {
        RangeHighlighter highlighter = markup.addRangeHighlighter(
            null,
            method.nameStartOffset(),
            method.nameEndOffset(),
            HighlighterLayer.ADDITIONAL_SYNTAX,
            HighlighterTargetArea.EXACT_RANGE
        );
        highlighter.setGutterIconRenderer(new EditorBenchmarkGutterIconRenderer(icon, LocalizeValue.localizeTODO(tooltip)));
    }
}
