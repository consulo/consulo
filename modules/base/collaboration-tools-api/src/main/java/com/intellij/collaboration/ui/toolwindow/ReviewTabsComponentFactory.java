// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.toolwindow;

import kotlinx.coroutines.CoroutineScope;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * Provides UI components for review toolwindow tabs and toolwindow empty state.
 */
public interface ReviewTabsComponentFactory<TVM extends ReviewTabViewModel, PVM extends ReviewToolwindowProjectViewModel<?, TVM>> {
    /**
     * Provides the main / home tab for given {@code projectVm} of the toolwindow
     *
     * @param cs scope that closes when context is changed
     */
    // TODO: to be renamed to createHomeTabComponent or refactored to get rid of, TBD
    @Nonnull
    JComponent createReviewListComponent(@Nonnull CoroutineScope cs, @Nonnull PVM projectVm);

    /**
     * Provides a component for given {@code tabVm} and {@code projectVm}
     *
     * @param cs scope that closes when tab is closed or context changed
     */
    @Nonnull
    JComponent createTabComponent(@Nonnull CoroutineScope cs, @Nonnull PVM projectVm, @Nonnull TVM tabVm);

    /**
     * Provides a component that should be shown in toolwindow when there are no {@link ReviewToolwindowProjectViewModel}
     * <p>
     * In most cases, this component should provide a way to log in and select a project
     *
     * @param cs scope that closes when {@link ReviewToolwindowProjectViewModel} appears (e.g. user is logged in)
     */
    @Nonnull
    JComponent createEmptyTabContent(@Nonnull CoroutineScope cs);
}
