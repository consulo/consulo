// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tree.ui;

import com.intellij.ui.TreeActions;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Consumer;

import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static javax.swing.KeyStroke.getKeyStroke;
import static javax.swing.tree.TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION;

final class TreeAction extends AbstractAction implements UIResource {
  private enum MoveType {
    ChangeLead,
    ChangeSelection,
    ExtendSelection
  }

  private static final List<TreeAction> ACTIONS =
          asList(new TreeAction(TreeAction::selectFirst, TreeActions.Home.ID, getKeyStroke(VK_HOME, 0)), new TreeAction(TreeAction::selectFirstChangeLead, "selectFirstChangeLead"),
                 new TreeAction(TreeAction::selectFirstExtendSelection, TreeActions.ShiftHome.ID),

                 new TreeAction(TreeAction::selectLast, TreeActions.End.ID, getKeyStroke(VK_END, 0)), new TreeAction(TreeAction::selectLastChangeLead, "selectLastChangeLead"),
                 new TreeAction(TreeAction::selectLastExtendSelection, TreeActions.ShiftEnd.ID),

                 new TreeAction(TreeAction::selectPrevious, TreeActions.Up.ID, getKeyStroke(VK_UP, 0), getKeyStroke(VK_KP_UP, 0)),
                 new TreeAction(TreeAction::selectPreviousChangeLead, "selectPreviousChangeLead"), new TreeAction(TreeAction::selectPreviousExtendSelection, TreeActions.ShiftUp.ID),

                 new TreeAction(TreeAction::selectNext, TreeActions.Down.ID, getKeyStroke(VK_DOWN, 0), getKeyStroke(VK_KP_DOWN, 0)),
                 new TreeAction(TreeAction::selectNextChangeLead, "selectNextChangeLead"), new TreeAction(TreeAction::selectNextExtendSelection, TreeActions.ShiftDown.ID),

                 new TreeAction(TreeAction::selectParent, TreeActions.Left.ID, getKeyStroke(VK_LEFT, 0), getKeyStroke(VK_KP_LEFT, 0)),
                 // new TreeAction(TreeAction::selectParentChangeLead, "selectParentChangeLead"),
                 // new TreeAction(TreeAction::selectParentExtendSelection, TreeActions.ShiftLeft.ID),

                 new TreeAction(TreeAction::selectChild, TreeActions.Right.ID, getKeyStroke(VK_RIGHT, 0), getKeyStroke(VK_KP_RIGHT, 0)),
                 // new TreeAction(TreeAction::selectChildChangeLead, "selectChildChangeLead"),
                 // new TreeAction(TreeAction::selectChildExtendSelection, TreeActions.ShiftRight.ID),

                 new TreeAction(TreeAction::scrollUpChangeSelection, TreeActions.PageUp.ID, getKeyStroke(VK_PAGE_UP, 0)), new TreeAction(TreeAction::scrollUpChangeLead, "scrollUpChangeLead"),
                 new TreeAction(TreeAction::scrollUpExtendSelection, TreeActions.ShiftPageUp.ID),

                 new TreeAction(TreeAction::scrollDownChangeSelection, TreeActions.PageDown.ID, getKeyStroke(VK_PAGE_DOWN, 0)),
                 new TreeAction(TreeAction::scrollDownChangeLead, "scrollDownChangeLead"), new TreeAction(TreeAction::scrollDownExtendSelection, TreeActions.ShiftPageDown.ID),

                 new TreeAction(TreeAction::selectNextSibling, TreeActions.NextSibling.ID), new TreeAction(TreeAction::selectPreviousSibling, TreeActions.PreviousSibling.ID));
  private final String name;
  private final Consumer<JTree> action;
  private final List<KeyStroke> keys;

  private TreeAction(@Nonnull Consumer<JTree> action, @Nonnull @NonNls String name, @Nonnull KeyStroke... keys) {
    this.name = name;
    this.action = action;
    this.keys = asList(keys);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    Object source = event.getSource();
    if (source instanceof JTree) action.accept((JTree)source);
  }

  static void installTo(@Nonnull ActionMap map) {
    Object[] keys = map.keys();
    if (keys != null && keys.length != 0) return; // actions are already installed
    for (TreeAction action : ACTIONS) map.put(action.name, action);
  }

  static void installTo(@Nonnull InputMap map) {
    Object[] keys = map.keys();
    if (keys != null && keys.length != 0) return; // keys for actions are already installed
    for (TreeAction action : ACTIONS) for (KeyStroke key : action.keys) map.put(key, action.name);
  }

  private static boolean isCycleScrollingAllowed(@Nonnull MoveType type) {
    return type != MoveType.ExtendSelection && TreeUtil.isCyclicScrollingAllowed();
  }

  private static boolean isLeaf(@Nonnull JTree tree, @Nonnull TreePath path) {
    return tree.getModel().isLeaf(path.getLastPathComponent()); // TODO:malenkov: via DefaultTreeUI
  }

  private static void lineDown(@Nonnull MoveType type, @Nonnull JTree tree) {
    TreePath lead = tree.getLeadSelectionPath();
    int row = tree.getRowForPath(lead);
    if (lead == null || row < 0) {
      selectFirst(type, tree);
    }
    else {
      row++; // NB!: increase row before checking for cycle scrolling
      if (isCycleScrollingAllowed(type) && row == tree.getRowCount()) row = 0;
      select(type, tree, row);
    }
  }

  private static void lineUp(@Nonnull MoveType type, @Nonnull JTree tree) {
    TreePath lead = tree.getLeadSelectionPath();
    int row = tree.getRowForPath(lead);
    if (lead == null || row < 0) {
      selectFirst(type, tree);
    }
    else {
      if (row == 0 && isCycleScrollingAllowed(type)) row = tree.getRowCount();
      row--; // NB!: decrease row after checking for cycle scrolling
      select(type, tree, row);
    }
  }

  private static void pageDown(@Nonnull MoveType type, @Nonnull JTree tree) {
    TreePath lead = tree.getLeadSelectionPath();
    Rectangle bounds = tree.getPathBounds(lead);
    if (lead == null || bounds == null) {
      selectLast(type, tree);
    }
    else {
      int height = Math.max(tree.getVisibleRect().height - bounds.height * 4, 1);
      TreePath next = tree.getClosestPathForLocation(bounds.x, bounds.y + bounds.height + height);
      if (next != null && !next.equals(lead)) select(type, tree, next);
    }
  }

  private static void pageUp(@Nonnull MoveType type, @Nonnull JTree tree) {
    TreePath lead = tree.getLeadSelectionPath();
    Rectangle bounds = tree.getPathBounds(lead);
    if (lead == null || bounds == null) {
      selectFirst(type, tree);
    }
    else {
      int height = Math.max(tree.getVisibleRect().height - bounds.height * 4, 1);
      TreePath next = tree.getClosestPathForLocation(bounds.x, bounds.y - height);
      if (next != null && !next.equals(lead)) select(type, tree, next);
    }
  }

  private static void select(@Nonnull MoveType type, @Nonnull JTree tree, int row) {
    select(type, tree, tree.getPathForRow(row), row);
  }

  private static void select(@Nonnull MoveType type, @Nonnull JTree tree, @Nonnull TreePath path) {
    select(type, tree, path, tree.getRowForPath(path));
  }

  private static void select(@Nonnull MoveType type, @Nonnull JTree tree, @Nullable TreePath path, int row) {
    if (path == null || row < 0) return;
    if (type == MoveType.ExtendSelection) {
      TreePath anchor = tree.getAnchorSelectionPath();
      int anchorRow = anchor == null ? -1 : tree.getRowForPath(anchor);
      if (anchorRow < 0) {
        tree.setSelectionPath(path);
      }
      else {
        tree.setSelectionInterval(row, anchorRow);
        tree.setAnchorSelectionPath(anchor);
        tree.setLeadSelectionPath(path);
      }
    }
    else if (type == MoveType.ChangeLead && DISCONTIGUOUS_TREE_SELECTION == tree.getSelectionModel().getSelectionMode()) {
      tree.setLeadSelectionPath(path);
    }
    else {
      tree.setSelectionPath(path);
    }
    TreeUtil.scrollToVisible(tree, path, false);
  }

  private static void selectChild(@Nonnull MoveType type, @Nonnull JTree tree) {
    TreePath lead = tree.getLeadSelectionPath();
    int row = tree.getRowForPath(lead);
    if (lead == null || row < 0) {
      selectFirst(type, tree);
    }
    else if (tree.isExpanded(lead) || isLeaf(tree, lead)) {
      TreePath path = tree.getPathForRow(row + 1);
      if (!TreeUtil.isLoadingPath(path)) select(type, tree, path, row + 1);
    }
    else {
      tree.expandPath(lead);
    }
  }

  private static void selectFirst(@Nonnull MoveType type, @Nonnull JTree tree) {
    select(type, tree, 0);
  }

  private static void selectLast(@Nonnull MoveType type, @Nonnull JTree tree) {
    select(type, tree, tree.getRowCount() - 1);
  }

  private static void selectParent(@Nonnull MoveType type, @Nonnull JTree tree) {
    TreePath lead = tree.getLeadSelectionPath();
    int row = tree.getRowForPath(lead);
    if (lead == null || row < 0) {
      selectFirst(type, tree);
    }
    else if (type == MoveType.ChangeSelection && tree.isExpanded(lead)) {
      tree.collapsePath(lead);
    }
    else {
      TreePath parent = lead.getParentPath();
      if (parent != null) {
        if (tree.isRootVisible() || null != parent.getParentPath()) {
          select(type, tree, parent);
        }
        else if (row > 0) {
          TreePath path = TreeUtil.previousVisiblePath(tree, row, false, tree::isExpanded);
          select(type, tree, path != null ? path : tree.getPathForRow(0), path == null ? 0 : tree.getRowForPath(path));
        }
      }
    }
  }

  private static void selectNextSibling(@Nonnull JTree tree) {
    TreePath lead = tree.getLeadSelectionPath();
    if (lead == null) return; // nothing is selected
    TreePath parent = lead.getParentPath();
    if (parent == null) return; // root node has no siblings
    TreePath found = TreeUtil.nextVisiblePath(tree, lead, path -> parent.equals(path.getParentPath()));
    if (found == null) return; // next sibling is not found
    tree.setSelectionPath(found);
    TreeUtil.scrollToVisible(tree, found, false);
  }

  private static void selectPreviousSibling(@Nonnull JTree tree) {
    TreePath lead = tree.getLeadSelectionPath();
    if (lead == null) return; // nothing is selected
    TreePath parent = lead.getParentPath();
    if (parent == null) return; // root node has no siblings
    TreePath found = TreeUtil.previousVisiblePath(tree, lead, path -> parent.equals(path.getParentPath()));
    if (found == null) return; // previous sibling is not found
    tree.setSelectionPath(found);
    TreeUtil.scrollToVisible(tree, found, false);
  }

  // NB!: the following method names correspond Tree.focusInputMap in BasicLookAndFeel and Actions in BasicTreeUI

  @SuppressWarnings("unused") // TODO:malenkov: implement addToSelection
  private static void addToSelection(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement cancel
  private static void cancel(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement clearSelection
  private static void clearSelection(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement collapse
  private static void collapse(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement expand
  private static void expand(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement extendTo
  private static void extendTo(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement moveSelectionTo
  private static void moveSelectionTo(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement moveSelectionToParent
  private static void moveSelectionToParent(@Nonnull JTree tree) {
  }

  private static void scrollDownChangeLead(@Nonnull JTree tree) {
    pageDown(MoveType.ChangeLead, tree);
  }

  private static void scrollDownChangeSelection(@Nonnull JTree tree) {
    pageDown(MoveType.ChangeSelection, tree);
  }

  private static void scrollDownExtendSelection(@Nonnull JTree tree) {
    pageDown(MoveType.ExtendSelection, tree);
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement scrollLeft
  private static void scrollLeft(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement scrollLeftChangeLead
  private static void scrollLeftChangeLead(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement scrollLeftExtendSelection
  private static void scrollLeftExtendSelection(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement scrollRight
  private static void scrollRight(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement scrollRightChangeLead
  private static void scrollRightChangeLead(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement scrollRightExtendSelection
  private static void scrollRightExtendSelection(@Nonnull JTree tree) {
  }

  private static void scrollUpChangeLead(@Nonnull JTree tree) {
    pageUp(MoveType.ChangeLead, tree);
  }

  private static void scrollUpChangeSelection(@Nonnull JTree tree) {
    pageUp(MoveType.ChangeSelection, tree);
  }

  private static void scrollUpExtendSelection(@Nonnull JTree tree) {
    pageUp(MoveType.ExtendSelection, tree);
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement selectAll
  private static void selectAll(@Nonnull JTree tree) {
  }

  private static void selectChild(@Nonnull JTree tree) {
    selectChild(MoveType.ChangeSelection, tree);
  }

  @SuppressWarnings("unused")
  private static void selectChildChangeLead(@Nonnull JTree tree) {
    selectChild(MoveType.ChangeLead, tree);
  }

  @SuppressWarnings("unused") // because inconvenient
  private static void selectChildExtendSelection(@Nonnull JTree tree) {
    selectChild(MoveType.ExtendSelection, tree);
  }

  private static void selectFirst(@Nonnull JTree tree) {
    selectFirst(MoveType.ChangeSelection, tree);
  }

  private static void selectFirstChangeLead(@Nonnull JTree tree) {
    selectFirst(MoveType.ChangeLead, tree);
  }

  private static void selectFirstExtendSelection(@Nonnull JTree tree) {
    selectFirst(MoveType.ExtendSelection, tree);
  }

  private static void selectLast(@Nonnull JTree tree) {
    selectLast(MoveType.ChangeSelection, tree);
  }

  private static void selectLastChangeLead(@Nonnull JTree tree) {
    selectLast(MoveType.ChangeLead, tree);
  }

  private static void selectLastExtendSelection(@Nonnull JTree tree) {
    selectLast(MoveType.ExtendSelection, tree);
  }

  private static void selectNext(@Nonnull JTree tree) {
    lineDown(MoveType.ChangeSelection, tree);
  }

  private static void selectNextChangeLead(@Nonnull JTree tree) {
    lineDown(MoveType.ChangeLead, tree);
  }

  private static void selectNextExtendSelection(@Nonnull JTree tree) {
    lineDown(MoveType.ExtendSelection, tree);
  }

  private static void selectParent(@Nonnull JTree tree) {
    selectParent(MoveType.ChangeSelection, tree);
  }

  @SuppressWarnings("unused")
  private static void selectParentChangeLead(@Nonnull JTree tree) {
    selectParent(MoveType.ChangeLead, tree);
  }

  @SuppressWarnings("unused") // because inconvenient
  private static void selectParentExtendSelection(@Nonnull JTree tree) {
    selectParent(MoveType.ExtendSelection, tree);
  }

  private static void selectPrevious(@Nonnull JTree tree) {
    lineUp(MoveType.ChangeSelection, tree);
  }

  private static void selectPreviousChangeLead(@Nonnull JTree tree) {
    lineUp(MoveType.ChangeLead, tree);
  }

  private static void selectPreviousExtendSelection(@Nonnull JTree tree) {
    lineUp(MoveType.ExtendSelection, tree);
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement startEditing
  private static void startEditing(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement toggle
  private static void toggle(@Nonnull JTree tree) {
  }

  @SuppressWarnings("unused") // TODO:malenkov: implement toggleAndAnchor
  private static void toggleAndAnchor(@Nonnull JTree tree) {
  }
}
