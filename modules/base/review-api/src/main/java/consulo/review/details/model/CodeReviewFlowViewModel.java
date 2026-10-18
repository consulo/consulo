// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.details.model;

import consulo.review.details.data.ReviewState;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.Map;

public interface CodeReviewFlowViewModel<ReviewerDTO> {
    ObservableValue<Map<ReviewerDTO, ReviewState>> getReviewerReviews();
}
