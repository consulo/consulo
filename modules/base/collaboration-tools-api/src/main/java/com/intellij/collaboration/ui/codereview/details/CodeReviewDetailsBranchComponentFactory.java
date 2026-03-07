// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import com.intellij.collaboration.async.LaunchNowKt;
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewBranches;
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewBranchesViewModel;
import com.intellij.collaboration.ui.util.BindingUtilKt;
import com.intellij.collaboration.ui.util.popup.PopupItemPresentation;
import com.intellij.collaboration.ui.util.popup.SimplePopupItemRenderer;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.FlowKt;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public final class CodeReviewDetailsBranchComponentFactory {
    private static final int BRANCH_ICON_LINK_GAP = 2;

    private CodeReviewDetailsBranchComponentFactory() {
    }

    public static @Nonnull JComponent create(
        @Nonnull CoroutineScope scope,
        @Nonnull CodeReviewBranchesViewModel branchesVm
    ) {
        InlineIconButton statusIcon = new InlineIconButton(DvcsImplIcons.BranchLabel);
        statusIcon.setActionListener(e -> branchesVm.showBranches());
        LaunchNowKt.launchNow(
            scope,
            (coroutineScope, continuation) -> FlowKt.collect(
                branchesVm.getIsCheckedOut(),
                isCheckedOut -> {
                    Icon icon;
                    if (!(boolean) isCheckedOut) {
                        icon = DvcsImplIcons.BranchLabel;
                    }
                    else if (ExperimentalUI.isNewUI()) {
                        icon = DvcsImplIcons.CurrentBranchLabel;
                    }
                    else {
                        icon = DvcsImplIcons.CurrentBranchFavoriteLabel;
                    }
                    statusIcon.setIcon(icon);
                    return kotlin.Unit.INSTANCE;
                },
                continuation
            )
        );

        ActionLink sourceBranch = new ActionLink();
        sourceBranch.addActionListener(e -> branchesVm.showBranches());
        sourceBranch.setDropDownLinkIcon();
        BindingUtilKt.bindTextIn(sourceBranch, scope, branchesVm.getSourceBranch());
        BindingUtilKt.bindTooltipTextIn(sourceBranch, scope, branchesVm.getSourceBranch());
        sourceBranch.setMinimumSize(new Dimension(0, 0));

        HorizontalListPanel panelWithIcon = new HorizontalListPanel(BRANCH_ICON_LINK_GAP);
        panelWithIcon.setBorder(JBUI.Borders.empty(
            CodeReviewDetailsCommitsComponentFactory.VERT_PADDING,
            0,
            CodeReviewDetailsCommitsComponentFactory.VERT_PADDING - 1,
            0
        ));
        panelWithIcon.add(statusIcon);
        panelWithIcon.add(sourceBranch);

        LaunchNowKt.launchNow(
            scope,
            (coroutineScope, continuation) -> FlowKt.collectLatest(
                branchesVm.getShowBranchesRequests(),
                branchesObj -> {
                    CodeReviewBranches branches = (CodeReviewBranches) branchesObj;
                    String source = branches.getSource();
                    String target = branches.getTarget();
                    boolean hasRemoteBranch = branches.getHasRemoteBranch();

                    RelativePoint point = RelativePoint.getSouthWestOf(panelWithIcon);

                    JComponent advertiser;
                    if (!hasRemoteBranch) {
                        advertiser = HintUtil.createAdComponent(
                            CollaborationToolsLocalize.reviewDetailsBranchCannotCheckoutAsBranch().get(),
                            JBUI.CurrentTheme.Advertiser.border(), SwingConstants.LEFT
                        );
                        advertiser.setIcon(PlatformIconGroup.generalWarning());
                    }
                    else {
                        advertiser = HintUtil.createAdComponent(
                            CollaborationToolsLocalize.reviewDetailsBranchCheckoutRemoteAdLabel(target, source).get(),
                            JBUI.CurrentTheme.Advertiser.border(), SwingConstants.LEFT
                        );
                    }

                    List<ReviewAction> actions = new ArrayList<>();
                    actions.add(ReviewAction.Checkout.INSTANCE);
                    if (branchesVm.getCanShowInLog()) {
                        actions.add(ReviewAction.ShowInLog.INSTANCE);
                    }
                    actions.add(ReviewAction.CopyBranchName.INSTANCE);

                    ListCellRenderer<ReviewAction> renderer = popupActionsRenderer(source, hasRemoteBranch);

                    JBPopupFactory.getInstance().createPopupChooserBuilder(actions)
                        .setRenderer(renderer)
                        .setAdvertiser(advertiser)
                        .setItemChosenCallback(action -> {
                            if (action instanceof ReviewAction.Checkout) {
                                branchesVm.fetchAndCheckoutRemoteBranch();
                            }
                            else if (action instanceof ReviewAction.ShowInLog) {
                                branchesVm.fetchAndShowInLog();
                            }
                            else if (action instanceof ReviewAction.CopyBranchName) {
                                CopyPasteManager.getInstance().setContents(new StringSelection(source));
                            }
                        })
                        .createPopup()
                        .show(point); // Note: showAndAwait is a suspend-specific API; using show here

                    return kotlin.Unit.INSTANCE;
                },
                continuation
            )
        );

        return panelWithIcon;
    }

    private static @Nonnull ListCellRenderer<ReviewAction> popupActionsRenderer(
        @Nonnull String sourceBranch, boolean hasRemoteBranch
    ) {
        return SimplePopupItemRenderer.create(item -> {
            if (item instanceof ReviewAction.Checkout) {
                return new PopupItemPresentation.Simple(
                    hasRemoteBranch
                        ? CollaborationToolsLocalize.reviewDetailsBranchCheckoutRemote(sourceBranch).get()
                        : CollaborationToolsLocalize.reviewDetailsBranchCheckoutRemoteAsDetachedHead(sourceBranch).get()
                );
            }
            else if (item instanceof ReviewAction.ShowInLog) {
                return new PopupItemPresentation.Simple(
                    CollaborationToolsLocalize.reviewDetailsBranchShowRemoteInGitLog(sourceBranch).get()
                );
            }
            else if (item instanceof ReviewAction.CopyBranchName) {
                return new PopupItemPresentation.Simple(
                    CollaborationToolsLocalize.reviewDetailsBranchCopyName().get()
                );
            }
            throw new IllegalArgumentException("Unknown ReviewAction: " + item);
        });
    }
}
