// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview;

import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.function.Function;

final class CodeReviewProgressRenderer implements TreeCellRenderer {
    private final CodeReviewProgressRendererComponent component;

    CodeReviewProgressRenderer(
        boolean hasViewedState,
        @Nonnull ColoredTreeCellRenderer renderer,
        @Nonnull Function<ChangesBrowserNode<?>, NodeCodeReviewProgressState> codeReviewProgressStateProvider
    ) {
        component = new CodeReviewProgressRendererComponent(hasViewedState, renderer, codeReviewProgressStateProvider);
    }

    @Override
    public Component getTreeCellRendererComponent(
        JTree tree, Object value, boolean selected, boolean expanded,
        boolean leaf, int row, boolean hasFocus
    ) {
        return component.prepareComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }
}
