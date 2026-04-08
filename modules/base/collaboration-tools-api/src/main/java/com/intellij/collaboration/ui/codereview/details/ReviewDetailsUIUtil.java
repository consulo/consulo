// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.codereview.avatar.Avatar;
import com.intellij.collaboration.ui.codereview.details.data.ReviewRequestState;
import com.intellij.collaboration.ui.codereview.details.data.ReviewState;
import consulo.application.AllIcons;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;

public final class ReviewDetailsUIUtil {
    private static final int TITLE_PANEL_GAP = 8;

    private static final int OLD_UI_LEFT_GAP = 14;
    private static final int NEW_UI_LEFT_GAP = 16;
    private static final int RIGHT_GAP = 14;
    private static final int BUTTON_PADDING = 2;

    private ReviewDetailsUIUtil() {
    }

    public static @Nonnull JComponent createTitlePanel(@Nonnull JComponent title, @Nonnull JComponent timelineLink) {
        VerticalListPanel panel = new VerticalListPanel(TITLE_PANEL_GAP);
        panel.add(title);
        panel.add(timelineLink);
        return panel;
    }

    public static @Nonnull String getRequestStateText(@Nonnull ReviewRequestState state) {
        return switch (state) {
            case OPENED -> CollaborationToolsLocalize.reviewDetailsReviewStateOpen().get();
            case CLOSED -> CollaborationToolsLocalize.reviewDetailsReviewStateClosed().get();
            case MERGED -> CollaborationToolsLocalize.reviewDetailsReviewStateMerged().get();
            case DRAFT -> CollaborationToolsLocalize.reviewDetailsReviewStateDraft().get();
        };
    }

    public static @Nonnull Icon getReviewStateIcon(@Nonnull ReviewState reviewState) {
        return switch (reviewState) {
            case ACCEPTED -> ExperimentalUI.isNewUI() ? AllIcons.Status.Success : PlatformIconGroup.runconfigurationsTestpassed();
            case WAIT_FOR_UPDATES -> ExperimentalUI.isNewUI() ? PlatformIconGroup.generalError() : PlatformIconGroup.runconfigurationsTesterror();
            case NEED_REVIEW -> ExperimentalUI.isNewUI() ? PlatformIconGroup.generalWarning() : PlatformIconGroup.runconfigurationsTestfailed();
        };
    }

    public static @Nls @Nonnull String getReviewStateText(@Nonnull ReviewState reviewState, @Nonnull String reviewer) {
        return switch (reviewState) {
            case ACCEPTED -> CollaborationToolsLocalize.reviewDetailsStatusReviewerApproved(reviewer).get();
            case WAIT_FOR_UPDATES -> CollaborationToolsLocalize.reviewDetailsStatusReviewerWaitForUpdates(reviewer).get();
            case NEED_REVIEW -> CollaborationToolsLocalize.reviewDetailsStatusReviewerNeedReview(reviewer).get();
        };
    }

    public static @Nonnull Color getReviewStateIconBorder(@Nonnull ReviewState reviewState) {
        return switch (reviewState) {
            case ACCEPTED -> Avatar.Color.ACCEPTED_BORDER;
            case WAIT_FOR_UPDATES -> Avatar.Color.WAIT_FOR_UPDATES_BORDER;
            case NEED_REVIEW -> Avatar.Color.NEED_REVIEW_BORDER;
        };
    }

    @SuppressWarnings("UseDPIAwareInsets")
    public static @Nonnull Insets getTitleGaps() {
        return CollaborationToolsUIUtil.getInsets(
            new Insets(12, OLD_UI_LEFT_GAP, 8, RIGHT_GAP),
            new Insets(16, NEW_UI_LEFT_GAP, 16, RIGHT_GAP)
        );
    }

    @SuppressWarnings("UseDPIAwareInsets")
    public static @Nonnull Insets getCommitPopupBranchesGaps() {
        return CollaborationToolsUIUtil.getInsets(
            new Insets(0, OLD_UI_LEFT_GAP, 0, RIGHT_GAP),
            new Insets(0, NEW_UI_LEFT_GAP, 4, RIGHT_GAP)
        );
    }

    @SuppressWarnings("UseDPIAwareInsets")
    public static @Nonnull Insets getCommitInfoGaps() {
        return CollaborationToolsUIUtil.getInsets(
            new Insets(0, OLD_UI_LEFT_GAP, 12, RIGHT_GAP),
            new Insets(0, NEW_UI_LEFT_GAP, 12, RIGHT_GAP)
        );
    }

    @SuppressWarnings("UseDPIAwareInsets")
    public static @Nonnull Insets getStatusesGaps() {
        return CollaborationToolsUIUtil.getInsets(
            new Insets(6, OLD_UI_LEFT_GAP, 10, RIGHT_GAP),
            new Insets(6, NEW_UI_LEFT_GAP, 10, RIGHT_GAP)
        );
    }

    @SuppressWarnings("UseDPIAwareInsets")
    public static @Nonnull Insets getActionsGaps() {
        return CollaborationToolsUIUtil.getInsets(
            new Insets(0, 11, 15, RIGHT_GAP),
            new Insets(0, NEW_UI_LEFT_GAP - BUTTON_PADDING, 18, RIGHT_GAP)
        );
    }

    public static int getStatusesMaxHeight() {
        return CollaborationToolsUIUtil.getSize(143, 149);
    }
}
