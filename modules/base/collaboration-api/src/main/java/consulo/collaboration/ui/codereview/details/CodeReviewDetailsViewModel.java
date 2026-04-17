// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.ui.codereview.details;

import consulo.collaboration.ui.codereview.details.data.ReviewRequestState;
import consulo.collaboration.util.ReadOnlyObservableValue;
import org.jspecify.annotations.Nullable;

public interface CodeReviewDetailsViewModel {
    String getNumber();

    String getUrl();

    ReadOnlyObservableValue<String> getTitle();

    @Nullable ReadOnlyObservableValue<String> getDescription();

    ReadOnlyObservableValue<ReviewRequestState> getReviewRequestState();
}
