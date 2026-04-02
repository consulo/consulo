// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.editor;

import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nullable;

import java.util.List;

@ApiStatus.Internal
public class MutableCodeReviewEditorGutterChangesModel implements CodeReviewEditorGutterChangesModel {
    private final MutableStateFlow<List<LstRange>> _reviewRanges = kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow(null);

    @Override
    public StateFlow<List<LstRange>> getReviewRanges() {
        return FlowKt.asStateFlow(_reviewRanges);
    }

    @Override
    public boolean isValid() {
        return _reviewRanges.getValue() != null;
    }

    @Override
    public @Nullable List<LstRange> getRanges() {
        return getReviewRanges().getValue();
    }

    @Override
    public @Nullable LstRange findRange(LstRange range) {
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

    public void setChanges(@Nullable List<LstRange> changedRanges) {
        _reviewRanges.setValue(changedRanges);
    }
}
