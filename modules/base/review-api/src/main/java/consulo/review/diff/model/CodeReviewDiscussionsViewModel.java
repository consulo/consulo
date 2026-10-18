// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.diff.model;

import consulo.review.diff.DiscussionsViewOption;
import consulo.util.concurrent.coroutine.ObservableValue;

/**
 * A view model somehow responsible for review discussions
 */
public interface CodeReviewDiscussionsViewModel {
    ObservableValue<DiscussionsViewOption> getDiscussionsViewOption();

    void setDiscussionsViewOption(DiscussionsViewOption viewOption);
}
