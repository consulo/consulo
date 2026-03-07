// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import jakarta.annotation.Nullable;
import kotlinx.coroutines.flow.StateFlow;

import java.util.List;

/**
 * A UI model for an editor with gutter changes highlighting
 * This model should exist in the same scope as the editor
 * One model - one editor
 */
public interface CodeReviewEditorGutterChangesModel extends LineStatusMarkerRangesSource<LstRange> {
    /**
     * Ranges changed in the tracked review
     * These ranges represent changes between file state in review base and the current state of the file (document)
     */
    StateFlow<List<LstRange>> getReviewRanges();

    @Override
    default @Nullable List<LstRange> getRanges() {
        if (!isValid()) {
            return null;
        }
        return getReviewRanges().getValue();
    }

    @Override
    default @Nullable LstRange findRange(LstRange range) {
        List<LstRange> ranges = getRanges();
        if (ranges == null) {
            return null;
        }
        for (LstRange r : ranges) {
            if (r.getVcsLine1() == range.getVcsLine1() && r.getVcsLine2() == range.getVcsLine2() &&
                r.getLine1() == range.getLine1() && r.getLine2() == range.getLine2()) {
                return r;
            }
        }
        return null;
    }
}
