// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.ui.codereview;

import com.intellij.collaboration.async.AsyncUtilKt;
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeListViewModel;
import com.intellij.collaboration.util.ChangesSelection;
import com.intellij.collaboration.util.RefComparisonChange;
import consulo.ui.ex.awt.ClientProperty;
import consulo.ui.ex.awt.util.RenderingHelper;
import kotlinx.coroutines.flow.FlowKt;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public final class CodeReviewProgressTreeModelUtil {
    private CodeReviewProgressTreeModelUtil() {
    }

    public static void setupCodeReviewProgressModel(
        @Nonnull ChangesTree tree,
        @Nonnull CodeReviewChangeListViewModel vm,
        @Nonnull CodeReviewProgressTreeModel<?> model
    ) {
        TreeHoverListener.DEFAULT.addTo(tree);

        ClientProperty.put(tree, RenderingHelper.SHRINK_LONG_RENDERER, true);
        ClientProperty.put(tree, RenderingHelper.SHRINK_LONG_SELECTION, true);

        boolean hasViewedState = vm instanceof CodeReviewChangeListViewModel.WithViewedState;
        tree.setCellRenderer(new CodeReviewProgressRenderer(
            hasViewedState,
            new ChangesBrowserNodeRenderer(tree.getProject(), tree::isShowFlatten, false),
            model::getState
        ));
        installViewedStateToggleHandler(tree, vm, model);

        model.addChangeListener(tree::repaint);

        // Note: The launchOnShow + debounce logic from Kotlin requires coroutine infrastructure.
        // This is a simplified version - the tree rebuild on loading changes would need
        // to be handled through the coroutine scope in the calling code.
    }

    private static void installViewedStateToggleHandler(
        @Nonnull ChangesTree tree,
        @Nonnull CodeReviewChangeListViewModel vm,
        @Nonnull CodeReviewProgressTreeModel<?> model
    ) {
        if (vm instanceof CodeReviewChangeListViewModel.WithViewedState viewedVm) {
            tree.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }

                    int row = tree.getClosestRowForLocation(1, e.getY());
                    if (row < 0) {
                        return;
                    }

                    TreePath path = tree.getPathForRow(row);
                    if (path == null) {
                        return;
                    }

                    TreeUI treeUI = tree.getUI();
                    Rectangle cellBounds = null;
                    if (treeUI instanceof CustomBoundsTreeUI customUI) {
                        cellBounds = customUI.getActualPathBounds(tree, path);
                    }
                    if (cellBounds == null) {
                        return;
                    }

                    Point positionInCell = new Point(e.getX() - cellBounds.x, e.getY() - cellBounds.y);

                    Object lastComponent = path.getLastPathComponent();
                    if (!(lastComponent instanceof ChangesBrowserNode<?> node)) {
                        return;
                    }

                    // get the top-level rendered cell component
                    Component component = tree.getCellRenderer().getTreeCellRendererComponent(
                        tree,
                        node,
                        tree.isRowSelected(row),
                        tree.isExpanded(row),
                        tree.getModel().isLeaf(node),
                        row,
                        true
                    );

                    if (!(component instanceof CodeReviewProgressRendererComponent rendererComponent)) {
                        return;
                    }
                    Rectangle checkboxBounds = rendererComponent.checkboxBounds(cellBounds.getSize());
                    if (checkboxBounds == null) {
                        return;
                    }

                    if (checkboxBounds.contains(positionInCell)) {
                        Object userObject = node.getUserObject();
                        if (!(userObject instanceof RefComparisonChange change)) {
                            return;
                        }

                        NodeCodeReviewProgressState state = model.getState(node);
                        boolean isViewed = !state.isRead();

                        viewedVm.setViewedState(List.of(change), isViewed);
                        tree.repaint();
                    }
                }
            });
        }
    }

    public static void updateSelectedChangesFromTree(
        @Nonnull CodeReviewChangeListViewModel vm,
        @Nonnull AsyncChangesTree tree
    ) {
        boolean fuzzy = false;
        List<RefComparisonChange> selectedChanges = new ArrayList<>();
        var selected = VcsTreeModelData.selected(tree);
        for (var it = selected.iterateRawNodes(); it.hasNext(); ) {
            var node = it.next();
            if (node.isLeaf()) {
                Object userObject = node.getUserObject();
                if (userObject instanceof RefComparisonChange change) {
                    selectedChanges.add(change);
                }
            }
            else {
                fuzzy = true;
            }
        }
        ChangesSelection selection;
        if (selectedChanges.isEmpty()) {
            selection = null;
        }
        else if (fuzzy) {
            selection = new ChangesSelection.Fuzzy(selectedChanges);
        }
        else if (selectedChanges.size() == 1) {
            selection = new ChangesSelection.Precise(vm.getChanges(), selectedChanges.get(0));
        }
        else {
            selection = new ChangesSelection.Precise(selectedChanges, selectedChanges.get(0));
        }
        vm.updateSelectedChanges(selection);
    }
}
