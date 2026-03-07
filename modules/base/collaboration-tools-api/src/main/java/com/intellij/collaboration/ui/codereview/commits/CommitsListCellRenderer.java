// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.commits;

import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.speedSearch.SpeedSearchUtil;
import consulo.versionControlSystem.log.VcsCommitMetadata;

import javax.swing.*;
import java.awt.*;

final class CommitsListCellRenderer extends BorderLayoutPanel implements ListCellRenderer<VcsCommitMetadata> {
    private final CommitNodeComponent nodeComponent;
    private final SimpleColoredComponent messageComponent;

    CommitsListCellRenderer() {
        nodeComponent = new CommitNodeComponent();
        nodeComponent.setForeground(ServiceKt.service(DefaultColorGenerator.class).getColor(1));
        messageComponent = new SimpleColoredComponent();

        addToLeft(nodeComponent);
        addToCenter(messageComponent);
    }

    @Override
    public Component getListCellRendererComponent(
        JList<? extends VcsCommitMetadata> list,
        VcsCommitMetadata value,
        int index,
        boolean isSelected,
        boolean cellHasFocus
    ) {
        messageComponent.clear();
        String subject = value != null ? value.getSubject() : "";
        messageComponent.append(
            subject,
            new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, UIUtil.getListForeground(isSelected, cellHasFocus))
        );
        SpeedSearchUtil.applySpeedSearchHighlighting(list, messageComponent, true, isSelected);

        int size = list.getModel().getSize();
        nodeComponent.setType(CommitNodeComponent.typeForListItem(index, size));

        setBackground(UIUtil.getListBackground(isSelected, cellHasFocus));
        return this;
    }
}
