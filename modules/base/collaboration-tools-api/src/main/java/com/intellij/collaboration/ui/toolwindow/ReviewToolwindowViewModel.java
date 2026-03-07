// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.toolwindow;

import kotlinx.coroutines.flow.StateFlow;
import jakarta.annotation.Nonnull;

/**
 * Represents view model for review toolwindow that holds selected project VM {@link #getProjectVm()} (for GitHub it is a repository).
 * <p>
 * Clients can provide more specific methods in implementation and acquire the view model using {@link ReviewToolwindowDataKeys#REVIEW_TOOLWINDOW_VM}
 */
public interface ReviewToolwindowViewModel<PVM extends ReviewToolwindowProjectViewModel<?, ?>> {
    @Nonnull
    StateFlow<PVM> getProjectVm();
}
