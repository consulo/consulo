// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import com.intellij.collaboration.async.LaunchNowKt;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.Either;
import com.intellij.collaboration.ui.codereview.avatar.Avatar;
import com.intellij.collaboration.ui.codereview.details.data.CodeReviewCIJob;
import com.intellij.collaboration.ui.codereview.details.data.CodeReviewCIJobState;
import com.intellij.collaboration.ui.codereview.details.data.ReviewState;
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewStatusViewModel;
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.collaboration.ui.codereview.list.search.PopupConfig;
import com.intellij.collaboration.ui.codereview.list.search.ShowDirection;
import com.intellij.collaboration.ui.icon.CIBuildStatusIcons;
import com.intellij.collaboration.ui.util.BindingUtilKt;
import com.intellij.collaboration.ui.util.DimensionRestrictions;
import com.intellij.collaboration.ui.util.popup.PopupItemPresentation;
import consulo.collaboration.CollaborationToolsBundle;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.PopupHandler;
import consulo.webBrowser.BrowserUtil;
import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class CodeReviewDetailsStatusComponentFactory {
    private static final int STATUS_COMPONENT_BORDER = 5;
    private static final int STATUS_REVIEWER_BORDER = 3;
    private static final int STATUS_REVIEWER_COMPONENT_GAP = 8;
    private static final int CI_COMPONENTS_GAP = 8;

    private CodeReviewDetailsStatusComponentFactory() {
    }

    @SuppressWarnings("FunctionName")
    public static @Nonnull JLabel ReviewDetailsStatusLabel(@Nonnull String componentName) {
        JLabel label = new JLabel();
        label.setName(componentName);
        label.setOpaque(false);
        JLabelUtil.setTrimOverflow(label, true);
        return label;
    }

    /**
     * @param resolveActionFlow If an ActionListener is emitted, there's some action to execute on click.
     *                          If a String is emitted, it's the tooltip text to tell why no action is available.
     */
    public static @Nonnull JComponent createConflictsComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<Boolean> hasConflicts,
        @Nonnull Flow<Either<@Nls String, ActionListener>> resolveActionFlow,
        @Nonnull Flow<Boolean> isBusyFlow
    ) {
        JLabel title = new JLabel();
        BindingUtilKt.bindIconIn(
            title,
            scope,
            FlowKt.map(hasConflicts, it -> it == null ? CIBuildStatusIcons.pending : CIBuildStatusIcons.failed)
        );
        BindingUtilKt.bindTextIn(
            title,
            scope,
            FlowKt.map(
                hasConflicts,
                it -> it == null
                    ? CollaborationToolsLocalize.reviewDetailsStatusConflictsPending().get()
                    : CollaborationToolsLocalize.reviewDetailsStatusConflicts().get()
            )
        );

        ActionLink resolveLink = new ActionLink(CollaborationToolsLocalize.reviewDetailsStatusConflictsResolve().get());
        BindingUtilKt.bindVisibilityIn(resolveLink, scope,
            FlowKt.combine(hasConflicts, resolveActionFlow, (hasConf, resolveActionOrText) ->
                Boolean.TRUE.equals(hasConf) && !Either.left(null).equals(resolveActionOrText)
            )
        );
        BindingUtilKt.bindEnabledIn(
            resolveLink,
            scope,
            FlowKt.combine(
                resolveActionFlow,
                isBusyFlow,
                (actionOrText, isBusy) -> !isBusy && actionOrText.isRight()
            )
        );

        resolveLink.setAutoHideOnDisable(false);

        LaunchNowKt.launchNow(
            scope,
            (coroutineScope, continuation) -> FlowKt.collect(
                resolveActionFlow,
                resolveActionOrText -> {
                    resolveLink.setToolTipText(null);
                    for (ActionListener al : resolveLink.getActionListeners()) {
                        resolveLink.removeActionListener(al);
                    }
                    resolveActionOrText.fold(
                        left -> {
                            resolveLink.setToolTipText(left);
                            return kotlin.Unit.INSTANCE;
                        },
                        right -> {
                            resolveLink.addActionListener(right);
                            return kotlin.Unit.INSTANCE;
                        }
                    );
                    return kotlin.Unit.INSTANCE;
                },
                continuation
            )
        );

        HorizontalListPanel panel = new HorizontalListPanel(CI_COMPONENTS_GAP);
        panel.setName("Code review status: review has conflicts");
        panel.setBorder(JBUI.Borders.empty(STATUS_COMPONENT_BORDER, 0));
        BindingUtilKt.bindVisibilityIn(panel, scope, FlowKt.map(hasConflicts, it -> it == null || it /* Also show when loading*/));

        panel.add(title);
        panel.add(resolveLink);
        return panel;
    }

    public static @Nonnull JComponent createConflictsComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<Boolean> hasConflicts
    ) {
        return createConflictsComponent(scope, hasConflicts,
            FlowKt.flowOf(Either.left(null)),
            FlowKt.flowOf(false)
        );
    }

    public static @Nonnull JComponent createMergeClarificationComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<String> clarification
    ) {
        JTextPane textPane = new JTextPane();
        textPane.setName("Code review status: merge clarification");
        textPane.setOpaque(false);
        textPane.setEditable(false);
        textPane.setBorder(JBUI.Borders.empty(0, 3));
        BindingUtilKt.bindTextIn(textPane, scope, clarification);

        JComponent wrapper = CollaborationToolsUIUtil.wrapWithLimitedSize(textPane, new DimensionRestrictions.LinesHeight(textPane, 2));
        wrapper.setBorder(JBUI.Borders.empty(STATUS_COMPONENT_BORDER, 0));
        wrapper.setVisible(false);
        BindingUtilKt.bindVisibilityIn(wrapper, scope, FlowKt.map(clarification, it -> !it.isEmpty()));
        return wrapper;
    }

    public static <T> @Nonnull JComponent createNeedReviewerComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<Map<T, ReviewState>> reviewersReview
    ) {
        JLabel label = ReviewDetailsStatusLabel("Code review status: need reviewer");
        label.setBorder(JBUI.Borders.empty(STATUS_COMPONENT_BORDER, 0));
        label.setIcon(CIBuildStatusIcons.warning);
        label.setText(CollaborationToolsLocalize.reviewDetailsStatusReviewerMissing().get());
        BindingUtilKt.bindVisibilityIn(label, scope, FlowKt.map(reviewersReview, Map::isEmpty));
        return label;
    }

    public static @Nonnull JComponent createRequiredReviewsComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<Integer> requiredApprovingReviewsCount,
        @Nonnull Flow<Boolean> isDraft
    ) {
        JLabel label = ReviewDetailsStatusLabel("Code review status: required reviews");
        label.setBorder(JBUI.Borders.empty(STATUS_COMPONENT_BORDER, 0));
        label.setIcon(CIBuildStatusIcons.failed);
        BindingUtilKt.bindVisibilityIn(
            label,
            scope,
            FlowKt.combine(
                requiredApprovingReviewsCount,
                isDraft,
                (count, draft) -> (int) count > 0 && !(boolean) draft
            )
        );
        BindingUtilKt.bindTextIn(
            label,
            scope,
            FlowKt.map(
                requiredApprovingReviewsCount,
                count -> CollaborationToolsBundle.message("review.details.status.reviewer.required", count)
            )
        );
        return label;
    }

    public static @Nonnull JComponent createRequiredResolveConversationsComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<Boolean> requiredConversationsResolved
    ) {
        JLabel label = ReviewDetailsStatusLabel("Code review status: required conversations resolved");
        label.setBorder(JBUI.Borders.empty(STATUS_COMPONENT_BORDER, 0));
        label.setIcon(CIBuildStatusIcons.failed);
        label.setText(CollaborationToolsLocalize.reviewDetailsStatusConversations().get());
        label.setVisible(false);
        BindingUtilKt.bindVisibilityIn(label, scope, requiredConversationsResolved);
        return label;
    }

    public static @Nonnull JComponent createRestrictionComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<Boolean> isRestricted,
        @Nonnull Flow<Boolean> isDraft
    ) {
        JLabel label = ReviewDetailsStatusLabel("Code review status: restricted rights");
        label.setBorder(JBUI.Borders.empty(STATUS_COMPONENT_BORDER, 0));
        label.setIcon(CIBuildStatusIcons.failed);
        label.setText(CollaborationToolsLocalize.reviewDetailsStatusNotAuthorizedToMerge().get());
        BindingUtilKt.bindVisibilityIn(
            label,
            scope,
            FlowKt.combine(
                isRestricted,
                isDraft,
                (restricted, draft) -> (boolean) restricted && !(boolean) draft
            )
        );
        return label;
    }

    public static @Nonnull JComponent createCiComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull CodeReviewStatusViewModel statusVm
    ) {
        var ciJobs = statusVm.getCiJobs();

        JLabel title = new JLabel();
        BindingUtilKt.bindIconIn(title, scope, FlowKt.map(ciJobs, jobs -> calcPipelineIcon((List<CodeReviewCIJob>) jobs)));
        BindingUtilKt.bindTextIn(title, scope, FlowKt.map(ciJobs, jobs -> calcPipelineText((List<CodeReviewCIJob>) jobs)));

        ActionLink detailsLink = new ActionLink(
            CollaborationToolsLocalize.reviewDetailsStatusCiLinkDetails().get(),
            e -> statusVm.showJobsDetails()
        );

        LaunchNowKt.launchNow(
            scope,
            (coroutineScope, continuation) -> kotlinx.coroutines.flow.FlowKt.collectLatest(
                statusVm.getShowJobsDetailsRequests(),
                jobs -> {
                    @SuppressWarnings("unchecked")
                    List<CodeReviewCIJob> jobList = (List<CodeReviewCIJob>) jobs;
                    CodeReviewCIJob selectedJob = ChooserPopupUtil.showChooserPopup(
                        new RelativePoint(detailsLink, new Point()),
                        jobList,
                        job -> new PopupItemPresentation.Simple(job.getName(), convertToIcon(job.getStatus())),
                        new PopupConfig(
                            CollaborationToolsLocalize.reviewDetailsStatusCiPopupTitle().get(),
                            false,
                            ShowDirection.ABOVE
                        )
                    );

                    if (selectedJob != null && selectedJob.getDetailsUrl() != null) {
                        BrowserUtil.browse(selectedJob.getDetailsUrl());
                    }
                    return kotlin.Unit.INSTANCE;
                },
                continuation
            )
        );

        HorizontalListPanel panel = new HorizontalListPanel(CI_COMPONENTS_GAP);
        panel.setName("Code review status: CI");
        panel.setBorder(JBUI.Borders.empty(STATUS_COMPONENT_BORDER, 0));
        BindingUtilKt.bindVisibilityIn(panel, scope, FlowKt.map(ciJobs, it -> !((List<?>) it).isEmpty()));

        panel.add(title);
        panel.add(detailsLink);
        return panel;
    }

    public static <Reviewer, IconKey> @Nonnull JComponent createReviewersReviewStateComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<Map<Reviewer, ReviewState>> reviewersReview,
        @Nonnull Function<Reviewer, ActionGroup> reviewerActionProvider,
        @Nonnull Function<Reviewer, String> reviewerNameProvider,
        @Nonnull Function<Reviewer, IconKey> avatarKeyProvider,
        @Nonnull TriFunction<ReviewState, IconKey, Integer, Icon> iconProvider,
        boolean statusIconsEnabled
    ) {
        VerticalListPanel panel = new VerticalListPanel();
        panel.setName("Code review status: reviewers");
        BindingUtilKt.bindVisibilityIn(panel, scope, FlowKt.map(reviewersReview, it -> !((Map<?, ?>) it).isEmpty()));

        kotlinx.coroutines.BuildersKt.launch(
            scope,
            scope.getCoroutineContext(),
            kotlinx.coroutines.CoroutineStart.DEFAULT,
            (coroutineScope, continuation) -> FlowKt.collect(
                reviewersReview,
                reviewMap -> {
                    @SuppressWarnings("unchecked")
                    Map<Reviewer, ReviewState> map = (Map<Reviewer, ReviewState>) reviewMap;
                    panel.removeAll();
                    for (Map.Entry<Reviewer, ReviewState> entry : map.entrySet()) {
                        panel.add(createReviewerReviewStatus(
                            entry.getKey(), entry.getValue(),
                            reviewerActionProvider, reviewerNameProvider,
                            avatarKeyProvider, iconProvider, statusIconsEnabled
                        ));
                    }
                    panel.revalidate();
                    panel.repaint();
                    return kotlin.Unit.INSTANCE;
                },
                continuation
            )
        );

        return panel;
    }

    public static <Reviewer, IconKey> @Nonnull JComponent createReviewersReviewStateComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<Map<Reviewer, ReviewState>> reviewersReview,
        @Nonnull Function<Reviewer, ActionGroup> reviewerActionProvider,
        @Nonnull Function<Reviewer, String> reviewerNameProvider,
        @Nonnull Function<Reviewer, IconKey> avatarKeyProvider,
        @Nonnull TriFunction<ReviewState, IconKey, Integer, Icon> iconProvider
    ) {
        return createReviewersReviewStateComponent(scope, reviewersReview, reviewerActionProvider,
            reviewerNameProvider, avatarKeyProvider, iconProvider, true
        );
    }

    private static <Reviewer, IconKey> @Nonnull JComponent createReviewerReviewStatus(
        @Nonnull Reviewer reviewer,
        @Nonnull ReviewState reviewState,
        @Nonnull Function<Reviewer, ActionGroup> reviewerActionProvider,
        @Nonnull Function<Reviewer, String> reviewerNameProvider,
        @Nonnull Function<Reviewer, IconKey> avatarKeyProvider,
        @Nonnull TriFunction<ReviewState, IconKey, Integer, Icon> iconProvider,
        boolean statusIconsEnabled
    ) {
        HorizontalListPanel panel = new HorizontalListPanel(STATUS_REVIEWER_COMPONENT_GAP);
        panel.setBorder(JBUI.Borders.empty(STATUS_REVIEWER_BORDER, 0));

        JLabel reviewerLabel = ReviewDetailsStatusLabel("Code review status: reviewer");
        reviewerLabel.setIconTextGap(STATUS_REVIEWER_COMPONENT_GAP);
        reviewerLabel.setIcon(iconProvider.apply(reviewState, avatarKeyProvider.apply(reviewer), Avatar.Sizes.OUTLINED));
        reviewerLabel.setText(ReviewDetailsUIUtil.getReviewStateText(reviewState, reviewerNameProvider.apply(reviewer)));

        if (statusIconsEnabled) {
            JLabel reviewStatusIconLabel = new JLabel();
            reviewStatusIconLabel.setIcon(ReviewDetailsUIUtil.getReviewStateIcon(reviewState));
            panel.add(reviewStatusIconLabel);
        }

        panel.add(reviewerLabel);

        if (reviewState != ReviewState.ACCEPTED) {
            ActionGroup action = reviewerActionProvider.apply(reviewer);
            if (action != null) {
                PopupHandler.installPopupMenu(panel, action, "CodeReviewReviewerStatus");
            }
        }

        return panel;
    }

    private static @Nonnull Icon calcPipelineIcon(@Nonnull List<CodeReviewCIJob> jobs) {
        long failed = jobs.stream().filter(j -> j.getStatus() == CodeReviewCIJobState.FAILED).count();
        long pending = jobs.stream().filter(j -> j.getStatus() == CodeReviewCIJobState.PENDING).count();
        if (jobs.stream().filter(CodeReviewCIJob::isRequired).allMatch(j -> j.getStatus() == CodeReviewCIJobState.SUCCESS)) {
            return CIBuildStatusIcons.success;
        }
        if (pending != 0 && failed != 0) {
            return CIBuildStatusIcons.failedInProgress;
        }
        if (pending != 0) {
            return CIBuildStatusIcons.pending;
        }
        return CIBuildStatusIcons.failed;
    }

    private static @Nls @Nonnull String calcPipelineText(@Nonnull List<CodeReviewCIJob> jobs) {
        long failed = jobs.stream().filter(j -> j.getStatus() == CodeReviewCIJobState.FAILED).count();
        long pending = jobs.stream().filter(j -> j.getStatus() == CodeReviewCIJobState.PENDING).count();
        if (jobs.stream().filter(CodeReviewCIJob::isRequired).allMatch(j -> j.getStatus() == CodeReviewCIJobState.SUCCESS)) {
            return CollaborationToolsLocalize.reviewDetailsStatusCiPassed().get();
        }
        if (pending != 0 && failed != 0) {
            return CollaborationToolsLocalize.reviewDetailsStatusCiProgressAndFailed().get();
        }
        if (pending != 0) {
            return CollaborationToolsLocalize.reviewDetailsStatusCiProgress().get();
        }
        return CollaborationToolsLocalize.reviewDetailsStatusCiFailed().get();
    }

    private static @Nonnull Icon convertToIcon(@Nonnull CodeReviewCIJobState state) {
        return switch (state) {
            case FAILED -> CIBuildStatusIcons.failed;
            case PENDING -> CIBuildStatusIcons.pending;
            case SKIPPED -> CIBuildStatusIcons.skipped;
            case SUCCESS -> CIBuildStatusIcons.success;
        };
    }

    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
