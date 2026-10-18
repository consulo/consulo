// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.details.model;

import consulo.review.details.data.ReviewRequestState;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

public interface CodeReviewDetailsViewModel {
    String getNumber();

    String getUrl();

    ObservableValue<String> getTitle();

    @Nullable
    ObservableValue<String> getDescription();

    ObservableValue<ReviewRequestState> getReviewRequestState();
}
