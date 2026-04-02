// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangesViewModel;
import com.intellij.collaboration.ui.codereview.list.search.ChooserPopupUtil;
import com.intellij.collaboration.ui.codereview.list.search.PopupConfig;
import com.intellij.collaboration.ui.util.BindingUtilKt;
import consulo.application.AllIcons;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.ui.ex.awt.JBFont;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.flow.FlowKt;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class CodeReviewDetailsCommitsComponentFactory {
    private static final int COMPONENTS_GAP = 4;
    private static final int COMMIT_HASH_OFFSET = 8;
    static final int VERT_PADDING = 6;

    private CodeReviewDetailsCommitsComponentFactory() {
    }

    public static <T> @Nonnull JComponent create(
        @Nonnull CoroutineScope scope,
        @Nonnull CodeReviewChangesViewModel<T> changesVm,
        @Nonnull Function<T, CommitPresentation> commitPresentation
    ) {
        JLabel commitsPopupTitle = new JLabel();
        commitsPopupTitle.setFont(JBFont.regular().asBold());
        BindingUtilKt.bindTextIn(
            commitsPopupTitle,
            scope,
            FlowKt.map(
                changesVm.getReviewCommits(),
                commits -> CollaborationToolsLocalize.reviewDetailsCommitsTitleText(((List<?>) commits).size()).get()
            )
        );

        JComponent commitsPopup = createCommitChooserActionLink(scope, changesVm, commitPresentation);

        var nextPrevVisibilityFlow = FlowKt.combine(
            changesVm.getSelectedCommitIndex(),
            changesVm.getReviewCommits(),
            (selectedIdx, commits) -> ((List<?>) commits).size() > 1 && (int) selectedIdx >= 0
        );

        InlineIconButton nextCommitIcon = new InlineIconButton(AllIcons.Chooser.Bottom);
        nextCommitIcon.setWithBackgroundHover(true);
        nextCommitIcon.setActionListener(e -> changesVm.selectNextCommit());
        nextCommitIcon.setVisible(false);
        BindingUtilKt.bindVisibilityIn(nextCommitIcon, scope, nextPrevVisibilityFlow);
        BindingUtilKt.bindDisabledIn(nextCommitIcon, scope, FlowKt.combine(changesVm.getSelectedCommitIndex(), changesVm.getReviewCommits(),
            (selectedIdx, commits) -> (int) selectedIdx == ((List<?>) commits).size() - 1
        ));

        InlineIconButton previousCommitIcon = new InlineIconButton(AllIcons.Chooser.Top);
        previousCommitIcon.setWithBackgroundHover(true);
        previousCommitIcon.setActionListener(e -> changesVm.selectPreviousCommit());
        previousCommitIcon.setVisible(false);
        BindingUtilKt.bindVisibilityIn(previousCommitIcon, scope, nextPrevVisibilityFlow);
        BindingUtilKt.bindDisabledIn(previousCommitIcon, scope, FlowKt.map(changesVm.getSelectedCommitIndex(), idx -> (int) idx == 0));

        HorizontalListPanel panel = new HorizontalListPanel(COMPONENTS_GAP);
        // should be 6 top and bottom, but labels are height 17 instead of 16
        panel.setBorder(JBUI.Borders.empty(VERT_PADDING, 0, VERT_PADDING - 1, 0));
        panel.add(commitsPopupTitle);
        panel.add(commitsPopup);
        panel.add(nextCommitIcon);
        panel.add(previousCommitIcon);
        return panel;
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nonnull JComponent createCommitChooserActionLink(
        @Nonnull CoroutineScope scope,
        @Nonnull CodeReviewChangesViewModel<T> changesVm,
        @Nonnull Function<T, CommitPresentation> commitPresentation
    ) {
        ActionLink link = new ActionLink();
        link.setHorizontalAlignment(SwingConstants.RIGHT);
        link.setDropDownLinkIcon();
        BindingUtilKt.bindTextIn(link, scope, FlowKt.combine(changesVm.getSelectedCommit(), changesVm.getReviewCommits(),
            (selectedCommit, commits) -> {
                if (selectedCommit != null) {
                    FontMetrics metrics = link.getFontMetrics(link.getFont());
                    int commitHashWidth = calculateCommitHashWidth(metrics, (List<T>) commits, changesVm::commitHash);
                    link.setPreferredSize(new JBDimension(commitHashWidth, link.getPreferredSize().height, true));
                    return changesVm.commitHash((T) selectedCommit);
                }
                else {
                    link.setPreferredSize(null);
                    return CollaborationToolsLocalize.reviewDetailsCommitsPopupText(((List<?>) commits).size()).get();
                }
            }
        ));
        BindingUtilKt.bindDisabledIn(
            link,
            scope,
            FlowKt.map(changesVm.getReviewCommits(), commits -> ((List<?>) commits).size() <= 1)
        );
        link.addActionListener(createCommitPopupAction(scope, changesVm, commitPresentation));
        return link;
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nonnull ActionListener createCommitPopupAction(
        @Nonnull CoroutineScope scope,
        @Nonnull CodeReviewChangesViewModel<T> changesVm,
        @Nonnull Function<T, CommitPresentation> commitPresentation
    ) {
        return event -> {
            JComponent parentComponent = event.getSource() instanceof JComponent ? (JComponent) event.getSource() : null;
            if (parentComponent == null) {
                return;
            }
            RelativePoint point = RelativePoint.getSouthWestOf(parentComponent);
            kotlinx.coroutines.BuildersKt.launch(
                scope,
                scope.getCoroutineContext(),
                kotlinx.coroutines.CoroutineStart.DEFAULT,
                (coroutineScope, continuation) -> {
                    List<T> commits = (List<T>) FlowKt.first(changesVm.getReviewCommits(), continuation);
                    int commitsCount = commits.size();
                    T selectedCommit = ((kotlinx.coroutines.flow.StateFlow<T>) FlowKt.stateIn(
                        changesVm.getSelectedCommit(),
                        coroutineScope,
                        kotlinx.coroutines.flow.SharingStarted.Companion.getEagerly(),
                        null
                    )).getValue();
                    List<T> popupItems = new ArrayList<>();
                    popupItems.add(null);
                    popupItems.addAll(commits);
                    T chosenCommit = ChooserPopupUtil.showChooserPopup(
                        point,
                        popupItems,
                        commit -> {
                            if (commit == null) {
                                return CollaborationToolsLocalize.reviewDetailsCommitsPopupAll(commitsCount).get();
                            }
                            else {
                                return commitPresentation.apply(commit).getTitleHtml();
                            }
                        },
                        CommitRenderer.createCommitRenderer(commitsCount, commit -> {
                            CommitPresentation pres = commit != null ? commitPresentation.apply(commit) : null;
                            return new SelectableWrapper<>(pres, commit != null && commit.equals(selectedCommit));
                        }),
                        new PopupConfig(CollaborationToolsLocalize.reviewDetailsCommitsSearchPlaceholder().get())
                    );
                    int index = chosenCommit != null ? commits.indexOf(chosenCommit) : -1;
                    changesVm.selectCommit(index);
                    return kotlin.Unit.INSTANCE;
                }
            );
        };
    }

    private static <T> int calculateCommitHashWidth(
        @Nonnull FontMetrics metrics, @Nonnull List<T> commits,
        @Nonnull Function<T, String> commitHashConverter
    ) {
        if (commits.isEmpty()) {
            throw new IllegalArgumentException("commits must not be empty");
        }
        int longestCommitHash = commits.stream()
            .mapToInt(commit -> metrics.stringWidth(commitHashConverter.apply(commit)))
            .max()
            .orElse(0);
        return longestCommitHash + AllIcons.General.LinkDropTriangle.getIconWidth() + COMMIT_HASH_OFFSET;
    }
}
