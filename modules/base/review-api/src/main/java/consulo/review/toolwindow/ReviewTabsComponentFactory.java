// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.toolwindow;

import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Provides UI components for review toolwindow tabs and toolwindow empty state.
 */
public interface ReviewTabsComponentFactory<TVM extends ReviewTabViewModel, PVM extends ReviewToolwindowProjectViewModel<?, TVM>> {
    /**
     * Provides the main / home tab for given {@code projectVm} of the toolwindow
     *
     * @param parentDisposable disposed when context is changed
     */
    @RequiredUIAccess
    Component createReviewListComponent(Disposable parentDisposable, PVM projectVm);

    /**
     * Provides a component for given {@code tabVm} and {@code projectVm}
     *
     * @param parentDisposable disposed when tab is closed or context changed
     */
    @RequiredUIAccess
    Component createTabComponent(Disposable parentDisposable, PVM projectVm, TVM tabVm);

    /**
     * Provides a component that should be shown in toolwindow when there are no {@link ReviewToolwindowProjectViewModel}
     * <p>
     * In most cases, this component should provide a way to log in and select a project
     *
     * @param parentDisposable disposed when {@link ReviewToolwindowProjectViewModel} appears (e.g. user is logged in)
     */
    @RequiredUIAccess
    Component createEmptyTabContent(Disposable parentDisposable);
}
