/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.sandboxPlugin.ide.review;

import consulo.application.util.ListSelection;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.review.diff.DiscussionsViewOption;
import consulo.review.diff.model.CodeReviewDiscussionsViewModel;
import consulo.review.util.ComputedResult;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.change.RefComparisonChange;
import consulo.versionControlSystem.review.diff.model.CodeReviewAsyncDiffViewModelDelegate;
import consulo.versionControlSystem.review.diff.model.CodeReviewDiffProcessorState;
import consulo.versionControlSystem.review.diff.model.CodeReviewDiffProcessorViewModel;
import consulo.versionControlSystem.review.diff.model.DiffViewerScrollRequest;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class SandReviewDiffProcessorViewModel implements CodeReviewDiffProcessorViewModel<SandReviewDiffChangeViewModel>,
    CodeReviewDiscussionsViewModel {
    public static final Key<SandReviewDiffProcessorViewModel> KEY = Key.create("Sand.Review.Diff.ViewModel");

    private final CodeReviewAsyncDiffViewModelDelegate<RefComparisonChange> myDelegate = CodeReviewAsyncDiffViewModelDelegate.create();
    private final ObservableValue<@Nullable ComputedResult<CodeReviewDiffProcessorState<SandReviewDiffChangeViewModel>>> myChanges;
    private final SandReviewInEditorViewModel myInEditorVm = new SandReviewInEditorViewModel();

    public SandReviewDiffProcessorViewModel(Project project, SandReviewService service, Disposable parentDisposable) {
        myChanges = CodeReviewAsyncDiffViewModelDelegate.mapChangesToVms(parentDisposable,
            myDelegate.getChangesToShow(),
            (scope, change) -> new SandReviewDiffChangeViewModel(project, service.getScheduler(), change));
    }

    public SandReviewInEditorViewModel getInEditorVm() {
        return myInEditorVm;
    }

    public void showChanges(ListSelection<RefComparisonChange> changes) {
        myDelegate.showChanges(changes);
    }

    @Override
    public ObservableValue<@Nullable ComputedResult<CodeReviewDiffProcessorState<SandReviewDiffChangeViewModel>>> getChanges() {
        return myChanges;
    }

    @Override
    public void showChange(SandReviewDiffChangeViewModel change, @Nullable DiffViewerScrollRequest scrollRequest) {
        myDelegate.showChange(change.getChange(), scrollRequest);
    }

    @Override
    public void showChange(int changeIdx, @Nullable DiffViewerScrollRequest scrollRequest) {
        List<RefComparisonChange> changes = myDelegate.getChangesToShow().get().getSelectedChanges().getList();
        if (changeIdx >= 0 && changeIdx < changes.size()) {
            myDelegate.showChange(changes.get(changeIdx), scrollRequest);
        }
    }

    @Override
    public ObservableValue<DiscussionsViewOption> getDiscussionsViewOption() {
        return myInEditorVm.getDiscussionsViewOption();
    }

    @Override
    public void setDiscussionsViewOption(DiscussionsViewOption viewOption) {
        myInEditorVm.setDiscussionsViewOption(viewOption);
    }
}
