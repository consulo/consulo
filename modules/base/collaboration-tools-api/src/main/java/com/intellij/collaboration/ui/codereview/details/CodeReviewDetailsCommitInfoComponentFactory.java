// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import com.intellij.collaboration.ui.HtmlEditorPaneUtil;
import com.intellij.collaboration.ui.layout.SizeRestrictedSingleComponentLayout;
import com.intellij.collaboration.ui.util.BindingUtilKt;
import com.intellij.collaboration.ui.util.DimensionRestrictions;
import consulo.application.util.DateFormatUtil;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.FlowKt;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlowKt;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;
import java.util.function.Supplier;

public final class CodeReviewDetailsCommitInfoComponentFactory {
    private static final int GAP = 8;

    private CodeReviewDetailsCommitInfoComponentFactory() {
    }

    public static <T> @Nonnull JComponent create(
        @Nonnull CoroutineScope scope,
        @Nonnull Flow<T> commit,
        @Nonnull Function<T, CommitPresentation> commitPresentation,
        @Nonnull Supplier<JEditorPane> htmlPaneFactory
    ) {
        MutableStateFlow<Boolean> withDetailedInfo = StateFlowKt.MutableStateFlow(false);

        JEditorPane title = htmlPaneFactory.get();
        JEditorPane description = htmlPaneFactory.get();
        JEditorPane info = htmlPaneFactory.get();
        info.setForeground(NamedColorUtil.getInactiveTextColor());

        ActionLink detailsAction = new ActionLink(
            CollaborationToolsLocalize.reviewDetailsCommitsDetailsShow().get(),
            e -> withDetailedInfo.setValue(!withDetailedInfo.getValue())
        );
        detailsAction.setVerticalAlignment(SwingConstants.TOP);

        DimensionRestrictions.LinesHeight singleTitleRestriction = new DimensionRestrictions.LinesHeight(title, 1);
        SizeRestrictedSingleComponentLayout titleLayout = new SizeRestrictedSingleComponentLayout();
        titleLayout.setMaxSize(singleTitleRestriction);
        JComponent topPanel = createTopPanel(title, detailsAction, titleLayout);

        VerticalListPanel panel = new VerticalListPanel(GAP);
        BindingUtilKt.bindVisibilityIn(panel, scope, FlowKt.map(commit, it -> it != null));
        panel.add(topPanel);
        panel.add(description);
        panel.add(info);

        kotlinx.coroutines.BuildersKt.launch(
            scope,
            scope.getCoroutineContext(),
            kotlinx.coroutines.CoroutineStart.DEFAULT,
            (coroutineScope, continuation) -> FlowKt.collect(
                withDetailedInfo,
                isDetailed -> {
                    description.setVisible((boolean) isDetailed);
                    info.setVisible((boolean) isDetailed);
                    titleLayout.setMaxSize((boolean) isDetailed ? DimensionRestrictions.None.INSTANCE : singleTitleRestriction);
                    detailsAction.setText(
                        (boolean) isDetailed
                            ? CollaborationToolsLocalize.reviewDetailsCommitsDetailsHide().get()
                            : CollaborationToolsLocalize.reviewDetailsCommitsDetailsShow().get()
                    );
                    return kotlin.Unit.INSTANCE;
                },
                continuation
            )
        );

        kotlinx.coroutines.BuildersKt.launch(
            scope,
            scope.getCoroutineContext(),
            kotlinx.coroutines.CoroutineStart.DEFAULT,
            (coroutineScope, continuation) -> FlowKt.collect(commit, commitValue -> {
                    if (commitValue == null) {
                        return kotlin.Unit.INSTANCE;
                    }
                    @SuppressWarnings("unchecked")
                    CommitPresentation presentation = commitPresentation.apply((T) commitValue);
                    HtmlEditorPaneUtil.setHtmlBody(title, presentation.getTitleHtml());
                    HtmlEditorPaneUtil.setHtmlBody(description, presentation.getDescriptionHtml());
                    HtmlEditorPaneUtil.setHtmlBody(
                        info,
                        presentation.getAuthor() + ", " + DateFormatUtil.formatPrettyDateTime(presentation.getCommittedDate())
                    );
                    return kotlin.Unit.INSTANCE;
                },
                continuation
            )
        );

        return panel;
    }

    private static @Nonnull JComponent createTopPanel(
        @Nonnull JComponent titlePanel,
        @Nonnull ActionLink detailsAction,
        @Nonnull SizeRestrictedSingleComponentLayout layout
    ) {
        JPanel wrappedTitle = new JPanel(layout);
        wrappedTitle.setOpaque(false);
        wrappedTitle.add(titlePanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(wrappedTitle, BorderLayout.CENTER);
        panel.add(detailsAction, BorderLayout.EAST);
        return panel;
    }
}
