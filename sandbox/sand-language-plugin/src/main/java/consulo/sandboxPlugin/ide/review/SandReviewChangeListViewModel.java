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

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.change.RefComparisonChange;
import consulo.versionControlSystem.review.details.model.CodeReviewChangeDetails;
import consulo.versionControlSystem.review.details.model.CodeReviewChangeList;
import consulo.versionControlSystem.review.details.model.CodeReviewChangeListViewModelBase;
import consulo.versionControlSystem.review.details.model.CodeReviewViewedStateChangeListViewModel;
import consulo.versionControlSystem.review.util.ChangesSelection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewChangeListViewModel extends CodeReviewChangeListViewModelBase implements CodeReviewViewedStateChangeListViewModel {
    private final Project myProject;
    private final MutableObservableValue<Map<RefComparisonChange, CodeReviewChangeDetails>> myDetails;

    public SandReviewChangeListViewModel(Project project,
                                         Disposable parentCs,
                                         CodeReviewChangeList changeList,
                                         Map<RefComparisonChange, CodeReviewChangeDetails> details) {
        super(parentCs, changeList);
        myProject = project;
        myDetails = MutableObservableValue.of(Map.copyOf(details));
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public ObservableValue<Map<RefComparisonChange, CodeReviewChangeDetails>> getDetailsByChange() {
        return myDetails;
    }

    @RequiredUIAccess
    @Override
    public void setViewedState(Iterable<RefComparisonChange> changes, boolean viewed) {
        Map<RefComparisonChange, CodeReviewChangeDetails> details = new HashMap<>(myDetails.get());
        for (RefComparisonChange change : changes) {
            CodeReviewChangeDetails current = details.get(change);
            details.put(change, new CodeReviewChangeDetails(viewed, current == null ? 0 : current.discussions()));
        }
        myDetails.set(Map.copyOf(details));
    }

    @RequiredUIAccess
    @Override
    public void showDiffPreview() {
        ChangesSelection selection = getChangesSelection().get();
        RefComparisonChange change = selection == null ? null : selection.getSelectedChange();
        if (change != null && canEdit(change)) {
            startEditing(change);
            setViewedState(List.of(change), true);
        }
    }

    @RequiredUIAccess
    @Override
    public void showDiff() {
        showDiffPreview();
    }
}
