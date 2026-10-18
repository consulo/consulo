// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.submit;

import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * A view model for a review submission interface
 */
public interface CodeReviewSubmitViewModel {
    /**
     * The number of comments that will be submitted in a review
     */
    ObservableValue<Integer> getDraftCommentsCount();

    /**
     * Body of the review comment
     */
    MutableObservableValue<String> getText();

    /**
     * Review submission in progress
     */
    ObservableValue<Boolean> isBusy();

    /**
     * Review submission error
     */
    ObservableValue<@Nullable Throwable> getError();

    /**
     * Cancel the submission
     */
    void cancel();
}
