// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import com.intellij.collaboration.ui.CollaborationToolsUIUtil;
import consulo.application.util.DateFormatUtil;
import consulo.collaboration.localize.CollaborationToolsLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.*;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

public final class CommitRenderer<T> implements ListCellRenderer<T> {
    private static final int ALL_COMMITS_TOP_BOTTOM = 4;
    private static final int COMMIT_TOP_BOTTOM = 8;
    private static final int ICON_TEXT_OFFSET = 6;
    private static final int MESSAGE_INFO_VERTICAL_GAP = 4;
    private static final EmptyIcon emptyIcon = JBUIScale.scaleIcon(EmptyIcon.create(PlatformIconGroup.actionsChecked_selected()));

    private final int commitsCount;
    private final Function<T, SelectableWrapper<CommitPresentation>> presenter;

    private final JLabel allCommitsMessage;
    private final JLabel commitMessageIcon;
    private final JBLabel commitMessageText;
    private final JPanel commitMessagePanel;
    private final JLabel authorAndDate;
    private final BorderLayoutPanel textPanel;
    private final BorderLayoutPanel commitPanel;

    private CommitRenderer(int commitsCount, @Nonnull Function<T, SelectableWrapper<CommitPresentation>> presenter) {
        this.commitsCount = commitsCount;
        this.presenter = presenter;

        allCommitsMessage = new JLabel();
        allCommitsMessage.setBorder(JBUI.Borders.empty(ALL_COMMITS_TOP_BOTTOM, getLeftRightGap()));
        allCommitsMessage.setIconTextGap(JBUIScale.scale(ICON_TEXT_OFFSET));
        allCommitsMessage.setText(CollaborationToolsLocalize.reviewDetailsCommitsPopupAll(commitsCount).get());

        commitMessageIcon = new JLabel();
        commitMessageText = new JBLabel();
        commitMessageText.setCopyable(true);

        commitMessagePanel = new HorizontalListPanel(ICON_TEXT_OFFSET);

        authorAndDate = new JLabel();
        authorAndDate.setBorder(JBUI.Borders.emptyTop(MESSAGE_INFO_VERTICAL_GAP));
        authorAndDate.setIconTextGap(JBUIScale.scale(ICON_TEXT_OFFSET));
        authorAndDate.setForeground(NamedColorUtil.getInactiveTextColor());

        textPanel = new BorderLayoutPanel();
        textPanel.setBorder(JBUI.Borders.empty(COMMIT_TOP_BOTTOM, getLeftRightGap()));

        commitPanel = new BorderLayoutPanel();
    }

    private static int getLeftRightGap() {
        return CollaborationToolsUIUtil.getSize(8, 0); // in case of the newUI gap handled by SelectablePanel
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends T> list,
        T value,
        int index,
        boolean cellSelected,
        boolean cellHasFocus
    ) {
        cleanupComponents();

        SelectableWrapper<CommitPresentation> presentation = presenter.apply(value);
        CommitPresentation commit = presentation.getValue();

        commitMessageIcon.setIcon(presentation.isSelected() ? PlatformIconGroup.actionsChecked_selected() : emptyIcon);
        allCommitsMessage.setIcon(presentation.isSelected() ? PlatformIconGroup.actionsChecked_selected() : emptyIcon);
        authorAndDate.setIcon(emptyIcon);

        if (commit == null) {
            commitPanel.addToCenter(allCommitsMessage);
        }
        else {
            commitMessageText.setText(commit.getTitleHtml());
            authorAndDate.setText(commit.getAuthor() + ", " + DateFormatUtil.formatPrettyDateTime(commit.getCommittedDate()));

            commitMessagePanel.add(commitMessageIcon);
            commitMessagePanel.add(commitMessageText);
            textPanel.addToCenter(commitMessagePanel).addToBottom(authorAndDate);
            commitPanel.addToCenter(textPanel);
        }

        UIUtil.setBackgroundRecursively(commitPanel, ListUiUtil.WithTallRow.background(list, cellSelected, list.hasFocus()));
        return commitPanel;
    }

    private void cleanupComponents() {
        commitMessagePanel.removeAll();
        textPanel.removeAll();
        commitPanel.removeAll();
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nonnull ListCellRenderer<T> createCommitRenderer(
        int commitsCount,
        @Nonnull Function<T, SelectableWrapper<CommitPresentation>> presenter
    ) {
        ListCellRenderer<T> commitRenderer = new CommitRenderer<>(commitsCount, presenter);
        if (ExperimentalUI.isNewUI()) {
            commitRenderer = new RoundedCellRenderer<>(commitRenderer, false);
        }
        return GroupedRenderer.create(commitRenderer, (value, index) -> false, (value, index) -> {
            SelectableWrapper<CommitPresentation> wrapper = presenter.apply(value);
            return wrapper.getValue() == null;
        });
    }
}
