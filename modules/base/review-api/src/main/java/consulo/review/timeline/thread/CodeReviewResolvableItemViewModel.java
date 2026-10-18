// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.timeline.thread;

import consulo.util.concurrent.coroutine.ObservableValue;

public interface CodeReviewResolvableItemViewModel {
    ObservableValue<Boolean> isResolved();

    ObservableValue<Boolean> getCanChangeResolvedState();

    ObservableValue<Boolean> isBusy();

    void changeResolvedState();
}
