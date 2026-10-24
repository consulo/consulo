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
package consulo.versionControlSystem.review.impl.internal.details;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.review.icon.ReviewIconGroup;
import consulo.review.util.ReviewUIUtil;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.PopupPosition;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.layout.HorizontalLayout;
import consulo.versionControlSystem.review.details.CodeReviewDetailsBranchComponentFactory;
import consulo.versionControlSystem.review.details.model.CodeReviewBranches;
import consulo.versionControlSystem.review.details.model.CodeReviewBranchesViewModel;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Singleton
@ServiceImpl
public class CodeReviewDetailsBranchComponentFactoryImpl implements CodeReviewDetailsBranchComponentFactory {
    private static final Space BRANCH_ICON_LINK_GAP = Space.X_SMALL;

    private enum ReviewAction {
        CHECKOUT,
        CHECKOUT_IN_NEW_WORKTREE,
        SHOW_IN_LOG,
        COPY_BRANCH_NAME
    }

    @RequiredUIAccess
    @Override
    public Component create(Disposable parentDisposable, CodeReviewBranchesViewModel branchesVm) {
        Hyperlink sourceBranch = Hyperlink.create(LocalizeValue.empty());
        sourceBranch.addHyperlinkListener(event -> branchesVm.showBranches());
        ReviewUIUtil.bind(parentDisposable, branchesVm.isCheckedOut(), isCheckedOut ->
            sourceBranch.setIcon(isCheckedOut ? ReviewIconGroup.reviewCurrentbranchlabel() : ReviewIconGroup.reviewBranchlabel()));
        ReviewUIUtil.bind(parentDisposable, branchesVm.getSourceBranch(), branch -> {
            sourceBranch.setText(LocalizeValue.of(branch));
            sourceBranch.setToolTipText(LocalizeValue.of(branch));
        });

        HorizontalLayout panelWithIcon = HorizontalLayout.create(BRANCH_ICON_LINK_GAP);
        panelWithIcon.paddingBuilder().verticalSet(CodeReviewDetailsCommitsComponentFactoryImpl.VERT_PADDING).apply();
        panelWithIcon.add(sourceBranch);

        UIAccess uiAccess = UIAccess.current();
        branchesVm.addShowBranchesRequestListener(parentDisposable, branches ->
            uiAccess.give(() -> showBranchesPopup(panelWithIcon, branchesVm, branches)));
        return panelWithIcon;
    }

    @RequiredUIAccess
    private static void showBranchesPopup(Component anchor, CodeReviewBranchesViewModel branchesVm, CodeReviewBranches branches) {
        String source = branches.source();
        String target = branches.target();
        boolean hasRemoteBranch = branches.hasRemoteBranch();

        List<ReviewAction> actions = new ArrayList<>();
        actions.add(ReviewAction.CHECKOUT);
        if (branchesVm.canCheckoutInNewWorktree()) {
            actions.add(ReviewAction.CHECKOUT_IN_NEW_WORKTREE);
        }
        if (branchesVm.canShowInLog()) {
            actions.add(ReviewAction.SHOW_IN_LOG);
        }
        actions.add(ReviewAction.COPY_BRANCH_NAME);

        BaseListPopupStep<ReviewAction> step = new BaseListPopupStep<>(null, actions) {
            @Override
            public String getTextFor(ReviewAction action) {
                return switch (action) {
                    case CHECKOUT -> hasRemoteBranch
                        ? ReviewLocalize.reviewDetailsBranchCheckoutRemote().get()
                        : ReviewLocalize.reviewDetailsBranchCheckoutRemoteAsDetachedHead().get();
                    case CHECKOUT_IN_NEW_WORKTREE -> ReviewLocalize.reviewDetailsBranchCheckoutInNewWorktree().get();
                    case SHOW_IN_LOG -> ReviewLocalize.reviewDetailsBranchShowRemoteInGitLog().get();
                    case COPY_BRANCH_NAME -> ReviewLocalize.reviewDetailsBranchCopyName().get();
                };
            }

            @Override
            public @Nullable PopupStep<?> onChosen(ReviewAction action, boolean finalChoice) {
                return doFinalStep(() -> {
                    switch (action) {
                        case CHECKOUT -> branchesVm.fetchAndCheckoutRemoteBranch();
                        case CHECKOUT_IN_NEW_WORKTREE -> branchesVm.checkoutInNewWorktree();
                        case SHOW_IN_LOG -> branchesVm.fetchAndShowInLog();
                        case COPY_BRANCH_NAME -> UIAccess.current().getClipboard().setText(source);
                    }
                });
            }
        };

        ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
        popup.setAdText(hasRemoteBranch
            ? ReviewLocalize.reviewDetailsBranchCheckoutRemoteAdLabel(target, source).get()
            : ReviewLocalize.reviewDetailsBranchCannotCheckoutAsBranch().get());
        popup.showBy(anchor, null, PopupPosition.BOTTOM);
    }
}
