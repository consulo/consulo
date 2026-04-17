// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.ui.codereview.timeline.thread;

import consulo.collaboration.util.ReadOnlyObservableValue;

/**
 * View model for a resolvable review item (thread, discussion).
 */
public interface CodeReviewResolvableItemViewModel {
    ReadOnlyObservableValue<Boolean> getIsResolved();

    ReadOnlyObservableValue<Boolean> getCanChangeResolvedState();

    ReadOnlyObservableValue<Boolean> getIsBusy();

    void changeResolvedState();
}
