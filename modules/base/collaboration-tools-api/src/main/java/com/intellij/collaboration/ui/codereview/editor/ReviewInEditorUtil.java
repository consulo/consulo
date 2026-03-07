// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import com.intellij.collaboration.ui.codereview.editor.action.CodeReviewInEditorToolbarActionGroup;
import consulo.codeEditor.Editor;
import consulo.diff.util.Range;
import consulo.document.Document;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.AnAction;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.Dispatchers;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ReviewInEditorUtil {
    public static final JBColor REVIEW_CHANGES_STATUS_COLOR =
        JBColor.namedColor("Review.Editor.Line.Status.Marker", new JBColor(0xF8A0DF, 0x8A4175));

    public static final LineStatusMarkerColorScheme REVIEW_STATUS_MARKER_COLOR_SCHEME =
        new LineStatusMarkerColorScheme() {
            @Override
            public @Nonnull Color getColor(@Nonnull Editor editor, byte type) {
                return REVIEW_CHANGES_STATUS_COLOR;
            }

            @Override
            public @Nonnull Color getIgnoredBorderColor(@Nonnull Editor editor, byte type) {
                return REVIEW_CHANGES_STATUS_COLOR;
            }

            @Override
            public @Nonnull Color getErrorStripeColor(byte type) {
                return REVIEW_CHANGES_STATUS_COLOR;
            }
        };

    private ReviewInEditorUtil() {
    }

    public static int transferLineToAfter(@Nonnull List<Range> ranges, int line) {
        if (ranges.isEmpty()) {
            return line;
        }
        int result = line;
        for (Range range : ranges) {
            if (line >= range.start1 && line < range.end1) {
                return Math.max(range.end2 - 1, 0);
            }

            if (range.end1 > line) {
                return result;
            }

            int length1 = range.end1 - range.start1;
            int length2 = range.end2 - range.start2;
            result += length2 - length1;
        }
        return result;
    }

    public static @Nullable Integer transferLineFromAfter(@Nonnull List<Range> ranges, int line) {
        return transferLineFromAfter(ranges, line, false);
    }

    public static @Nullable Integer transferLineFromAfter(@Nonnull List<Range> ranges, int line, boolean approximate) {
        if (ranges.isEmpty()) {
            return line;
        }
        int result = line;
        for (Range range : ranges) {
            if (line < range.start2) {
                return result;
            }

            if (line >= range.start2 && line < range.end2) {
                return approximate ? range.end1 : null;
            }

            int length1 = range.end1 - range.start1;
            int length2 = range.end2 - range.start2;
            result -= length2 - length1;
        }
        return result;
    }

    /**
     * Suspending method to track document diff sync with original content as CharSequence.
     */
    @SuppressWarnings("unused")
    public static @Nullable Object trackDocumentDiffSync(
        @Nonnull CharSequence originalContent,
        @Nonnull Document document,
        @Nonnull Consumer<List<Range>> changesCollector,
        @Nonnull Continuation<? super kotlin.Nothing> continuation
    ) {
        Document reviewHeadDocument = LineStatusTrackerBase.createVcsDocument(originalContent);
        return trackDocumentDiffSync(reviewHeadDocument, document, changesCollector, continuation);
    }

    /**
     * Suspending method to track document diff sync.
     */
    @SuppressWarnings("unused")
    public static @Nullable Object trackDocumentDiffSync(
        @Nonnull Document originalDocument,
        @Nonnull Document currentDocument,
        @Nonnull Consumer<List<Range>> changesCollector,
        @Nonnull Continuation<? super kotlin.Nothing> continuation
    ) {
        // This is a suspend function that needs to be called from Kotlin coroutine context.
        // The implementation creates a DocumentTracker and monitors changes.
        return kotlinx.coroutines.BuildersKt.withContext(
            Dispatchers.getMain().immediate(),
            (scope, cont) -> {
                DocumentTracker documentTracker = new DocumentTracker(originalDocument, currentDocument);
                DocumentTracker.Handler trackerHandler = new DocumentTracker.Handler() {
                    @Override
                    public void afterBulkRangeChange(boolean isDirty) {
                        List<Range> trackerRanges = documentTracker.getBlocks().stream()
                            .map(block -> block.getRange())
                            .toList();
                        changesCollector.accept(trackerRanges);
                    }
                };

                try {
                    documentTracker.addHandler(trackerHandler);
                    trackerHandler.afterBulkRangeChange(true);
                    return kotlinx.coroutines.CompletableDeferredKt.awaitCancellation(cont);
                }
                finally {
                    Disposer.dispose(documentTracker);
                }
            },
            continuation
        );
    }

    /**
     * Sets up an inspection widget action group for review in editor.
     * Suspends until canceled.
     */
    @SuppressWarnings("unused")
    public static @Nullable Object showReviewToolbar(
        @Nonnull CodeReviewInEditorViewModel vm,
        @Nonnull Editor editor,
        @Nonnull Continuation<? super kotlin.Nothing> continuation
    ) {
        return showReviewToolbarWithActions(vm, editor, new AnAction[0], continuation);
    }

    @SuppressWarnings("unused")
    public static @Nullable Object showReviewToolbarWithActions(
        @Nonnull CodeReviewInEditorViewModel vm,
        @Nonnull Editor editor,
        @Nonnull AnAction[] additionalActions,
        @Nonnull Continuation<? super kotlin.Nothing> continuation
    ) {
        // Builds toolbar action group and shows inspection widget action.
        // This is a suspend function.
        return null; // Placeholder - actual suspend implementation is in Kotlin bridge
    }

    @ApiStatus.Internal
    @SuppressWarnings("unused")
    public static @Nullable Object showReviewToolbarWithWarning(
        @Nonnull CodeReviewInEditorViewModel vm,
        @Nonnull Editor editor,
        @Nonnull AnAction[] additionalActions,
        @Nonnull Supplier<@Nls String> warningSupplier,
        @Nonnull Continuation<? super kotlin.Nothing> continuation
    ) {
        // Builds toolbar action group with warning and shows inspection widget action.
        // This is a suspend function.
        return null; // Placeholder - actual suspend implementation is in Kotlin bridge
    }

    /**
     * Converts a diff Range to an LstRange.
     */
    public static @Nonnull LstRange asLst(@Nonnull Range range) {
        return new LstRange(range.start2, range.end2, range.start1, range.end1);
    }
}
