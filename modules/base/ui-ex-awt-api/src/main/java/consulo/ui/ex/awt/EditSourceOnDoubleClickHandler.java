// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt;

import consulo.application.Application;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.project.Project;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.internal.laf.WideSelectionTreeUI;
import consulo.ui.ex.awt.tree.table.TreeTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;

public final class EditSourceOnDoubleClickHandler {

  private EditSourceOnDoubleClickHandler() {
  }

  public static void install(final JTree tree, @Nullable final Runnable whenPerformed) {
    new TreeMouseListener(tree, whenPerformed).installOn(tree);
  }

  public static void install(final JTree tree) {
    install(tree, null);
  }

  public static void install(final TreeTable treeTable) {
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(@Nonnull MouseEvent e) {
        if (Application.get().getCurrentModalityState().dominates(Application.get().getNoneModalityState())) return false;
        if (treeTable.getTree().getPathForLocation(e.getX(), e.getY()) == null) return false;
        DataContext dataContext = DataManager.getInstance().getDataContext(treeTable);
        Project project = dataContext.getData(Project.KEY);
        if (project == null) return false;
        OpenSourceUtil.openSourcesFrom(dataContext, true);
        return true;
      }
    }.installOn(treeTable);
  }

  public static void install(final JTable table) {
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(@Nonnull MouseEvent e) {
        if (Application.get().getCurrentModalityState().dominates(Application.get().getNoneModalityState())) return false;
        if (table.columnAtPoint(e.getPoint()) < 0) return false;
        if (table.rowAtPoint(e.getPoint()) < 0) return false;
        DataContext dataContext = DataManager.getInstance().getDataContext(table);
        Project project = dataContext.getData(Project.KEY);
        if (project == null) return false;
        OpenSourceUtil.openSourcesFrom(dataContext, true);
        return true;
      }
    }.installOn(table);
  }

  public static void install(final JList list, final Runnable whenPerformed) {
    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(@Nonnull MouseEvent e) {
        Point point = e.getPoint();
        int index = list.locationToIndex(point);
        if (index == -1) return false;
        if (!list.getCellBounds(index, index).contains(point)) return false;
        DataContext dataContext = DataManager.getInstance().getDataContext(list);
        OpenSourceUtil.openSourcesFrom(dataContext, true);
        whenPerformed.run();
        return true;
      }
    }.installOn(list);
  }

  public static boolean isToggleEvent(@Nonnull JTree tree, @Nonnull MouseEvent e) {
    if (!SwingUtilities.isLeftMouseButton(e)) return false;
    int count = tree.getToggleClickCount();
    if (count <= 0 || e.getClickCount() % count != 0) return false;
    return isExpandPreferable(tree, tree.getSelectionPath());
  }

  /**
   * @return {@code true} to expand/collapse the node, {@code false} to navigate to source if possible
   */
  public static boolean isExpandPreferable(@Nonnull JTree tree, @Nullable TreePath path) {
    return EditSourceOnDoubleClickHandlerBase.isExpandPreferable(tree, path);
  }

  public static class TreeMouseListener extends DoubleClickListener {
    private final JTree myTree;
    @Nullable
    private final Runnable myWhenPerformed;

    public TreeMouseListener(final JTree tree) {
      this(tree, null);
    }

    public TreeMouseListener(final JTree tree, @Nullable final Runnable whenPerformed) {
      myTree = tree;
      myWhenPerformed = whenPerformed;
    }

    @Override
    public void installOn(@Nonnull Component c, boolean allowDragWhileClicking) {
      super.installOn(c, allowDragWhileClicking);
      myTree.putClientProperty(EditSourceOnDoubleClickHandlerBase.INSTALLED, true);
    }

    @Override
    public void uninstall(Component c) {
      super.uninstall(c);
      myTree.putClientProperty(EditSourceOnDoubleClickHandlerBase.INSTALLED, null);
    }

    @Override
    public boolean onDoubleClick(@Nonnull MouseEvent e) {
      TreePath clickPath = WideSelectionTreeUI.isWideSelection(myTree) ? myTree.getClosestPathForLocation(e.getX(), e.getY()) : myTree.getPathForLocation(e.getX(), e.getY());
      if (clickPath == null) return false;

      final DataContext dataContext = DataManager.getInstance().getDataContext(myTree);
      Project project = dataContext.getData(Project.KEY);
      if (project == null) return false;

      TreePath selectionPath = myTree.getSelectionPath();
      if (!clickPath.equals(selectionPath)) return false;

      //Node expansion for non-leafs has a higher priority
      if (isToggleEvent(myTree, e)) return false;

      processDoubleClick(e, dataContext, selectionPath);
      return true;
    }

    @SuppressWarnings("UnusedParameters")
    protected void processDoubleClick(@Nonnull MouseEvent e, @Nonnull DataContext dataContext, @Nonnull TreePath treePath) {
      OpenSourceUtil.openSourcesFrom(dataContext, true);
      if (myWhenPerformed != null) myWhenPerformed.run();
    }
  }
}
