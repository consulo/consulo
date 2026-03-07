// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.create;

import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

@ApiStatus.Internal
final class TwoLinesCommitRenderer<T> extends BorderLayoutPanel implements ListCellRenderer<T> {
    private static final int TOP_BOTTOM_OFFSET = 4;

    private final Function<T, @Nls String> getCommitMessage;
    private final Function<T, @Nls String> getAuthorAndDateLine;

    private final SimpleColoredComponent commitMessage;
    private final SimpleColoredComponent authorAndDate;

    TwoLinesCommitRenderer(
        @Nonnull Function<T, @Nls String> getCommitMessage,
        @Nonnull Function<T, @Nls String> getAuthorAndDateLine
    ) {
        this.getCommitMessage = getCommitMessage;
        this.getAuthorAndDateLine = getAuthorAndDateLine;

        commitMessage = new SimpleColoredComponent();
        commitMessage.setOpaque(false);
        commitMessage.setBorder(JBUI.Borders.emptyTop(TOP_BOTTOM_OFFSET));

        authorAndDate = new SimpleColoredComponent();
        authorAndDate.setOpaque(false);
        authorAndDate.setBorder(JBUI.Borders.emptyBottom(TOP_BOTTOM_OFFSET));

        addToCenter(commitMessage);
        addToBottom(authorAndDate);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends T> list,
        T value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        commitMessage.clear();
        commitMessage.append(getCommitMessage.apply(value));
        commitMessage.setForeground(ListUiUtil.WithTallRow.foreground(isSelected, list.hasFocus()));
        SpeedSearchUtil.applySpeedSearchHighlighting(list, commitMessage, true, isSelected);

        authorAndDate.clear();
        authorAndDate.append(getAuthorAndDateLine.apply(value));
        authorAndDate.setForeground(ListUiUtil.WithTallRow.secondaryForeground(isSelected, list.hasFocus()));

        UIUtil.setBackgroundRecursively(this, ListUiUtil.WithTallRow.background(list, isSelected, list.hasFocus()));
        return this;
    }
}
