// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import com.intellij.collaboration.async.LaunchNowKt;
import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.SingleValueModel;
import com.intellij.collaboration.ui.codereview.CodeReviewTitleUIUtil;
import com.intellij.collaboration.ui.codereview.details.data.ReviewRequestState;
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewDetailsViewModel;
import com.intellij.collaboration.ui.util.BindingUtilKt;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awt.PopupHandler;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.FlowKt;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Nls;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.util.function.Supplier;

public final class CodeReviewDetailsTitleComponentFactory {
    private CodeReviewDetailsTitleComponentFactory() {
    }

    public static @Nonnull JComponent create(
        @Nonnull CoroutineScope scope,
        @Nonnull CodeReviewDetailsViewModel detailsVm,
        @Nonnull @Nls String urlTooltip,
        @Nonnull ActionGroup actionGroup,
        @Nonnull Supplier<JEditorPane> htmlPaneFactory
    ) {
        JEditorPane titleLabel = htmlPaneFactory.get();
        titleLabel.setName("Review details title panel");
        titleLabel.setFont(JBFont.h2().asBold());
        BindingUtilKt.bindTextHtmlIn(
            titleLabel,
            scope,
            FlowKt.map(
                detailsVm.getTitle(),
                title -> CodeReviewTitleUIUtil.createTitleText(
                    title,
                    detailsVm.getNumber(),
                    detailsVm.getUrl(),
                    urlTooltip
                )
            )
        );
        PopupHandler.installPopupMenu(titleLabel, actionGroup, "CodeReviewDetailsPopup");

        SingleValueModel<@Nls String> stateTextModel = new SingleValueModel<>(null);
        LaunchNowKt.launchNow(
            scope,
            (coroutineScope, continuation) -> FlowKt.collect(
                detailsVm.getReviewRequestState(),
                reviewRequestState -> {
                    stateTextModel.setValue(ReviewDetailsUIUtil.getRequestStateText((ReviewRequestState) reviewRequestState));
                    return kotlin.Unit.INSTANCE;
                },
                continuation
            )
        );

        JComponent stateLabel = CollaborationToolsUIUtil.createTagLabel(stateTextModel);
        BindingUtilKt.bindVisibilityIn(
            stateLabel,
            scope,
            FlowKt.map(detailsVm.getReviewRequestState(), state -> state != ReviewRequestState.OPENED)
        );

        LC lc = new LC().fillX().hideMode(3);
        lc.setInsets("0 0 0 0");
        JPanel panel = new JPanel(new MigLayout(lc, new AC().gap("push")));
        panel.setOpaque(false);
        panel.add(titleLabel);
        panel.add(stateLabel, new CC().alignY("top"));

        return panel;
    }
}
