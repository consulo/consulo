// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff.model;

import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * A viewmodel for a diff processor which can show multiple diffs and switch between them
 */
public interface CodeReviewDiffProcessorViewModel<C> {
    ObservableValue<@Nullable ComputedResult<CodeReviewDiffProcessorState<C>>> getChanges();

    void showChange(C change, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChange(C change) {
        showChange(change, null);
    }

    void showChange(int changeIdx, @Nullable DiffViewerScrollRequest scrollRequest);

    default void showChange(int changeIdx) {
        showChange(changeIdx, null);
    }
}
