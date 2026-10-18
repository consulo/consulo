// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.editor;

import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentListener;
import consulo.logging.Logger;
import consulo.review.editor.CodeReviewChangedRange;
import consulo.review.editor.CodeReviewEditorGutterActionableChangesModel;
import consulo.review.editor.CodeReviewEditorGutterChangesModel;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Marks the lines a review changed in the gutter, in one colour for every kind of review change, in the strip the
 * version control marks its own changes in. Each changed range is a {@code FillColorLineMarkerPresentation} every
 * frontend draws, and a deletion - which covers no line - is not marked.
 */
public final class CodeReviewEditorGutterChangesRenderer {
    private static final Logger LOG = Logger.getInstance(CodeReviewEditorGutterChangesRenderer.class);

    private CodeReviewEditorGutterChangesRenderer() {
    }

    @RequiredUIAccess
    public static void render(Disposable parentDisposable, Editor editor, CodeReviewEditorGutterChangesModel model) {
        List<RangeHighlighter> highlighters = new ArrayList<>();
        AtomicReference<@Nullable List<CodeReviewChangedRange>> lastRejectedRanges = new AtomicReference<>();

        Runnable update = () -> {
            for (RangeHighlighter highlighter : highlighters) {
                highlighter.dispose();
            }
            highlighters.clear();

            if (editor.isDisposed()) {
                return;
            }

            Document document = editor.getDocument();
            List<CodeReviewChangedRange> ranges = getRanges(model, document, lastRejectedRanges);
            if (ranges == null) {
                return;
            }

            for (CodeReviewChangedRange range : ranges) {
                if (range.isDeletion()) {
                    continue;
                }

                RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(document.getLineStartOffset(range.line1()),
                    document.getLineEndOffset(range.line2() - 1),
                    HighlighterLayer.FIRST - 1,
                    null,
                    HighlighterTargetArea.LINES_IN_RANGE);
                highlighter.setLineMarkerPresentationProvider(new CodeReviewChangesPresentationProvider(model, range));
                highlighters.add(highlighter);
            }
        };

        Disposer.register(parentDisposable, () -> {
            for (RangeHighlighter highlighter : highlighters) {
                highlighter.dispose();
            }
            highlighters.clear();
        });

        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void bulkUpdateStarting(Document document) {
                update.run();
            }

            @Override
            public void bulkUpdateFinished(Document document) {
                update.run();
            }
        }, parentDisposable);

        ReviewUIUtil.bind(parentDisposable, model.getReviewRanges(), ranges -> update.run());

        if (model instanceof CodeReviewEditorGutterActionableChangesModel actionable) {
            editor.putUserData(CodeReviewEditorGutterActionableChangesModel.KEY, actionable);
            Disposer.register(parentDisposable, () -> editor.putUserData(CodeReviewEditorGutterActionableChangesModel.KEY, null));
        }
    }

    private static @Nullable List<CodeReviewChangedRange> getRanges(CodeReviewEditorGutterChangesModel model,
                                                                     Document document,
                                                                     AtomicReference<@Nullable List<CodeReviewChangedRange>> lastRejectedRanges) {
        if (document.isInBulkUpdate()) {
            return null;
        }
        List<CodeReviewChangedRange> ranges = model.getReviewRanges().get();
        if (ranges == null) {
            return null;
        }
        int lineCount = Math.max(document.getLineCount(), 1);
        for (CodeReviewChangedRange range : ranges) {
            if (range.line1() < 0 || range.line1() > range.line2() || range.line2() > lineCount) {
                if (ranges != lastRejectedRanges.get()) {
                    lastRejectedRanges.set(ranges);
                    LOG.warn("Review ranges are hidden, because a range is outside the document: " + range + ", lineCount=" + lineCount +
                        ", rangesCount=" + ranges.size());
                }
                return null;
            }
        }
        return ranges;
    }
}
