// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.toolwindow;

import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

/**
 * Represents view model for review toolwindow that holds selected project VM {@link #getProjectVm()} (for GitHub it is a repository).
 * <p>
 * Clients can provide more specific methods in implementation and acquire the view model using
 * {@link ReviewToolwindowDataKeys#REVIEW_TOOLWINDOW_VM}
 */
public interface ReviewToolwindowViewModel<PVM extends ReviewToolwindowProjectViewModel<?, ?>> {
    ObservableValue<@Nullable PVM> getProjectVm();
}
