// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.create;

import com.intellij.collaboration.ui.codereview.commits.CommitNodeComponent;
import consulo.application.util.DateFormatUtil;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.versionControlSystem.log.VcsCommitMetadata;

import javax.swing.*;
import java.awt.*;

/**
 * Use {@link CodeReviewCreateReviewUIUtil#createCommitListCellRenderer()} instead.
 */
@ApiStatus.Internal
public final class CodeReviewTwoLinesCommitRenderer extends BorderLayoutPanel implements ListCellRenderer<VcsCommitMetadata> {
    private static final int BASE_GAP = 12;

    private final MyCommitNodeComponent nodeComponent;
    private final TwoLinesCommitRenderer<VcsCommitMetadata> commitRenderer;

    CodeReviewTwoLinesCommitRenderer() {
        nodeComponent = new MyCommitNodeComponent();
        nodeComponent.setForeground(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground());

        commitRenderer = new TwoLinesCommitRenderer<>(
            VcsCommitMetadata::getSubject,
            metadata -> metadata.getAuthor().getName() + " " + DateFormatUtil.formatPrettyDateTime(metadata.getCommitTime())
        );
        commitRenderer.setBorder(JBUI.Borders.emptyLeft(BASE_GAP));

        setBorder(JBUI.Borders.emptyLeft(BASE_GAP));
        addToLeft(nodeComponent);
        addToCenter(commitRenderer);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends VcsCommitMetadata> list,
        VcsCommitMetadata value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        nodeComponent.setForeground(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground());

        commitRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        int size = list.getModel().getSize();
        nodeComponent.setType(CommitNodeComponent.typeForListItem(index, size));

        UIUtil.setBackgroundRecursively(this, ListUiUtil.WithTallRow.background(list, isSelected, list.hasFocus()));
        return this;
    }
}
