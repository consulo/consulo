// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MutableCodeReviewEditorGutterChangesModel implements CodeReviewEditorGutterChangesModel {
    private final MutableObservableValue<@Nullable List<CodeReviewChangedRange>> myReviewRanges = MutableObservableValue.of(null);

    @Override
    public ObservableValue<@Nullable List<CodeReviewChangedRange>> getReviewRanges() {
        return myReviewRanges;
    }

    public boolean isValid() {
        return myReviewRanges.get() != null;
    }

    public @Nullable List<CodeReviewChangedRange> getRanges() {
        return myReviewRanges.get();
    }

    public @Nullable CodeReviewChangedRange findRange(CodeReviewChangedRange range) {
        List<CodeReviewChangedRange> ranges = getRanges();
        if (ranges == null) {
            return null;
        }
        for (CodeReviewChangedRange candidate : ranges) {
            if (candidate.baseLine1() == range.baseLine1() && candidate.baseLine2() == range.baseLine2() &&
                candidate.line1() == range.line1() && candidate.line2() == range.line2()) {
                return candidate;
            }
        }
        return null;
    }

    public void setChanges(@Nullable List<CodeReviewChangedRange> changedRanges) {
        myReviewRanges.set(changedRanges);
    }
}
