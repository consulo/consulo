// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.details.model;

import consulo.disposer.Disposable;
import consulo.review.details.data.CodeReviewCIJob;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public interface CodeReviewStatusViewModel {
    /**
     * Whether there are conflicts that need to be resolved before merging.
     * <p>
     * If the value is {@code null}, there is a check currently in progress or there is something
     * else preventing us from knowing whether there are conflicts.
     */
    ObservableValue<@Nullable Boolean> getHasConflicts();

    ObservableValue<List<CodeReviewCIJob>> getCiJobs();

    void addShowJobsDetailsRequestListener(Disposable parentDisposable, Consumer<List<CodeReviewCIJob>> listener);

    void showJobsDetails();
}
