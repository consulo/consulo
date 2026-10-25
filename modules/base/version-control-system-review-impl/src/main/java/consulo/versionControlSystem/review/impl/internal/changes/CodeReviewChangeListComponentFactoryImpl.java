// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.review.impl.internal.changes;

import consulo.annotation.component.ServiceImpl;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.review.icon.ReviewIconGroup;
import consulo.ui.Component;
import consulo.ui.TextItemPresentation;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.RefComparisonChange;
import consulo.versionControlSystem.review.CodeReviewProgressTreeModel;
import consulo.versionControlSystem.review.NodeCodeReviewProgressState;
import consulo.versionControlSystem.review.changes.CodeReviewChangeListComponentFactory;
import consulo.versionControlSystem.review.changes.CodeReviewChangeListDataKeys;
import consulo.versionControlSystem.review.details.model.AllChangesSelectionRequest;
import consulo.versionControlSystem.review.details.model.CodeReviewChangeListSelectionRequest;
import consulo.versionControlSystem.review.details.model.CodeReviewChangeListViewModel;
import consulo.versionControlSystem.review.details.model.CodeReviewGroupingChangeListViewModel;
import consulo.versionControlSystem.review.details.model.OneChangeSelectionRequest;
import consulo.versionControlSystem.review.util.ChangesSelection;
import consulo.versionControlSystem.review.util.FuzzyChangesSelection;
import consulo.versionControlSystem.review.util.PreciseChangesSelection;
import consulo.versionControlSystem.ui.VcsChangesBrowser;
import consulo.versionControlSystem.ui.VcsChangesBrowserBuilder;
import consulo.versionControlSystem.ui.VcsChangesDialogFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
@ServiceImpl
public class CodeReviewChangeListComponentFactoryImpl implements CodeReviewChangeListComponentFactory {
    private final VcsChangesDialogFactory myVcsChangesDialogFactory;

    @Inject
    public CodeReviewChangeListComponentFactoryImpl(VcsChangesDialogFactory vcsChangesDialogFactory) {
        myVcsChangesDialogFactory = vcsChangesDialogFactory;
    }

    @RequiredUIAccess
    @Override
    public Component createIn(Disposable parentDisposable,
                              CodeReviewChangeListViewModel vm,
                              @Nullable CodeReviewProgressTreeModel<?> progressModel,
                              LocalizeValue emptyTextText) {
        Map<Change, RefComparisonChange> refByChange = new LinkedHashMap<>();
        Map<RefComparisonChange, Change> changeByRef = new HashMap<>();
        for (RefComparisonChange ref : vm.getChanges()) {
            Change change = toChange(ref);
            refByChange.put(change, ref);
            changeByRef.put(ref, change);
        }

        AtomicReference<@Nullable VcsChangesBrowser> browserRef = new AtomicReference<>();
        VcsChangesBrowserBuilder builder = myVcsChangesDialogFactory.changesBrowser(vm.getProject())
            .emptyText(emptyTextText)
            .openHandler(change -> {
                VcsChangesBrowser browser = browserRef.get();
                if (browser != null) {
                    updateSelectedChangesFromTree(vm, browser, refByChange);
                }
                vm.showDiffPreview();
            });
        if (vm instanceof CodeReviewGroupingChangeListViewModel groupingVm) {
            builder.grouping(groupingVm.getGrouping().get(), groupingVm::setGrouping);
        }
        if (progressModel != null) {
            builder.changeDecorator((change, presentation) -> {
                RefComparisonChange ref = refByChange.get(change);
                if (ref != null) {
                    decorate(presentation, progressModel.getState(ref));
                }
            });
        }

        VcsChangesBrowser browser = builder.build(parentDisposable);
        browserRef.set(browser);
        browser.setChanges(List.copyOf(refByChange.keySet()));

        browser.addSelectionListener(parentDisposable, () -> updateSelectedChangesFromTree(vm, browser, refByChange));

        UIAccess uiAccess = UIAccess.current();
        vm.addSelectionRequestListener(parentDisposable, request ->
            uiAccess.give(() -> handleSelectionRequest(browser, request, refByChange, changeByRef)));

        if (progressModel != null) {
            progressModel.addChangeListener(parentDisposable, () -> uiAccess.give(() -> browser.setChanges(browser.getChanges())));
        }

        DockLayout root = DockLayout.create();
        root.center(browser.getComponent());
        root.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(CodeReviewChangeListDataKeys.SELECTED_CHANGES, toRefs(browser.getSelectedChanges(), refByChange));
            sink.set(CodeReviewChangeListViewModel.DATA_KEY, vm);
        });
        return root;
    }

    @RequiredUIAccess
    private static void handleSelectionRequest(VcsChangesBrowser browser,
                                               CodeReviewChangeListSelectionRequest request,
                                               Map<Change, RefComparisonChange> refByChange,
                                               Map<RefComparisonChange, Change> changeByRef) {
        if (request instanceof AllChangesSelectionRequest) {
            browser.selectFirst();
        }
        else if (request instanceof OneChangeSelectionRequest oneChange) {
            List<RefComparisonChange> currentSelection = toRefs(browser.getSelectedChanges(), refByChange);
            Change change = changeByRef.get(oneChange.change());
            if (change != null && !currentSelection.contains(oneChange.change())) {
                browser.selectChanges(List.of(change));
            }
        }
    }

    private static void updateSelectedChangesFromTree(CodeReviewChangeListViewModel vm,
                                                      VcsChangesBrowser browser,
                                                      Map<Change, RefComparisonChange> refByChange) {
        boolean fuzzy = browser.isGroupSelected();
        List<RefComparisonChange> selectedChanges = toRefs(browser.getSelectedChanges(), refByChange);
        ChangesSelection selection;
        if (selectedChanges.isEmpty()) {
            selection = null;
        }
        else if (fuzzy) {
            selection = new FuzzyChangesSelection(selectedChanges);
        }
        else if (selectedChanges.size() == 1) {
            selection = new PreciseChangesSelection(vm.getChanges(), selectedChanges.get(0));
        }
        else {
            selection = new PreciseChangesSelection(selectedChanges, selectedChanges.get(0));
        }
        vm.updateSelectedChanges(selection);
    }

    private static List<RefComparisonChange> toRefs(List<Change> changes, Map<Change, RefComparisonChange> refByChange) {
        List<RefComparisonChange> result = new ArrayList<>(changes.size());
        for (Change change : changes) {
            RefComparisonChange ref = refByChange.get(change);
            if (ref != null) {
                result.add(ref);
            }
        }
        return result;
    }

    private static void decorate(TextItemPresentation presentation, NodeCodeReviewProgressState state) {
        // if loading, don't show any icons yet
        if (state.isLoading()) {
            return;
        }
        if (state.discussionsCount() > 0) {
            presentation.withSuffix(LocalizeValue.of(String.valueOf(state.discussionsCount())),
                state.isRead() ? ReviewIconGroup.comment() : ReviewIconGroup.reviewCommentunread());
        }
        else if (!state.isRead()) {
            presentation.withSuffix(LocalizeValue.empty(), ReviewIconGroup.reviewFileunread());
        }
    }

    private static Change toChange(RefComparisonChange ref) {
        FilePath before = ref.filePathBefore();
        FilePath after = ref.filePathAfter();
        return new Change(before == null ? null : new RefComparisonContentRevision(before, ref.revisionNumberBefore()),
            after == null ? null : new RefComparisonContentRevision(after, ref.revisionNumberAfter()));
    }
}
