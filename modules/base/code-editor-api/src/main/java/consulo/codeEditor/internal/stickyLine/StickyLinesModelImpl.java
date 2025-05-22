// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.codeEditor.internal.stickyLine;

import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.markup.*;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class StickyLinesModelImpl implements StickyLinesModel {
    private static final Logger LOG = Logger.getInstance(StickyLinesModelImpl.class);
    private static final Key<SourceID> STICKY_LINE_SOURCE = Key.create("editor.sticky.lines.source");
    private static final Key<StickyLinesModelImpl> STICKY_LINES_MODEL_KEY = Key.create("editor.sticky.lines.model");
    private static final Key<StickyLineImpl> STICKY_LINE_IMPL_KEY = Key.create("editor.sticky.line.impl");
    private static final String STICKY_LINE_MARKER = "STICKY_LINE_MARKER";
    private static final TextAttributesKey STICKY_LINE_ATTRIBUTE = TextAttributesKey.createTextAttributesKey(
        STICKY_LINE_MARKER
    );

    public static boolean isStickyLine(@Nonnull RangeHighlighter highlighter) {
        TextAttributesKey key = highlighter.getTextAttributesKey();
        if (key != null && STICKY_LINE_MARKER.equals(key.getExternalName())) {
            return true;
        }
        return false;
    }

    /**
     * IJPL-26873 skip sticky line highlighter in all editors.
     *
     * <p>Even though the sticky highlighter does not contain text attributes,
     * {@code highlighter.getTextAttributes(editor.getColorsScheme())} may return non-null attributes.
     * Which means incorrect text color.
     */
    public static void skipInAllEditors(@Nonnull RangeHighlighter highlighter) {
        highlighter.setEditorFilter(StickyLinesModelImpl::alwaysFalsePredicate);
    }

    static @Nullable StickyLinesModelImpl getModel(@Nonnull Project project, @Nonnull Document document) {
        if (project.isDisposed()) {
            String editors = editorsAsString(document);
            LOG.error(
                """
                  ______________________________________________________________________________________
                  getting sticky lines model when project is already disposed
                  disposed project: %s
                  editors:
                  %s
                  ______________________________________________________________________________________
                  """.
                    formatted(project, editors),
                new Throwable()
            );
            return null;
        }
        MarkupModel markupModel = DocumentMarkupModel.forDocument(document, project, false);
        if (markupModel == null) {
            String editors = editorsAsString(document);
            // TODO: it should be error but in test `RunWithComposeHotReloadRunGutterSmokeTest` happens something crazy:
            //  editor and markup model do not exist inside `doApplyInformationToEditor`
            LOG.warn /*error*/ (
                """
                  ______________________________________________________________________________________
                  getting sticky lines model when markup model is not created
                  editors:
                  %s
                  ______________________________________________________________________________________
                  """.formatted(editors),
                new Throwable()
            );
            return null;
        }
        return getModel(markupModel);
    }

    static @Nonnull StickyLinesModelImpl getModel(@Nonnull MarkupModel markupModel) {
        StickyLinesModelImpl stickyModel = markupModel.getUserData(STICKY_LINES_MODEL_KEY);
        if (stickyModel == null) {
            stickyModel = new StickyLinesModelImpl((MarkupModelEx) markupModel);
            markupModel.putUserData(STICKY_LINES_MODEL_KEY, stickyModel);
        }
        return stickyModel;
    }

    private final MarkupModelEx myMarkupModel;
    private final List<Listener> myListeners;
    private boolean myIsCleared;

    private StickyLinesModelImpl(MarkupModelEx markupModel) {
        myMarkupModel = markupModel;
        myListeners = new ArrayList<>();
        myIsCleared = false;
    }

    @Override
    public @Nonnull StickyLine addStickyLine(@Nonnull SourceID source, int startOffset, int endOffset, @Nullable String debugText) {
        if (startOffset >= endOffset) {
            throw new IllegalArgumentException(String.format(
                "sticky line endOffset %s should be less than startOffset %s", startOffset, endOffset
            ));
        }
        RangeHighlighter highlighter = myMarkupModel.addRangeHighlighter(
            STICKY_LINE_ATTRIBUTE,
            startOffset,
            endOffset,
            0, // value should be less than SYNTAX because of bug in colors scheme IJPL-149486
            HighlighterTargetArea.EXACT_RANGE
        );
        StickyLineImpl stickyLine = new StickyLineImpl(highlighter.getDocument(), highlighter, debugText);
        highlighter.putUserData(STICKY_LINE_IMPL_KEY, stickyLine);
        highlighter.putUserData(STICKY_LINE_SOURCE, source);
        skipInAllEditors(highlighter);
        myIsCleared = false;
        return stickyLine;
    }

    @Override
    public void removeStickyLine(@Nonnull StickyLine stickyLine) {
        RangeMarker rangeMarker = ((StickyLineImpl)stickyLine).rangeMarker();
        myMarkupModel.removeHighlighter((RangeHighlighter) rangeMarker);
    }

    @Override
    public void processStickyLines(int startOffset, int endOffset, @Nonnull Predicate<? super StickyLine> processor) {
        processStickyLines(null, startOffset, endOffset, processor);
    }

    @Override
    public void processStickyLines(@Nonnull SourceID source, @Nonnull Predicate<? super StickyLine> processor) {
        processStickyLines(source, 0, myMarkupModel.getDocument().getTextLength(), processor);
    }

    @Override
    public @Nonnull List<StickyLine> getAllStickyLines() {
        ArrayList<StickyLine> stickyLines = new ArrayList<>();
        processStickyLines(
            0,
            myMarkupModel.getDocument().getTextLength(),
            line -> {
                stickyLines.add(line);
                return true;
            }
        );
        return stickyLines;
    }

    @Override
    public void addListener(@Nonnull Listener listener) {
        myListeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull Listener listener) {
        myListeners.remove(listener);
    }

    @Override
    public void notifyLinesUpdate() {
        for (Listener listener : myListeners) {
            listener.linesUpdated();
        }
    }

    @Override
    public void removeAllStickyLines(@Nullable Project project) {
        if (myIsCleared) {
            return;
        }
        for (StickyLine line : getAllStickyLines()) {
            removeStickyLine(line);
        }
        for (Listener listener : myListeners) {
            listener.linesRemoved();
        }
        if (project != null) {
            restartStickyLinesPass(project);
        }
        myIsCleared = true;
    }

    private void processStickyLines(
        @Nullable SourceID source,
        int startOffset,
        int endOffset,
        @Nonnull Predicate<? super StickyLine> processor
    ) {
        myMarkupModel.processRangeHighlightersOverlappingWith(
            startOffset,
            endOffset,
            highlighter -> {
                if (STICKY_LINE_ATTRIBUTE.equals(highlighter.getTextAttributesKey()) && isSuitableSource(highlighter, source)) {
                    StickyLineImpl stickyLine = highlighter.getUserData(STICKY_LINE_IMPL_KEY);
                    if (stickyLine == null) {
                        // probably it is a zombie highlighter
                        stickyLine = new StickyLineImpl(highlighter.getDocument(), highlighter, "StickyZombie");
                    }
                    return processor.test(stickyLine);
                } else {
                    return true;
                }
            }
        );
    }

    private static boolean isSuitableSource(RangeHighlighterEx highlighter, @Nullable SourceID source) {
        return source == null || source.equals(highlighter.getUserData(STICKY_LINE_SOURCE));
    }

    private static boolean alwaysFalsePredicate(@Nonnull Editor editor) {
        return false;
    }

    private void restartStickyLinesPass(@Nonnull Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            ReadAction.run(() -> {
                if (!project.isDisposed()) {
                    StickyLinesCollector collector = new StickyLinesCollector(project, myMarkupModel.getDocument());
                    collector.forceCollectPass();
                }
            });
        });
    }

    private static @Nonnull String editorsAsString(@Nonnull Document document) {
        return Arrays.stream(EditorFactory.getInstance().getEditors(document))
            .map(editor -> editor.toString() + "\n" + editor.getProject())
            .collect(Collectors.joining("\n"));
    }

    private record StickyLineImpl(
        @Nonnull Document document,
        @Nonnull RangeMarker rangeMarker,
        @Nullable String debugText
    ) implements StickyLine {

        @Override
        public int primaryLine() {
            return document.getLineNumber(rangeMarker.getStartOffset());
        }

        @Override
        public int scopeLine() {
            return document.getLineNumber(rangeMarker.getEndOffset());
        }

        @Override
        public int navigateOffset() {
            return rangeMarker.getStartOffset();
        }

        @Override
        public @Nonnull TextRange textRange() {
            return rangeMarker.getTextRange();
        }

        @Override
        public @Nullable String debugText() {
            return debugText;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof StickyLineImpl impl)) return false;
            return textRange().equals(impl.textRange());
        }

        @Override
        public int hashCode() {
            return textRange().hashCode();
        }

        @Override
        public int compareTo(@Nonnull StickyLine other) {
            TextRange range = textRange();
            TextRange otherRange = other.textRange();
            int compare = Integer.compare(range.getStartOffset(), otherRange.getStartOffset());
            if (compare != 0) {
                return compare;
            }
            // reverse order
            return Integer.compare(otherRange.getEndOffset(), range.getEndOffset());
        }

        @Override
        public @Nonnull String toString() {
            String prefix = debugText == null ? "" : debugText;
            return prefix + "(" + primaryLine() + ", " + scopeLine() + ")";
        }
    }
}
