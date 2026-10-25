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
package consulo.review.impl.internal.details;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.review.details.CodeReviewDetailsStatusComponentFactory;
import consulo.review.details.data.CodeReviewCIJob;
import consulo.review.details.data.CodeReviewCIJobState;
import consulo.review.details.data.ReviewState;
import consulo.review.details.model.CodeReviewStatusViewModel;
import consulo.review.icon.CIBuildStatusIcons;
import consulo.review.localize.ReviewLocalize;
import consulo.review.util.Either;
import consulo.review.util.ReviewUIUtil;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

@Singleton
@ServiceImpl
public class CodeReviewDetailsStatusComponentFactoryImpl implements CodeReviewDetailsStatusComponentFactory {
    private static final String ERROR_PLACE = "CodeReviewDetailsStatusError";
    private static final String REVIEWER_STATUS_PLACE = "CodeReviewReviewerStatus";

    private static final Space STATUS_COMPONENT_BORDER = Space.SMALL;
    private static final Space STATUS_REVIEWER_BORDER = Space.X_SMALL;
    private static final Space STATUS_REVIEWER_COMPONENT_GAP = Space.LARGE;

    private static final Space CI_COMPONENTS_GAP = Space.LARGE;

    @RequiredUIAccess
    @Override
    public Component createErrorComponent(LocalizeValue message, @Nullable AnAction action) {
        Label label = Label.create(message);
        label.setImage(CIBuildStatusIcons.failed());
        if (action == null) {
            return label;
        }

        HorizontalLayout panel = HorizontalLayout.create(CI_COMPONENTS_GAP);
        panel.paddingBuilder().verticalSet(STATUS_COMPONENT_BORDER).apply();
        panel.add(label);
        Hyperlink actionLink = Hyperlink.create(action.getTemplatePresentation().getTextValue());
        actionLink.addHyperlinkListener(event -> ReviewUIUtil.performAction(action, actionLink, ERROR_PLACE));
        panel.add(actionLink);
        return panel;
    }

    @RequiredUIAccess
    @Override
    public Component createConflictsComponent(Disposable parentDisposable,
                                              ObservableValue<@Nullable Boolean> hasConflicts,
                                              ObservableValue<Either<LocalizeValue, Runnable>> resolveAction,
                                              ObservableValue<Boolean> isBusy) {
        Label title = Label.create();
        ReviewUIUtil.bind(parentDisposable, hasConflicts, it -> {
            title.setImage(it == null ? CIBuildStatusIcons.pending() : CIBuildStatusIcons.failed());
            title.setText(it == null
                ? ReviewLocalize.reviewDetailsStatusConflictsPending()
                : ReviewLocalize.reviewDetailsStatusConflicts());
        });

        AtomicReference<@Nullable Runnable> resolveListener = new AtomicReference<>();
        Hyperlink resolveLink = Hyperlink.create(ReviewLocalize.reviewDetailsStatusConflictsResolve(), event -> {
            Runnable listener = resolveListener.get();
            if (listener != null) {
                listener.run();
            }
        });
        ReviewUIUtil.bind(parentDisposable, hasConflicts, resolveAction, (conflicts, resolveActionOrText) ->
            resolveLink.setVisible(conflicts == Boolean.TRUE && !resolveActionOrText.equals(Either.left(LocalizeValue.empty()))));
        ReviewUIUtil.bind(parentDisposable, resolveAction, isBusy, (actionOrText, busy) ->
            resolveLink.setEnabled(!busy && actionOrText.isRight()));
        ReviewUIUtil.bind(parentDisposable, resolveAction, resolveActionOrText -> {
            resolveLink.setToolTipText(LocalizeValue.empty());
            resolveListener.set(null);

            if (resolveActionOrText instanceof Either.Left<LocalizeValue, Runnable> left) {
                resolveLink.setToolTipText(left.value());
            }
            else if (resolveActionOrText instanceof Either.Right<LocalizeValue, Runnable> right) {
                resolveListener.set(right.value());
            }
        });

        HorizontalLayout panel = HorizontalLayout.create(CI_COMPONENTS_GAP);
        panel.paddingBuilder().verticalSet(STATUS_COMPONENT_BORDER).apply();
        // Also show when loading
        ReviewUIUtil.bind(parentDisposable, hasConflicts, it -> panel.setVisible(it == null || it));
        panel.add(title);
        panel.add(resolveLink);
        return panel;
    }

    @RequiredUIAccess
    @Override
    public Component createMergeClarificationComponent(Disposable parentDisposable, ObservableValue<String> clarification) {
        Label label = Label.create();
        HorizontalLayout panel = HorizontalLayout.create(Space.NONE);
        panel.paddingBuilder().verticalSet(STATUS_COMPONENT_BORDER).apply();
        panel.add(label);
        panel.setVisible(false);
        ReviewUIUtil.bind(parentDisposable, clarification, text -> {
            label.setText(LocalizeValue.of(text));
            panel.setVisible(!text.isEmpty());
        });
        return panel;
    }

    @RequiredUIAccess
    @Override
    public <T> Component createNeedReviewerComponent(Disposable parentDisposable, ObservableValue<Map<T, ReviewState>> reviewersReview) {
        Label label = createStatusLabel(CIBuildStatusIcons.warning(), ReviewLocalize.reviewDetailsStatusReviewerMissing());
        ReviewUIUtil.bind(parentDisposable, reviewersReview, it -> label.setVisible(it.isEmpty()));
        return label;
    }

    @RequiredUIAccess
    @Override
    public Component createRequiredReviewsComponent(Disposable parentDisposable,
                                                    ObservableValue<Integer> requiredApprovingReviewsCount,
                                                    ObservableValue<Boolean> isDraft) {
        Label label = createStatusLabel(CIBuildStatusIcons.failed(), LocalizeValue.empty());
        ReviewUIUtil.bind(parentDisposable, requiredApprovingReviewsCount, isDraft, (count, draft) ->
            label.setVisible(count > 0 && !draft));
        ReviewUIUtil.bind(parentDisposable, requiredApprovingReviewsCount, count ->
            label.setText(ReviewLocalize.reviewDetailsStatusReviewerRequired(count)));
        return label;
    }

    @RequiredUIAccess
    @Override
    public Component createRequiredResolveConversationsComponent(Disposable parentDisposable,
                                                                 ObservableValue<Boolean> hasUnresolvedConversations) {
        Label label = createStatusLabel(CIBuildStatusIcons.failed(), ReviewLocalize.reviewDetailsStatusConversations());
        label.setVisible(false);
        ReviewUIUtil.bind(parentDisposable, hasUnresolvedConversations, label::setVisible);
        return label;
    }

    @RequiredUIAccess
    @Override
    public Component createRestrictionComponent(Disposable parentDisposable,
                                                ObservableValue<Boolean> isRestricted,
                                                ObservableValue<Boolean> isDraft) {
        Label label = createStatusLabel(CIBuildStatusIcons.failed(), ReviewLocalize.reviewDetailsStatusNotAuthorizedToMerge());
        ReviewUIUtil.bind(parentDisposable, isRestricted, isDraft, (restricted, draft) -> label.setVisible(restricted && !draft));
        return label;
    }

    @RequiredUIAccess
    @Override
    public Component createCiComponent(Disposable parentDisposable, CodeReviewStatusViewModel statusVm) {
        Label title = Label.create();

        Hyperlink detailsLink = Hyperlink.create(ReviewLocalize.reviewDetailsStatusCiLinkDetails(), event -> statusVm.showJobsDetails());

        HorizontalLayout panel = HorizontalLayout.create(CI_COMPONENTS_GAP);
        panel.paddingBuilder().verticalSet(STATUS_COMPONENT_BORDER).apply();
        panel.add(title);
        panel.add(detailsLink);

        ReviewUIUtil.bind(parentDisposable, statusVm.getCiJobs(), jobs -> {
            title.setImage(calcPipelineIcon(jobs));
            title.setText(calcPipelineText(jobs));
            panel.setVisible(!jobs.isEmpty());
        });

        UIAccess uiAccess = UIAccess.current();
        statusVm.addShowJobsDetailsRequestListener(parentDisposable, jobs -> uiAccess.give(() -> showJobsPopup(detailsLink, jobs)));
        return panel;
    }

    @RequiredUIAccess
    @Override
    public <Reviewer, ReviewStateType> Component createReviewersReviewStateComponent(
        Disposable parentDisposable,
        ObservableValue<Map<Reviewer, ReviewStateType>> reviewersReview,
        BiFunction<Reviewer, ReviewStateType, LocalizeValue> statusText,
        BiFunction<Reviewer, ReviewStateType, Component> avatar,
        BiFunction<Reviewer, ReviewStateType, @Nullable Image> statusIcon,
        BiFunction<Reviewer, ReviewStateType, @Nullable ActionGroup> actionGroup
    ) {
        VerticalLayout panel = VerticalLayout.create(Space.NONE);
        ReviewUIUtil.bind(parentDisposable, reviewersReview, reviews -> {
            panel.removeAll();
            reviews.forEach((reviewer, reviewState) -> panel.add(createReviewerReviewStatus(
                statusText.apply(reviewer, reviewState),
                avatar.apply(reviewer, reviewState),
                statusIcon.apply(reviewer, reviewState),
                actionGroup.apply(reviewer, reviewState)
            )));
            panel.setVisible(!reviews.isEmpty());
        });
        return panel;
    }

    @RequiredUIAccess
    private static Component createReviewerReviewStatus(LocalizeValue statusText,
                                                        Component avatar,
                                                        @Nullable Image statusIcon,
                                                        @Nullable ActionGroup actionGroup) {
        HorizontalLayout panel = HorizontalLayout.create(STATUS_REVIEWER_COMPONENT_GAP);
        panel.paddingBuilder().verticalSet(STATUS_REVIEWER_BORDER).apply();

        if (statusIcon != null) {
            Label statusLabel = Label.create();
            statusLabel.setImage(statusIcon);
            panel.add(statusLabel);
        }

        panel.add(avatar);
        panel.add(Label.create(statusText));

        if (actionGroup != null) {
            panel.addContextMenuListener(event -> {
                ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(REVIEWER_STATUS_PLACE, actionGroup);
                menu.setTargetComponent(panel);
                menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
            });
        }
        return panel;
    }

    @RequiredUIAccess
    private static Label createStatusLabel(Image icon, LocalizeValue text) {
        Label label = Label.create(text);
        label.setImage(icon);
        return label;
    }

    @RequiredUIAccess
    private static void showJobsPopup(Component anchor, List<CodeReviewCIJob> jobs) {
        BaseListPopupStep<CodeReviewCIJob> step = new BaseListPopupStep<>(ReviewLocalize.reviewDetailsStatusCiPopupTitle().get(), jobs) {
            @Override
            public String getTextFor(CodeReviewCIJob job) {
                return job.name();
            }

            @Override
            public Image getIconFor(CodeReviewCIJob job) {
                return convertToIcon(job.status());
            }

            @Override
            public PopupStep onChosen(CodeReviewCIJob job, boolean finalChoice) {
                String url = job.detailsUrl();
                if (url != null) {
                    Platform.current().openInBrowser(url);
                }
                return FINAL_CHOICE;
            }
        };
        ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
        popup.showBy(anchor, null, PopupPosition.TOP);
    }

    private static Image calcPipelineIcon(List<CodeReviewCIJob> jobs) {
        long failed = jobs.stream().filter(it -> it.status() == CodeReviewCIJobState.FAILED).count();
        long pending = jobs.stream().filter(it -> it.status() == CodeReviewCIJobState.PENDING).count();
        if (jobs.stream().filter(CodeReviewCIJob::isRequired).allMatch(it -> it.status() == CodeReviewCIJobState.SUCCESS)) {
            return CIBuildStatusIcons.success();
        }
        if (pending != 0 && failed != 0) {
            return CIBuildStatusIcons.failedInProgress();
        }
        if (pending != 0) {
            return CIBuildStatusIcons.pending();
        }
        return CIBuildStatusIcons.failed();
    }

    private static LocalizeValue calcPipelineText(List<CodeReviewCIJob> jobs) {
        long failed = jobs.stream().filter(it -> it.status() == CodeReviewCIJobState.FAILED).count();
        long pending = jobs.stream().filter(it -> it.status() == CodeReviewCIJobState.PENDING).count();
        if (jobs.stream().filter(CodeReviewCIJob::isRequired).allMatch(it -> it.status() == CodeReviewCIJobState.SUCCESS)) {
            return ReviewLocalize.reviewDetailsStatusCiPassed();
        }
        if (pending != 0 && failed != 0) {
            return ReviewLocalize.reviewDetailsStatusCiProgressAndFailed();
        }
        if (pending != 0) {
            return ReviewLocalize.reviewDetailsStatusCiProgress();
        }
        return ReviewLocalize.reviewDetailsStatusCiFailed();
    }

    private static Image convertToIcon(CodeReviewCIJobState state) {
        return switch (state) {
            case FAILED -> CIBuildStatusIcons.failed();
            case PENDING -> CIBuildStatusIcons.pending();
            case SKIPPED -> CIBuildStatusIcons.skipped();
            case SUCCESS -> CIBuildStatusIcons.success();
        };
    }
}
