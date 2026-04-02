// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import com.intellij.collaboration.ui.HorizontalListPanel;
import com.intellij.collaboration.ui.codereview.details.data.ReviewRequestState;
import com.intellij.collaboration.ui.codereview.details.data.ReviewState;
import com.intellij.collaboration.ui.util.BindingUtilKt;
import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Function;

public final class CodeReviewDetailsActionsComponentFactory {
    private static final int BUTTONS_GAP = 10;

    private CodeReviewDetailsActionsComponentFactory() {
    }

    public static <Reviewer> @Nonnull JComponent createRequestReviewButton(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<ReviewState> reviewState,
        @Nonnull Flow<List<Reviewer>> requestedReviewers,
        @Nonnull Action requestReviewAction
    ) {
        JButton button = new JButton(requestReviewAction);
        button.setOpaque(false);
        BindingUtilKt.bindVisibilityIn(
            button,
            scope,
            FlowKt.combine(
                reviewState,
                requestedReviewers,
                (state, reviewers) -> state == ReviewState.NEED_REVIEW || (state == ReviewState.WAIT_FOR_UPDATES && !reviewers.isEmpty())
            )
        );
        return button;
    }

    public static <Reviewer> @Nonnull JComponent createReRequestReviewButton(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<ReviewState> reviewState,
        @Nonnull Flow<List<Reviewer>> requestedReviewers,
        @Nonnull Action reRequestReviewAction
    ) {
        JButton button = new JButton(reRequestReviewAction);
        button.setOpaque(false);
        BindingUtilKt.bindVisibilityIn(
            button,
            scope,
            FlowKt.combine(
                reviewState,
                requestedReviewers,
                (state, reviewers) -> state == ReviewState.WAIT_FOR_UPDATES && reviewers.isEmpty()
            )
        );
        return button;
    }

    public static @Nonnull JComponent createActionsForGuest(
        @Nonnull CodeReviewActions reviewActions,
        @Nonnull DefaultActionGroup moreActionsGroup,
        @Nonnull Function<CodeReviewActions, ActionGroup> mergeActionsCreator
    ) {
        JButton setMyselfAsReviewerButton = new JButton(reviewActions.setMyselfAsReviewerAction());
        setMyselfAsReviewerButton.setOpaque(false);

        JComponent moreActionsButton = createMoreButton(moreActionsGroup);

        moreActionsGroup.removeAll();
        moreActionsGroup.add(BindingUtilKt.toAnAction(reviewActions.requestReviewAction()));
        moreActionsGroup.add(mergeActionsCreator.apply(reviewActions));
        moreActionsGroup.add(BindingUtilKt.toAnAction(reviewActions.closeReviewAction()));

        HorizontalListPanel panel = new HorizontalListPanel(BUTTONS_GAP);
        panel.add(setMyselfAsReviewerButton);
        panel.add(moreActionsButton);
        return panel;
    }

    public static @Nonnull JComponent createActionsComponent(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<ReviewRequestState> reviewRequestState,
        @Nonnull JComponent openedStatePanel,
        @Nonnull JComponent mergedStatePanel,
        @Nonnull JComponent closedStatePanel,
        @Nonnull JComponent draftedStatePanel
    ) {
        Wrapper wrapper = new Wrapper();
        BindingUtilKt.bindContentIn(
            wrapper,
            scope,
            FlowKt.map(
                reviewRequestState,
                requestState -> switch ((ReviewRequestState) requestState) {
                    case OPENED -> openedStatePanel;
                    case MERGED -> mergedStatePanel;
                    case CLOSED -> closedStatePanel;
                    case DRAFT -> draftedStatePanel;
                }
            )
        );
        return wrapper;
    }

    public static @Nonnull JComponent createActionsForMergedReview() {
        return JBUI.Panels.simplePanel();
    }

    public static @Nonnull JComponent createActionsForClosedReview(@Nonnull Action reopenReviewAction) {
        JButton reopenReviewButton = new JButton(reopenReviewAction);
        reopenReviewButton.setOpaque(false);
        HorizontalListPanel panel = new HorizontalListPanel(BUTTONS_GAP);
        panel.add(reopenReviewButton);
        return panel;
    }

    public static @Nonnull JComponent createActionsForDraftReview(@Nonnull Action postReviewAction) {
        JButton postReviewButton = new JButton(postReviewAction);
        postReviewButton.setOpaque(false);
        HorizontalListPanel panel = new HorizontalListPanel(BUTTONS_GAP);
        panel.add(postReviewButton);
        return panel;
    }

    public static @Nonnull JComponent createMoreButton(@Nonnull ActionGroup actionGroup) {
        InlineIconButton button = new InlineIconButton(AllIcons.Actions.More);
        button.setWithBackgroundHover(true);
        button.setActionListener(event -> {
            JComponent parentComponent = (JComponent) event.getSource();
            var popupMenu = ActionManager.getInstance().createActionPopupMenu("Code.Review.Details.Actions.More", actionGroup);
            Point point = RelativePoint.getSouthWestOf(parentComponent).getOriginalPoint();
            popupMenu.getComponent().show(parentComponent, point.x, point.y + JBUIScale.scale(8));
        });
        return button;
    }

    public record CodeReviewActions(
        @Nonnull Action requestReviewAction,
        @Nonnull Action reRequestReviewAction,
        @Nonnull Action closeReviewAction,
        @Nonnull Action reopenReviewAction,
        @Nonnull Action setMyselfAsReviewerAction,
        @Nonnull Action postReviewAction,
        @Nonnull Action mergeReviewAction,
        @Nonnull Action mergeSquashReviewAction,
        @Nonnull Action rebaseReviewAction
    ) {
    }
}
