// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.review.impl.internal.editor;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.EditorFactory;
import consulo.diff.util.Range;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.review.editor.CodeReviewChangedRange;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.impl.internal.DocumentTracker;
import consulo.versionControlSystem.review.editor.CodeReviewEditorDocumentTracker;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@Singleton
@ServiceImpl
public class CodeReviewEditorDocumentTrackerImpl implements CodeReviewEditorDocumentTracker {
    @RequiredUIAccess
    @Override
    public void trackDocumentDiff(Disposable parentDisposable,
                                  CharSequence originalContent,
                                  Document document,
                                  Consumer<List<CodeReviewChangedRange>> changesCollector) {
        Document reviewHeadDocument = EditorFactory.getInstance().createDocument(originalContent);
        reviewHeadDocument.setReadOnly(true);
        trackDocumentDiff(parentDisposable, reviewHeadDocument, document, changesCollector);
    }

    @RequiredUIAccess
    @Override
    public void trackDocumentDiff(Disposable parentDisposable,
                                  Document originalDocument,
                                  Document currentDocument,
                                  Consumer<List<CodeReviewChangedRange>> changesCollector) {
        DocumentTracker documentTracker = new DocumentTracker(originalDocument, currentDocument);
        DocumentTracker.Handler trackerHandler = new DocumentTracker.Handler() {
            @Override
            public void afterBulkRangeChange(boolean isDirty) {
                List<CodeReviewChangedRange> trackerRanges = new ArrayList<>();
                for (DocumentTracker.Block block : documentTracker.getBlocks()) {
                    Range range = block.getRange();
                    trackerRanges.add(new CodeReviewChangedRange(range.start2(), range.end2(), range.start1(), range.end1()));
                }
                changesCollector.accept(trackerRanges);
            }
        };

        Disposer.register(parentDisposable, documentTracker);
        documentTracker.addHandler(trackerHandler);
        trackerHandler.afterBulkRangeChange(true);
    }
}
