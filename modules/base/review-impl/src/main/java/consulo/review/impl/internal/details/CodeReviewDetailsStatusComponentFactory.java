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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.review.details.data.CodeReviewCIJob;
import consulo.review.details.data.CodeReviewCIJobState;
import consulo.review.details.model.CodeReviewStatusViewModel;
import consulo.review.icon.CIBuildStatusIcons;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;

import java.util.List;

/**
 * The status rows of a review details tab - its CI checks and whether it has conflicts.
 */
public final class CodeReviewDetailsStatusComponentFactory {
    private static final Space STATUS_COMPONENT_BORDER = Space.SMALL;
    private static final Space CI_COMPONENTS_GAP = Space.LARGE;

    private CodeReviewDetailsStatusComponentFactory() {
    }

    @RequiredUIAccess
    public static Component createConflictsComponent(Disposable parentDisposable, CodeReviewStatusViewModel statusVm) {
        Label title = Label.create();

        HorizontalLayout panel = HorizontalLayout.create(CI_COMPONENTS_GAP);
        panel.paddingBuilder().verticalSet(STATUS_COMPONENT_BORDER).apply();
        panel.add(title);

        ReviewUIUtil.bind(parentDisposable, statusVm.getHasConflicts(), hasConflicts -> {
            title.setImage(hasConflicts == null ? CIBuildStatusIcons.pending() : CIBuildStatusIcons.failed());
            title.setText(hasConflicts == null
                ? ReviewLocalize.reviewDetailsStatusConflictsPending()
                : ReviewLocalize.reviewDetailsStatusConflicts());
            // Also show when loading
            panel.setVisible(hasConflicts == null || hasConflicts);
        });
        return panel;
    }

    @RequiredUIAccess
    public static Component createCiComponent(Disposable parentDisposable, CodeReviewStatusViewModel statusVm) {
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
