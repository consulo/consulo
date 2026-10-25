// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.diff;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.diff.DiffEditorViewer;
import consulo.diff.combined.CombinedDiffComponentProcessor;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.review.util.KeyValuePair;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.PresentableChange;
import consulo.versionControlSystem.review.diff.model.AsyncDiffViewModel;
import consulo.versionControlSystem.review.diff.model.CodeReviewDiffProcessorViewModel;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

@ServiceAPI(ComponentScope.APPLICATION)
public interface AsyncDiffRequestProcessorFactory {
    /**
     * A diff viewer showing the changes of the view model {@code diffVm} holds, switching between them with the
     * view model; it follows {@code diffVm} until the viewer or {@code parentDisposable} is disposed.
     *
     * @param createContext   the data the diff context gets while a view model is shown
     * @param changePresenter the file path and status of a change
     */
    @RequiredUIAccess
    <VM extends CodeReviewDiffProcessorViewModel<C>, C extends AsyncDiffViewModel> DiffEditorViewer createIn(
        Disposable parentDisposable,
        Project project,
        ObservableValue<@Nullable VM> diffVm,
        Function<VM, List<KeyValuePair<?>>> createContext,
        Function<C, PresentableChange> changePresenter
    );

    /**
     * An all-in-one diff viewer showing every change of the view model {@code reviewDiffVm} holds in a single scrollable view;
     * it follows {@code reviewDiffVm} until the viewer or {@code parentDisposable} is disposed.
     *
     * @param createContext     the data the diff context gets while a view model is shown
     * @param changeVmPresenter the file path and status of a change
     */
    @RequiredUIAccess
    <VM extends CodeReviewDiffProcessorViewModel<C>, C extends AsyncDiffViewModel> CombinedDiffComponentProcessor createCombinedIn(
        Disposable parentDisposable,
        Project project,
        ObservableValue<@Nullable VM> reviewDiffVm,
        Function<VM, List<KeyValuePair<?>>> createContext,
        Function<C, PresentableChange> changeVmPresenter
    );
}
