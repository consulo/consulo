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
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.review.details.CodeReviewDetailsActionsComponentFactory;
import consulo.review.details.CodeReviewDetailsComponentFactory;
import consulo.review.details.CodeReviewDetailsStatusComponentFactory;
import consulo.review.details.data.ReviewState;
import consulo.review.localize.ReviewLocalize;
import consulo.review.list.ReviewListComponentFactory;
import consulo.review.timeline.CodeReviewTimelineComponentFactory;
import consulo.review.toolwindow.ReviewTabsComponentFactory;
import consulo.review.util.ComputedResult;
import consulo.review.util.ReviewAction;
import consulo.review.util.ReviewUIUtil;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.versionControlSystem.review.CodeReviewProgressTreeModelFromDetails;
import consulo.versionControlSystem.review.changes.CodeReviewChangeListComponentFactory;
import consulo.versionControlSystem.review.details.CodeReviewDetailsBranchComponentFactory;
import consulo.versionControlSystem.review.details.CodeReviewDetailsCommitInfoComponentFactory;
import consulo.versionControlSystem.review.details.CodeReviewDetailsCommitsComponentFactory;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * The sand review tool window built only from the review API, the way a review plugin builds its own.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public class SandReviewTabsComponentFactory implements ReviewTabsComponentFactory<SandReviewTabViewModel, SandReviewProjectViewModel> {
    private final Project myProject;
    private final ReviewListComponentFactory myListComponentFactory;
    private final CodeReviewDetailsComponentFactory myDetailsComponentFactory;
    private final CodeReviewTimelineComponentFactory myTimelineComponentFactory;
    private final CodeReviewDetailsCommitsComponentFactory myCommitsComponentFactory;
    private final CodeReviewDetailsCommitInfoComponentFactory myCommitInfoComponentFactory;
    private final CodeReviewDetailsBranchComponentFactory myBranchComponentFactory;
    private final CodeReviewChangeListComponentFactory myChangeListComponentFactory;
    private final CodeReviewDetailsStatusComponentFactory myStatusComponentFactory;
    private final CodeReviewDetailsActionsComponentFactory myActionsComponentFactory;
    private final SandReviewSubmitPopupHandler mySubmitPopupHandler = new SandReviewSubmitPopupHandler();

    public SandReviewTabsComponentFactory(Project project,
                                          ReviewListComponentFactory listComponentFactory,
                                          CodeReviewDetailsComponentFactory detailsComponentFactory,
                                          CodeReviewTimelineComponentFactory timelineComponentFactory,
                                          CodeReviewDetailsCommitsComponentFactory commitsComponentFactory,
                                          CodeReviewDetailsCommitInfoComponentFactory commitInfoComponentFactory,
                                          CodeReviewDetailsBranchComponentFactory branchComponentFactory,
                                          CodeReviewChangeListComponentFactory changeListComponentFactory,
                                          CodeReviewDetailsStatusComponentFactory statusComponentFactory,
                                          CodeReviewDetailsActionsComponentFactory actionsComponentFactory) {
        myProject = project;
        myStatusComponentFactory = statusComponentFactory;
        myActionsComponentFactory = actionsComponentFactory;
        myCommitsComponentFactory = commitsComponentFactory;
        myCommitInfoComponentFactory = commitInfoComponentFactory;
        myBranchComponentFactory = branchComponentFactory;
        myChangeListComponentFactory = changeListComponentFactory;
        myListComponentFactory = listComponentFactory;
        myDetailsComponentFactory = detailsComponentFactory;
        myTimelineComponentFactory = timelineComponentFactory;
    }

    @RequiredUIAccess
    @Override
    public Component createReviewListComponent(Disposable parentDisposable, SandReviewProjectViewModel projectVm) {
        DockLayout root = DockLayout.create();
        root.top(new SandReviewSearchPanelFactory(projectVm.getSearchVm()).create(parentDisposable));
        root.center(createList(parentDisposable, projectVm));
        return root;
    }

    @RequiredUIAccess
    private Component createList(Disposable parentDisposable, SandReviewProjectViewModel projectVm) {
        return myListComponentFactory.create(parentDisposable, projectVm.getProjectName(), projectVm.getListVm(), review -> {
            projectVm.getListVm().open(review);
            SandReviewTabViewModel tabVm = projectVm.getTabs().get().tabs().get(new SandReviewTab(review.getNumber()));
            if (tabVm != null) {
                myTimelineComponentFactory.openTimeline(myProject, tabVm, tabVm.getTimelineVm());
            }
        });
    }

    @RequiredUIAccess
    @Override
    public Component createTabComponent(Disposable parentDisposable, SandReviewProjectViewModel projectVm, SandReviewTabViewModel tabVm) {
        SandReviewViewModel review = tabVm.review();
        SandReviewChangesViewModel changesVm = review.getChangesVm();

        VerticalLayout titleSection = VerticalLayout.create(Space.LARGE);
        titleSection.add(myDetailsComponentFactory.createTitle(parentDisposable, review));
        titleSection.add(myDetailsComponentFactory.createDescription(parentDisposable, review,
            () -> myTimelineComponentFactory.openTimeline(myProject, tabVm, tabVm.getTimelineVm())));
        titleSection.paddingBuilder().topSet(Space.X_LARGE).bottomSet(Space.LARGE).horizontalSet(Space.X_LARGE).apply();

        DockLayout commitsRow = DockLayout.create();
        commitsRow.left(myCommitsComponentFactory.create(parentDisposable, changesVm, SandReviewCommit::toPresentation));
        commitsRow.right(myBranchComponentFactory.create(parentDisposable, review.getBranchesVm()));

        VerticalLayout commitsSection = VerticalLayout.create(Space.NONE);
        commitsSection.add(commitsRow);
        commitsSection.add(myCommitInfoComponentFactory.create(parentDisposable, changesVm.getSelectedCommit(), SandReviewCommit::toPresentation));
        commitsSection.paddingBuilder().horizontalSet(Space.X_LARGE).apply();

        SandReviewFlowViewModel flowVm = review.getFlowVm();

        VerticalLayout statuses = VerticalLayout.create(Space.NONE);
        statuses.add(myStatusComponentFactory.createCiComponent(parentDisposable, review));
        statuses.add(myStatusComponentFactory.createConflictsComponent(parentDisposable,
            review.getHasConflicts(),
            flowVm.getResolveConflictsAction(),
            flowVm.isBusy()));
        statuses.add(myStatusComponentFactory.createRequiredReviewsComponent(parentDisposable,
            flowVm.getRequiredApprovingReviewsCount(),
            flowVm.isDraft()));
        statuses.add(myStatusComponentFactory.createRequiredResolveConversationsComponent(parentDisposable,
            flowVm.getHasUnresolvedConversations()));
        statuses.add(myStatusComponentFactory.createNeedReviewerComponent(parentDisposable, flowVm.getReviewerReviews()));
        statuses.add(myStatusComponentFactory.createReviewersReviewStateComponent(parentDisposable,
            flowVm.getReviewerReviews(),
            SandReviewUser::getPresentableName,
            reviewer -> null,
            (reviewer, reviewState) -> new DefaultActionGroup(DumbAwareAction.create(
                ReviewLocalize.reviewDetailsActionRemoveReviewer(reviewer.getPresentableName()),
                e -> flowVm.removeReviewer(reviewer)))));
        statuses.paddingBuilder().topSet(Space.MEDIUM).bottomSet(Space.LARGE).horizontalSet(Space.X_LARGE).apply();

        Component actions = createActionsComponent(parentDisposable, review, flowVm);

        VerticalLayout header = VerticalLayout.create(Space.NONE);
        header.add(titleSection);
        header.add(commitsSection);
        header.add(statuses);
        header.add(actions);

        DockLayout changesHolder = DockLayout.create();
        AtomicReference<@Nullable Disposable> changesDisposable = new AtomicReference<>();
        Disposer.register(parentDisposable, () -> {
            Disposable previous = changesDisposable.getAndSet(null);
            if (previous != null) {
                Disposer.dispose(previous);
            }
        });
        UIAccess uiAccess = UIAccess.current();
        Consumer<ComputedResult<SandReviewChangeListViewModel>> showChanges = result -> uiAccess.give(() -> {
            Disposable previous = changesDisposable.getAndSet(null);
            if (previous != null) {
                Disposer.dispose(previous);
            }
            SandReviewChangeListViewModel changeListVm = result.getOrNull();
            if (changeListVm == null) {
                return;
            }
            Disposable disposable = Disposable.newDisposable("Sand review changes");
            Disposer.register(parentDisposable, disposable);
            changesDisposable.set(disposable);
            changesHolder.center(myChangeListComponentFactory.createIn(disposable,
                changeListVm,
                new CodeReviewProgressTreeModelFromDetails(disposable, changeListVm),
                LocalizeValue.localizeTODO("No changes")));
        });
        showChanges.accept(changesVm.getChangeListVm().get());
        Runnable unsubscribe = changesVm.getChangeListVm().addListener(showChanges);
        Disposer.register(parentDisposable, unsubscribe::run);

        DockLayout root = DockLayout.create();
        root.top(header);
        root.center(changesHolder);
        return root;
    }

    @RequiredUIAccess
    @Override
    public Component createEmptyTabContent(Disposable parentDisposable) {
        return Label.create(LocalizeValue.localizeTODO("Not connected to the sand review service"));
    }

    @RequiredUIAccess
    private Component createActionsComponent(Disposable parentDisposable, SandReviewViewModel review, SandReviewFlowViewModel flowVm) {
        ReviewAction requestReviewAction = ReviewAction.create(ReviewLocalize.reviewDetailsActionRequest(), e -> flowVm.requestReview());
        ReviewAction reRequestReviewAction = ReviewAction.create(ReviewLocalize.reviewDetailsActionRerequest(), e -> flowVm.reRequestReview());
        ReviewAction setMyselfAsReviewerAction = ReviewAction.create(ReviewLocalize.reviewDetailsActionSetMyselfAsReviewer(),
            e -> flowVm.setMyselfAsReviewer());
        ReviewAction mergeAction = ReviewAction.create(ReviewLocalize.reviewDetailsActionMerge(), e -> flowVm.merge());
        ReviewAction squashAndMergeAction = ReviewAction.create(ReviewLocalize.reviewDetailsActionSquashAndMerge(), e -> flowVm.merge());
        ReviewAction rebaseAction = ReviewAction.create(ReviewLocalize.reviewDetailsActionRebase(), e -> flowVm.merge());
        ReviewAction closeAction = ReviewAction.create(LocalizeValue.localizeTODO("Close"), e -> flowVm.close());
        ReviewAction reopenAction = ReviewAction.create(LocalizeValue.localizeTODO("Reopen"), e -> flowVm.reopen());
        ReviewAction postReviewAction = ReviewAction.create(LocalizeValue.localizeTODO("Ready for Review"), e -> flowVm.postReview());

        List<ReviewAction> allActions = List.of(requestReviewAction, reRequestReviewAction, setMyselfAsReviewerAction, mergeAction,
            squashAndMergeAction, rebaseAction, closeAction, reopenAction, postReviewAction);
        ReviewUIUtil.bind(parentDisposable, flowVm.isBusy(), busy -> {
            for (ReviewAction action : allActions) {
                action.setEnabled(!busy);
            }
        });

        DefaultActionGroup mergeGroup = new DefaultActionGroup(ReviewLocalize.reviewDetailsActionMergeGroup(), true);
        mergeGroup.add(mergeAction);
        mergeGroup.add(squashAndMergeAction);
        mergeGroup.add(rebaseAction);

        DefaultActionGroup moreActionsGroup = new DefaultActionGroup();
        ReviewUIUtil.bind(parentDisposable, flowVm.getReviewState(), reviewState -> {
            moreActionsGroup.removeAll();
            if (reviewState == ReviewState.ACCEPTED) {
                moreActionsGroup.add(requestReviewAction);
            }
            else {
                moreActionsGroup.add(setMyselfAsReviewerAction);
                moreActionsGroup.add(mergeGroup);
            }
            moreActionsGroup.add(closeAction);
        });

        SandReviewSubmitViewModel submitVm = new SandReviewSubmitViewModel(flowVm, review.getThreads().size());
        Button submitButton = Button.create(ReviewLocalize.reviewStartSubmitActionWithComments(review.getThreads().size()));
        submitButton.addClickListener(event -> submitVm.setPopupFuture(mySubmitPopupHandler.show(submitVm, submitButton, true)));
        ReviewUIUtil.bind(parentDisposable, flowVm.isBusy(), busy -> submitButton.setEnabled(!busy));

        Button mergeButton = ReviewUIUtil.createActionButton(parentDisposable, mergeAction, "SandReviewDetails");
        mergeButton.addStyle(ButtonStyle.PRIMARY);
        ReviewUIUtil.bind(parentDisposable, flowVm.getReviewState(), reviewState -> mergeButton.setVisible(reviewState == ReviewState.ACCEPTED));

        HorizontalLayout openedStatePanel = HorizontalLayout.create(CodeReviewDetailsActionsComponentFactory.BUTTONS_GAP);
        openedStatePanel.add(myActionsComponentFactory.createRequestReviewButton(parentDisposable,
            flowVm.getReviewState(), flowVm.getRequestedReviewers(), requestReviewAction));
        openedStatePanel.add(myActionsComponentFactory.createReRequestReviewButton(parentDisposable,
            flowVm.getReviewState(), flowVm.getRequestedReviewers(), reRequestReviewAction));
        openedStatePanel.add(submitButton);
        openedStatePanel.add(mergeButton);
        openedStatePanel.add(myActionsComponentFactory.createMoreButton(moreActionsGroup));

        Component actions = myActionsComponentFactory.createActionsComponent(parentDisposable,
            review.getReviewRequestState(),
            openedStatePanel,
            myActionsComponentFactory.createActionsForMergedReview(),
            myActionsComponentFactory.createActionsForClosedReview(parentDisposable, reopenAction),
            myActionsComponentFactory.createActionsForDraftReview(parentDisposable, postReviewAction));

        VerticalLayout panel = VerticalLayout.create(Space.NONE);
        panel.add(actions);
        panel.paddingBuilder().bottomSet(Space.X_LARGE).horizontalSet(Space.X_LARGE).apply();
        return panel;
    }
}
