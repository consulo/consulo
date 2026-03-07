// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewDetailsViewModel;
import com.intellij.collaboration.ui.util.BindingUtilKt;
import com.intellij.collaboration.ui.util.DimensionRestrictions;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.awt.PopupHandler;
import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CodeReviewDetailsDescriptionComponentFactory {
    private static final int VISIBLE_DESCRIPTION_LINES = 2;

    private CodeReviewDetailsDescriptionComponentFactory() {
    }

    public static @Nonnull JComponent create(
        @Nonnull CoroutineScope scope,
        @Nonnull CodeReviewDetailsViewModel detailsVm,
        @Nonnull ActionGroup actionGroup,
        @Nonnull Consumer<JComponent> showTimelineAction,
        @Nonnull Supplier<JEditorPane> htmlPaneFactory
    ) {
        JEditorPane descriptionPane = htmlPaneFactory.get();
        Flow<String> description = detailsVm.getDescription();
        if (description != null) {
            BindingUtilKt.bindTextIn(descriptionPane, scope, description);
        }
        PopupHandler.installPopupMenu(descriptionPane, actionGroup, "CodeReviewDetailsPopup");
        JComponent descriptionPanel = CollaborationToolsUIUtil.wrapWithLimitedSize(
            descriptionPane, new DimensionRestrictions.LinesHeight(descriptionPane, VISIBLE_DESCRIPTION_LINES)
        );

        ActionLink timelineLink = new ActionLink(
            CollaborationToolsLocalize.reviewDetailsViewTimelineAction().get(),
            e -> showTimelineAction.accept((JComponent) e.getSource())
        );
        timelineLink.setBorder(JBUI.Borders.emptyTop(4));

        VerticalListPanel panel = new VerticalListPanel();
        panel.setName("Review details description panel");
        panel.add(descriptionPanel);
        panel.add(timelineLink);

        return panel;
    }
}
