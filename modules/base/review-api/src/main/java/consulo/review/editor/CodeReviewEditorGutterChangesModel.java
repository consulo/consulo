// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.editor;

import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface CodeReviewEditorGutterChangesModel {
    /**
     * Ranges changed in the tracked review
     * These ranges represent changes between file state in review base and the current state of the file (document)
     */
    ObservableValue<@Nullable List<CodeReviewChangedRange>> getReviewRanges();
}
