// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.comment;

import consulo.application.util.DateFormatUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.CommonLocalize;
import consulo.review.icon.ReviewIconGroup;
import consulo.review.util.ReviewUIUtil;
import consulo.review.list.UserPresentation;
import consulo.review.localize.ReviewLocalize;
import consulo.review.timeline.thread.CodeReviewFoldableThreadViewModel;
import consulo.review.timeline.thread.CodeReviewResolvableItemViewModel;
import consulo.review.user.CodeReviewUser;
import consulo.ui.Avatar;
import consulo.ui.AvatarStyle;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.style.ComponentColors;

import java.util.Date;

/**
 * The controls of a folded review thread: who replied, the link unfolding the replies, when the last reply came and
 * the resolve toggle.
 */
public final class CodeReviewCommentUIUtil {
    public static final int COMPACT_AVATAR_SIZE = 20;

    private static final Space HORIZONTAL_GAP = Space.LARGE;
    private static final Space HORIZONTAL_GROUP_GAP = Space.MEDIUM;
    private static final Space VERTICAL_PADDING = Space.LARGE;

    private CodeReviewCommentUIUtil() {
    }

    @RequiredUIAccess
    public static Component createPostNowButton(Runnable action) {
        return Hyperlink.create(ReviewLocalize.reviewCommentsPostNowAction(), event -> action.run());
    }

    @RequiredUIAccess
    public static Component createDeleteCommentIconButton(Runnable action) {
        Button button = createIconButton(PlatformIconGroup.actionsGc(), CommonLocalize.buttonDelete());
        button.addClickListener(event -> {
            UIAccess uiAccess = UIAccess.current();
            MessageBoxes.yesNo()
                .title(ReviewLocalize.reviewCommentsDeleteConfirmationTitle())
                .text(ReviewLocalize.reviewCommentsDeleteConfirmation())
                .showAsync(button)
                .whenCompleteAsync((yes, error) -> {
                    if (Boolean.TRUE.equals(yes)) {
                        action.run();
                    }
                }, uiAccess);
        });
        return button;
    }

    @RequiredUIAccess
    public static Component createEditButton(Runnable action) {
        Button button = createIconButton(PlatformIconGroup.actionsEdit(), CommonLocalize.buttonEdit());
        button.addClickListener(event -> action.run());
        return button;
    }

    @RequiredUIAccess
    public static Component createAddReactionButton(Runnable action) {
        Button button = createIconButton(ReviewIconGroup.addemoji(), ReviewLocalize.reviewCommentsReactionAddTooltip());
        button.addClickListener(event -> action.run());
        return button;
    }

    @RequiredUIAccess
    private static Button createIconButton(Image icon, LocalizeValue tooltip) {
        Button button = Button.create(LocalizeValue.empty());
        button.setIcon(icon);
        button.addStyle(ButtonStyle.TOOLBAR);
        button.setToolTipText(tooltip);
        return button;
    }

    @RequiredUIAccess
    public static Component createFoldedThreadControlsIn(Disposable parentDisposable, CodeReviewFoldableThreadViewModel vm) {
        HorizontalLayout authorsPanel = HorizontalLayout.create(Space.NONE);
        authorsPanel.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);

        Hyperlink repliesLink = Hyperlink.create(LocalizeValue.empty(), event -> vm.unfoldReplies());

        Label lastReplyDateLabel = Label.create();
        lastReplyDateLabel.setForegroundColor(ComponentColors.DISABLED_TEXT);

        HorizontalLayout panel = HorizontalLayout.create(HORIZONTAL_GAP);
        panel.paddingBuilder().verticalSet(VERTICAL_PADDING).apply();
        panel.add(authorsPanel);
        panel.add(repliesLink);
        panel.add(lastReplyDateLabel);

        ReviewUIUtil.bind(parentDisposable, vm.getRepliesState(), vm.getCanCreateReplies(), (data, canReply) -> {
            authorsPanel.removeAll();
            for (CodeReviewUser author : data.getRepliesAuthors()) {
                authorsPanel.add(createAvatar(author));
            }
            authorsPanel.setVisible(data.getRepliesCount() > 0);

            repliesLink.setVisible(data.getRepliesCount() > 0 || canReply);
            repliesLink.setText(data.getRepliesCount() == 0
                ? ReviewLocalize.reviewCommentsReplyAction()
                : ReviewLocalize.reviewCommentsRepliesAction(data.getRepliesCount()));

            Date lastReplyDate = data.getLastReplyDate();
            lastReplyDateLabel.setVisible(lastReplyDate != null);
            lastReplyDateLabel.setText(lastReplyDate == null
                ? LocalizeValue.empty()
                : LocalizeValue.of(DateFormatUtil.formatPrettyDateTime(lastReplyDate)));
        });
        ReviewUIUtil.bind(parentDisposable, vm.isBusy(), busy -> repliesLink.setEnabled(!busy));

        if (vm instanceof CodeReviewResolvableItemViewModel resolvable) {
            Component resolveLink = createResolveLink(parentDisposable, resolvable);
            resolveLink.paddingBuilder().leftSet(HORIZONTAL_GROUP_GAP).apply();
            panel.add(resolveLink);
        }
        return panel;
    }

    @RequiredUIAccess
    private static Avatar createAvatar(CodeReviewUser author) {
        UserPresentation presentation = author instanceof UserPresentation user ? user : null;
        Avatar avatar = Avatar.create(LocalizeValue.of(presentation == null ? "" : presentation.getPresentableName()),
            presentation == null ? null : presentation.getAvatarIcon());
        avatar.addStyle(AvatarStyle.X_SMALL);
        return avatar;
    }

    @RequiredUIAccess
    public static Component createResolveLink(Disposable parentDisposable, CodeReviewResolvableItemViewModel vm) {
        Hyperlink unResolveLink = Hyperlink.create(LocalizeValue.empty(), event -> vm.changeResolvedState());
        ReviewUIUtil.bind(parentDisposable, vm.getCanChangeResolvedState(), unResolveLink::setVisible);
        ReviewUIUtil.bind(parentDisposable, vm.isBusy(), busy -> unResolveLink.setEnabled(!busy));
        ReviewUIUtil.bind(parentDisposable, vm.isResolved(), resolved -> unResolveLink.setText(getResolveToggleActionText(resolved)));
        return unResolveLink;
    }

    public static LocalizeValue getResolveToggleActionText(boolean resolved) {
        return resolved ? ReviewLocalize.reviewCommentsUnresolveAction() : ReviewLocalize.reviewCommentsResolveAction();
    }

    public static Image avatarOf(CodeReviewUser user) {
        return user instanceof UserPresentation presentation ? presentation.getAvatarIcon() : PlatformIconGroup.actionsLoginavatar();
    }
}
