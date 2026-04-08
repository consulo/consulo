// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview;

import com.intellij.collaboration.ui.layout.SizeRestrictedSingleComponentLayout;
import com.intellij.collaboration.ui.util.DimensionRestrictions;
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.awt.CellRendererPanel;
import consulo.ui.ex.awt.ClientProperty;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

final class CodeReviewProgressRendererComponent extends CellRendererPanel {
    private static final int TEXT_ICON_GAP = 4;
    private static final int COMMENT_AND_UNREAD_GAP = 10;
    private static final int LEFT_SIDE_GAP = 4;
    private static final int RIGHT_SIDE_GAP = 16;

    private final boolean hasViewedState;
    private final ColoredTreeCellRenderer renderer;
    private final Function<ChangesBrowserNode<?>, NodeCodeReviewProgressState> codeReviewProgressStateProvider;

    private final JCheckBox checkbox;
    private final JLabel commentIconLabel;
    private final JLabel unreadIconLabel;
    private final JLabel invisiblePlaceholder;
    private final JPanel unreadOrCheckboxContainer;
    private final HorizontalListPanel stateContainer;

    CodeReviewProgressRendererComponent(
        boolean hasViewedState,
        @Nonnull ColoredTreeCellRenderer renderer,
        @Nonnull Function<ChangesBrowserNode<?>, NodeCodeReviewProgressState> codeReviewProgressStateProvider
    ) {
        this.hasViewedState = hasViewedState;
        this.renderer = renderer;
        this.codeReviewProgressStateProvider = codeReviewProgressStateProvider;

        checkbox = new JCheckBox();
        checkbox.setOpaque(false);
        checkbox.setBorder(JBUI.Borders.empty());

        commentIconLabel = new JLabel();
        commentIconLabel.setBorder(JBUI.Borders.empty());
        commentIconLabel.setIconTextGap(JBUI.scale(TEXT_ICON_GAP));
        commentIconLabel.setIcon(CollaborationToolsIcons.Review.CommentUnread);

        unreadIconLabel = new JLabel();
        unreadIconLabel.setBorder(JBUI.Borders.empty());
        unreadIconLabel.setIcon(CollaborationToolsIcons.Review.FileUnread);
        unreadIconLabel.setHorizontalAlignment(JLabel.CENTER);
        unreadIconLabel.setVerticalAlignment(JLabel.CENTER);

        invisiblePlaceholder = new JLabel();
        invisiblePlaceholder.setVisible(false);

        unreadOrCheckboxContainer = new JPanel();
        unreadOrCheckboxContainer.setOpaque(false);
        unreadOrCheckboxContainer.setBorder(JBUI.Borders.empty());

        DimensionRestrictions sizeRestriction = new DimensionRestrictions() {
            @Override
            public @Nullable Integer getWidth() {
                return checkbox.getPreferredSize().width;
            }

            @Override
            public @Nullable Integer getHeight() {
                return null;
            }
        };
        SizeRestrictedSingleComponentLayout layout = new SizeRestrictedSingleComponentLayout();
        layout.setPrefSize(sizeRestriction);
        layout.setMinSize(sizeRestriction);
        layout.setMaxSize(sizeRestriction);
        unreadOrCheckboxContainer.setLayout(layout);

        stateContainer = new HorizontalListPanel(COMMENT_AND_UNREAD_GAP);

        setLayout(new BorderLayout());
        ClientProperty.put(this, ExpandableItemsHandler.RENDERER_DISABLED, true);
    }

    @RequiresEdt
    @Nullable
    Rectangle checkboxBounds(@Nonnull Dimension cellSize) {
        setBounds(new Rectangle(0, 0, cellSize.width, cellSize.height));
        return calculateBoundsWithin(checkbox, this);
    }

    @RequiresEdt
    @Nonnull
    JComponent prepareComponent(
        @Nonnull JTree tree, @Nonnull Object value, boolean selected, boolean expanded,
        boolean leaf, int row, boolean hasFocus
    ) {
        ChangesBrowserNode<?> node = (ChangesBrowserNode<?>) value;
        NodeCodeReviewProgressState state = codeReviewProgressStateProvider.apply(node);

        setBorder(JBUI.Borders.empty(0, LEFT_SIDE_GAP, 0, RIGHT_SIDE_GAP));
        setBackground(null);
        setSelected(selected);

        removeAll();
        add(updateFilenameContainer(tree, value, selected, expanded, leaf, row, hasFocus), BorderLayout.CENTER);

        // if loading, don't show any icons yet
        if (isStateContainerShown(leaf, expanded) && !state.isLoading()) {
            add(updateStateContainer(tree, state, row, leaf), BorderLayout.EAST);
        }

        return this;
    }

    private @Nonnull Component updateFilenameContainer(
        @Nonnull JTree tree, @Nonnull Object value, boolean selected,
        boolean expanded, boolean leaf, int row, boolean hasFocus
    ) {
        Component comp = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        comp.setBackground(null);
        comp.setMinimumSize(new Dimension());
        if (comp instanceof JComponent jcomp) {
            jcomp.setBorder(JBUI.Borders.empty());
        }
        return comp;
    }

    private @Nonnull JComponent updateStateContainer(
        @Nonnull JTree tree, @Nonnull NodeCodeReviewProgressState state,
        int row, boolean isLeaf
    ) {
        stateContainer.removeAll();

        JComponent commentLabel = updateCommentIconLabel(state);
        if (commentLabel != null) {
            stateContainer.add(commentLabel);
        }

        boolean isHovered = TreeHoverListener.getHoveredRow(tree) == row;
        JComponent rightSideComp = null;
        if (isLeaf && hasViewedState && (isHovered || state.isRead())) {
            rightSideComp = updateViewedCheckbox(state);
        }
        else if (!state.isRead()) {
            rightSideComp = unreadIconLabel;
        }

        if (commentLabel != null || rightSideComp != null) {
            unreadOrCheckboxContainer.removeAll();
            unreadOrCheckboxContainer.add(rightSideComp != null ? rightSideComp : invisiblePlaceholder);
            stateContainer.add(unreadOrCheckboxContainer);
        }

        return stateContainer;
    }

    private boolean isStateContainerShown(boolean isLeaf, boolean isExpanded) {
        return isLeaf || !isExpanded;
    }

    private @Nonnull JComponent updateViewedCheckbox(@Nonnull NodeCodeReviewProgressState valueData) {
        checkbox.setSelected(valueData.isRead());
        return checkbox;
    }

    private @Nullable JComponent updateCommentIconLabel(@Nonnull NodeCodeReviewProgressState state) {
        if (state.discussionsCount() <= 0) {
            return null;
        }

        commentIconLabel.setIcon(state.isRead() ? CollaborationToolsIcons.Review.CommentUnresolved : CollaborationToolsIcons.Review.CommentUnread);
        commentIconLabel.setText(String.valueOf(state.discussionsCount()));

        return commentIconLabel;
    }

    private static @Nullable Rectangle calculateBoundsWithin(@Nonnull JComponent comp, @Nonnull JComponent parent) {
        if (!SwingUtilities.isDescendingFrom(comp, parent)) {
            return null;
        }

        // Perform layouts on all parents in top-down order
        UIUtil.layoutRecursively(parent);

        // Get and translate bounds to the parent
        Rectangle bounds = new Rectangle(comp.getBounds());
        Point translation = SwingUtilities.convertPoint(comp, new Point(0, 0), parent);
        bounds.translate(translation.x, translation.y);

        return bounds;
    }
}
